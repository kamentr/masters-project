package com.plovdiv.advisor.web;

import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.SearchCriteria;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SearchForm {
    private BuyerProfile profile = BuyerProfile.FAMILY;
    private BigDecimal maxBudgetEUR = new BigDecimal("150000");
    private List<District> districts = new ArrayList<>();
    private int minRooms = 2;
    private int minBedrooms = 1;
    private ConstructionType constructionType;
    private HeatingType heatingType;
    private boolean requiresElevator;
    private boolean requiresParking;
    private boolean requiresBalcony;
    private List<String> priorities = new ArrayList<>();

    public SearchCriteria toCriteria() {
        return new SearchCriteria(
                profile,
                maxBudgetEUR,
                districts == null ? List.of() : districts,
                Math.max(0, minRooms),
                Math.max(0, minBedrooms),
                constructionType,
                heatingType,
                requiresElevator,
                requiresParking,
                requiresBalcony,
                priorities == null ? List.of() : priorities
        );
    }

    public BuyerProfile getProfile() {
        return profile;
    }

    public void setProfile(BuyerProfile profile) {
        this.profile = profile;
    }

    public BigDecimal getMaxBudgetEUR() {
        return maxBudgetEUR;
    }

    public void setMaxBudgetEUR(BigDecimal maxBudgetEUR) {
        this.maxBudgetEUR = maxBudgetEUR;
    }

    public List<District> getDistricts() {
        return districts;
    }

    public void setDistricts(List<District> districts) {
        this.districts = districts;
    }

    public int getMinRooms() {
        return minRooms;
    }

    public void setMinRooms(int minRooms) {
        this.minRooms = minRooms;
    }

    public int getMinBedrooms() {
        return minBedrooms;
    }

    public void setMinBedrooms(int minBedrooms) {
        this.minBedrooms = minBedrooms;
    }

    public ConstructionType getConstructionType() {
        return constructionType;
    }

    public void setConstructionType(ConstructionType constructionType) {
        this.constructionType = constructionType;
    }

    public HeatingType getHeatingType() {
        return heatingType;
    }

    public void setHeatingType(HeatingType heatingType) {
        this.heatingType = heatingType;
    }

    public boolean isRequiresElevator() {
        return requiresElevator;
    }

    public void setRequiresElevator(boolean requiresElevator) {
        this.requiresElevator = requiresElevator;
    }

    public boolean isRequiresParking() {
        return requiresParking;
    }

    public void setRequiresParking(boolean requiresParking) {
        this.requiresParking = requiresParking;
    }

    public boolean isRequiresBalcony() {
        return requiresBalcony;
    }

    public void setRequiresBalcony(boolean requiresBalcony) {
        this.requiresBalcony = requiresBalcony;
    }

    public List<String> getPriorities() {
        return priorities;
    }

    public void setPriorities(List<String> priorities) {
        this.priorities = priorities;
    }
}
