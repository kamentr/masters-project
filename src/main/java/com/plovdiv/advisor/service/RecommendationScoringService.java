package com.plovdiv.advisor.service;

import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.Confidence;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.NeighborhoodScore;
import com.plovdiv.advisor.dto.PropertyCandidate;
import com.plovdiv.advisor.dto.RecommendationResult;
import com.plovdiv.advisor.dto.SearchCriteria;
import com.plovdiv.advisor.ontology.OntologyService;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecommendationScoringService {

    public Optional<PropertyCandidate> candidateFor(PropertyOntologyRecord record, SearchCriteria criteria) {
        if (!passesBaseFilters(record, criteria)) {
            return Optional.empty();
        }
        return Optional.of(new PropertyCandidate(record.id(), baseScore(record, criteria)));
    }

    public boolean passesBaseFilters(PropertyOntologyRecord record, SearchCriteria criteria) {
        if (!record.available()) {
            return false;
        }
        if (criteria.maxBudgetEUR() != null
                && criteria.maxBudgetEUR().compareTo(BigDecimal.ZERO) > 0
                && record.priceEUR().compareTo(criteria.maxBudgetEUR()) > 0) {
            return false;
        }
        if (record.rooms() < criteria.minRooms()) {
            return false;
        }
        if (record.bedrooms() < criteria.minBedrooms()) {
            return false;
        }
        if (criteria.districts() != null && !criteria.districts().isEmpty()
                && !criteria.districts().contains(record.district())) {
            return false;
        }
        if (criteria.constructionType() != null && record.constructionType() != criteria.constructionType()) {
            return false;
        }
        if (criteria.heatingType() != null && record.heatingType() != criteria.heatingType()) {
            return false;
        }
        if (criteria.requiresElevator() && !record.hasElevator()) {
            return false;
        }
        if (criteria.requiresParking() && !record.hasParking()) {
            return false;
        }
        return !criteria.requiresBalcony() || record.hasBalcony();
    }

    public int baseScore(PropertyOntologyRecord record, SearchCriteria criteria) {
        return budgetScore(record, criteria) + featureScore(record, criteria) + districtScore(record, criteria);
    }

    public int neighborhoodScore(PropertyOntologyRecord record, BuyerProfile profile) {
        if (profile == null) {
            return 0;
        }

        return switch (profile) {
            case FAMILY -> familyScore(record);
            case STUDENT -> studentScore(record);
            case RETIRED_PERSON -> retiredScore(record);
            case INVESTOR -> investorScore(record);
            case YOUNG_PROFESSIONAL -> youngProfessionalScore(record);
        };
    }

    public List<RecommendationResult> combineAndRank(
            SearchCriteria criteria,
            List<PropertyCandidate> candidates,
            List<NeighborhoodScore> neighborhoodScores,
            Confidence confidence,
            boolean fallback,
            OntologyService ontologyService) {
        Map<String, Integer> neighborhoodByProperty = new ConcurrentHashMap<>();
        for (NeighborhoodScore score : neighborhoodScores) {
            neighborhoodByProperty.put(score.propertyId(), score.score());
        }

        List<RecommendationResult> results = new ArrayList<>();
        for (PropertyCandidate candidate : candidates) {
            Optional<PropertyOntologyRecord> record = ontologyService.findProperty(candidate.propertyId());
            if (record.isEmpty()) {
                continue;
            }
            int finalScore = Math.min(100, candidate.baseScore() + neighborhoodByProperty.getOrDefault(candidate.propertyId(), 0));
            results.add(new RecommendationResult(
                    candidate.propertyId(),
                    finalScore,
                    confidence,
                    explanations(record.get(), criteria, fallback)
            ));
        }

        results.sort(tieBreaker(criteria, ontologyService));
        return results;
    }

    private int budgetScore(PropertyOntologyRecord record, SearchCriteria criteria) {
        if (criteria.maxBudgetEUR() == null || criteria.maxBudgetEUR().compareTo(BigDecimal.ZERO) <= 0) {
            return 25;
        }
        BigDecimal maxBudget = criteria.maxBudgetEUR();
        BigDecimal strongFitThreshold = maxBudget.multiply(new BigDecimal("0.85"));
        if (record.priceEUR().compareTo(strongFitThreshold) <= 0) {
            return 25;
        }
        if (record.priceEUR().compareTo(maxBudget) <= 0) {
            return 18;
        }
        return 0;
    }

    private int featureScore(PropertyOntologyRecord record, SearchCriteria criteria) {
        int score = 0;
        score += record.bedrooms() >= Math.max(1, criteria.minBedrooms()) ? 10 : 0;
        score += record.rooms() >= Math.max(1, criteria.minRooms()) ? 6 : 0;
        score += record.hasElevator() ? 3 : 0;
        score += record.hasParking() ? 3 : 0;
        score += record.hasBalcony() ? 3 : 0;
        return Math.min(25, score);
    }

    private int districtScore(PropertyOntologyRecord record, SearchCriteria criteria) {
        if (criteria.districts() == null || criteria.districts().isEmpty()) {
            return record.distanceToTransport() <= 500 ? 15 : 8;
        }
        return criteria.districts().contains(record.district()) ? 15 : 0;
    }

    private int familyScore(PropertyOntologyRecord record) {
        int score = 0;
        if (record.distanceToSchool() <= 800) score += 10;
        if (record.distanceToKindergarten() <= 800) score += 10;
        if (record.distanceToPark() <= 1000) score += 7;
        if (record.hasElevator()) score += 4;
        if (record.hasParking()) score += 4;
        return score;
    }

    private int studentScore(PropertyOntologyRecord record) {
        int score = 0;
        if (record.distanceToUniversity() <= 1500) score += 14;
        if (record.distanceToTransport() <= 500) score += 10;
        if (record.pricePerSqM().compareTo(new BigDecimal("1200")) <= 0) score += 6;
        if (Set.of(District.CENTER, District.OLD_TOWN, District.KAPANA, District.KAMENITSA, District.KARSHIYAKA)
                .contains(record.district())) {
            score += 5;
        }
        return score;
    }

    private int retiredScore(PropertyOntologyRecord record) {
        int score = 0;
        if (record.hasElevator() || record.floor() <= 2) score += 10;
        if (record.distanceToPharmacy() <= 700) score += 8;
        if (record.distanceToHospital() <= 2000) score += 7;
        if (record.distanceToPark() <= 1000) score += 5;
        if (record.distanceToTransport() <= 500) score += 5;
        return score;
    }

    private int investorScore(PropertyOntologyRecord record) {
        int score = 0;
        if (record.pricePerSqM().compareTo(new BigDecimal("1600")) <= 0) score += 15;
        if (Set.of(District.CENTER, District.KAPANA, District.KARSHIYAKA, District.TRAKIA).contains(record.district())) {
            score += 8;
        }
        if (record.distanceToUniversity() <= 1500 || record.distanceToTransport() <= 500) score += 7;
        if (record.areaSqM().compareTo(new BigDecimal("40")) >= 0
                && record.areaSqM().compareTo(new BigDecimal("130")) <= 0) {
            score += 5;
        }
        return score;
    }

    private int youngProfessionalScore(PropertyOntologyRecord record) {
        int score = 0;
        if (record.distanceToTransport() <= 500) score += 12;
        if (Set.of(District.CENTER, District.KAPANA, District.KAMENITSA, District.TRAKIA, District.KARSHIYAKA)
                .contains(record.district())) {
            score += 10;
        }
        if (record.hasParking()) score += 5;
        if (record.distanceToPark() <= 1000 || record.distanceToPharmacy() <= 700) score += 4;
        if (record.yearBuilt() >= 2015) score += 4;
        return score;
    }

    private List<String> explanations(PropertyOntologyRecord record, SearchCriteria criteria, boolean fallback) {
        List<String> explanation = new ArrayList<>();
        if (criteria.maxBudgetEUR() != null && criteria.maxBudgetEUR().compareTo(BigDecimal.ZERO) > 0) {
            if (record.priceEUR().compareTo(criteria.maxBudgetEUR().multiply(new BigDecimal("0.85"))) <= 0) {
                explanation.add("Well within budget");
            } else {
                explanation.add("Within budget");
            }
        }
        explanation.add(record.bedrooms() + " bedrooms");
        explanation.add(record.rooms() + " rooms");
        if (criteria.districts() != null && criteria.districts().contains(record.district())) {
            explanation.add("Located in preferred district: " + record.district().csvValue());
        }
        if (record.hasElevator()) explanation.add("Has elevator");
        if (record.hasParking()) explanation.add("Has parking");
        if (record.hasBalcony()) explanation.add("Has balcony");
        if (!fallback) {
            addProfileExplanations(explanation, record, criteria.profile());
        } else {
            explanation.add("Lifestyle suitability scoring was unavailable");
        }
        return explanation;
    }

    private void addProfileExplanations(List<String> explanation, PropertyOntologyRecord record, BuyerProfile profile) {
        if (profile == null) {
            return;
        }
        switch (profile) {
            case FAMILY -> {
                if (record.distanceToSchool() <= 800) explanation.add(record.distanceToSchool() + "m from school");
                if (record.distanceToKindergarten() <= 800) explanation.add(record.distanceToKindergarten() + "m from kindergarten");
                if (record.distanceToPark() <= 1000) explanation.add(record.distanceToPark() + "m from park");
            }
            case STUDENT -> {
                if (record.distanceToUniversity() <= 1500) explanation.add(record.distanceToUniversity() + "m from university");
                if (record.distanceToTransport() <= 500) explanation.add(record.distanceToTransport() + "m from transport");
            }
            case RETIRED_PERSON -> {
                if (record.distanceToPharmacy() <= 700) explanation.add(record.distanceToPharmacy() + "m from pharmacy");
                if (record.distanceToHospital() <= 2000) explanation.add(record.distanceToHospital() + "m from hospital");
            }
            case INVESTOR -> explanation.add("Price per square meter: " + record.pricePerSqM() + " EUR");
            case YOUNG_PROFESSIONAL -> {
                if (record.distanceToTransport() <= 500) explanation.add(record.distanceToTransport() + "m from transport");
                if (record.yearBuilt() >= 2015) explanation.add("Newer construction built in " + record.yearBuilt());
            }
        }
    }

    private Comparator<RecommendationResult> tieBreaker(SearchCriteria criteria, OntologyService ontologyService) {
        return (left, right) -> {
            int scoreDiff = Integer.compare(right.score(), left.score());
            if (scoreDiff != 0) return scoreDiff;

            Optional<PropertyOntologyRecord> leftRecord = ontologyService.findProperty(left.propertyId());
            Optional<PropertyOntologyRecord> rightRecord = ontologyService.findProperty(right.propertyId());
            if (leftRecord.isEmpty() || rightRecord.isEmpty()) return 0;

            int priceDiff = leftRecord.get().pricePerSqM().compareTo(rightRecord.get().pricePerSqM());
            if (priceDiff != 0) return priceDiff;

            if (criteria.priorities() != null && !criteria.priorities().isEmpty()) {
                int distanceDiff = Integer.compare(
                        distanceToPriority(leftRecord.get(), criteria.priorities().getFirst()),
                        distanceToPriority(rightRecord.get(), criteria.priorities().getFirst())
                );
                if (distanceDiff != 0) return distanceDiff;
            }

            return Integer.compare(rightRecord.get().yearBuilt(), leftRecord.get().yearBuilt());
        };
    }

    private int distanceToPriority(PropertyOntologyRecord record, String priority) {
        return switch (priority.toLowerCase().replace(" ", "")) {
            case "school" -> record.distanceToSchool();
            case "kindergarten" -> record.distanceToKindergarten();
            case "university" -> record.distanceToUniversity();
            case "park" -> record.distanceToPark();
            case "transport", "transportstop" -> record.distanceToTransport();
            case "hospital" -> record.distanceToHospital();
            case "pharmacy" -> record.distanceToPharmacy();
            default -> 9999;
        };
    }
}
