package com.plovdiv.advisor.ontology;

import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.dto.PropertyType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OntologyService {

    public static final String BASE_IRI = "http://www.semanticweb.org/plovdiv-real-estate";

    private final Path ontologyPath;
    private final OWLOntologyManager manager;
    private final OWLDataFactory dataFactory;
    private OWLOntology ontology;

    @Autowired
    public OntologyService(@Value("${app.ontology.path}") String ontologyPath) {
        this(Path.of(ontologyPath));
    }

    public OntologyService(Path ontologyPath) {
        this.ontologyPath = ontologyPath;
        this.manager = OWLManager.createOWLOntologyManager();
        this.dataFactory = manager.getOWLDataFactory();
        load();
    }

    public final synchronized void load() {
        try {
            if (this.ontology != null) {
                manager.removeOntology(this.ontology);
            }
            this.ontology = manager.loadOntologyFromOntologyDocument(ontologyPath.toFile());
        } catch (OWLOntologyCreationException ex) {
            throw new OntologyException("Failed to load ontology from " + ontologyPath, ex);
        }
    }

    public synchronized void save() {
        try {
            manager.saveOntology(ontology, IRI.create(ontologyPath.toFile()));
        } catch (OWLOntologyStorageException ex) {
            throw new OntologyException("Failed to save ontology to " + ontologyPath, ex);
        }
    }

    public synchronized void upsertProperty(PropertyImportRow row) {
        OWLNamedIndividual property = propertyIndividual(row.id());
        removeSubjectAssertions(property);

        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(dataFactory.getOWLClassAssertionAxiom(owlClass(row.type() == PropertyType.APARTMENT ? "Apartment" : "House"), property));
        axioms.add(dataFactory.getOWLClassAssertionAxiom(owlClass("Property"), property));

        addObjectAssertion(axioms, "locatedInNeighborhood", property, "Neighborhood_" + row.district().csvValue());
        addObjectAssertion(axioms, "hasConstructionType", property, "ConstructionType_" + individualToken(row.constructionType()));
        addObjectAssertion(axioms, "hasHeatingType", property, "HeatingType_" + individualToken(row.heatingType()));

        addString(axioms, property, "hasTitle", row.title());
        addDecimal(axioms, property, "hasPriceEUR", row.priceEUR());
        addDecimal(axioms, property, "hasAreaSqM", row.areaSqM());
        addDecimal(axioms, property, "hasPricePerSqM", row.pricePerSqM());
        addInteger(axioms, property, "hasRooms", row.rooms());
        addInteger(axioms, property, "hasBedrooms", row.bedrooms());
        addInteger(axioms, property, "hasFloor", row.floor());
        addInteger(axioms, property, "hasTotalFloors", row.totalFloors());
        addInteger(axioms, property, "hasYearBuilt", row.yearBuilt());
        addBoolean(axioms, property, "isAvailable", row.isAvailable());
        addBoolean(axioms, property, "hasElevator", row.hasElevator());
        addBoolean(axioms, property, "hasParking", row.hasParking());
        addBoolean(axioms, property, "hasBalcony", row.hasBalcony());
        addDecimal(axioms, property, "hasLatitude", row.latitude());
        addDecimal(axioms, property, "hasLongitude", row.longitude());
        addInteger(axioms, property, "hasDistanceToSchool", row.distanceToSchool());
        addInteger(axioms, property, "hasDistanceToKindergarten", row.distanceToKindergarten());
        addInteger(axioms, property, "hasDistanceToUniversity", row.distanceToUniversity());
        addInteger(axioms, property, "hasDistanceToPark", row.distanceToPark());
        addInteger(axioms, property, "hasDistanceToTransport", row.distanceToTransport());
        addInteger(axioms, property, "hasDistanceToHospital", row.distanceToHospital());
        addInteger(axioms, property, "hasDistanceToPharmacy", row.distanceToPharmacy());

        addSuitabilityAssertions(axioms, property, row);
        manager.addAxioms(ontology, new HashSet<>(axioms));
    }

    public synchronized void upsertProperties(List<PropertyImportRow> rows) {
        rows.forEach(this::upsertProperty);
    }

    public synchronized void updatePrice(String propertyId, BigDecimal priceEUR) {
        OWLNamedIndividual property = propertyIndividual(propertyId);
        replaceDataProperty(property, "hasPriceEUR", dataFactory.getOWLLiteral(priceEUR.toPlainString(), OWL2Datatype.XSD_DECIMAL));
        BigDecimal area = decimalValue(property, "hasAreaSqM");
        BigDecimal pricePerSqM = priceEUR.divide(area, 2, java.math.RoundingMode.HALF_UP);
        replaceDataProperty(property, "hasPricePerSqM", dataFactory.getOWLLiteral(pricePerSqM.toPlainString(), OWL2Datatype.XSD_DECIMAL));
    }

    public synchronized void updateAvailability(String propertyId, boolean available) {
        OWLNamedIndividual property = propertyIndividual(propertyId);
        replaceDataProperty(property, "isAvailable", dataFactory.getOWLLiteral(available));
        if (!available) {
            Set<OWLAxiom> suitabilityAssertions = ontology.objectPropertyAssertionAxioms(property)
                    .filter(axiom -> axiom.getProperty().equals(objectProperty("suitableForProfile")))
                    .collect(java.util.stream.Collectors.toSet());
            manager.removeAxioms(ontology, suitabilityAssertions);
        }
    }

    public synchronized Optional<PropertyOntologyRecord> findProperty(String propertyId) {
        OWLNamedIndividual property = propertyIndividual(propertyId);
        if (ontology.classAssertionAxioms(property).findAny().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PropertyOntologyRecord(
                propertyId,
                stringValue(property, "hasTitle"),
                propertyType(property),
                district(property),
                decimalValue(property, "hasPriceEUR"),
                decimalValue(property, "hasAreaSqM"),
                decimalValue(property, "hasPricePerSqM"),
                integerValue(property, "hasRooms"),
                integerValue(property, "hasBedrooms"),
                integerValue(property, "hasFloor"),
                integerValue(property, "hasTotalFloors"),
                constructionType(property),
                integerValue(property, "hasYearBuilt"),
                heatingType(property),
                booleanValue(property, "isAvailable"),
                booleanValue(property, "hasElevator"),
                booleanValue(property, "hasParking"),
                booleanValue(property, "hasBalcony"),
                decimalValue(property, "hasLatitude"),
                decimalValue(property, "hasLongitude"),
                integerValue(property, "hasDistanceToSchool"),
                integerValue(property, "hasDistanceToKindergarten"),
                integerValue(property, "hasDistanceToUniversity"),
                integerValue(property, "hasDistanceToPark"),
                integerValue(property, "hasDistanceToTransport"),
                integerValue(property, "hasDistanceToHospital"),
                integerValue(property, "hasDistanceToPharmacy"),
                suitableProfiles(property)
        ));
    }

    private void addSuitabilityAssertions(List<OWLAxiom> axioms, OWLNamedIndividual property, PropertyImportRow row) {
        if (!row.isAvailable()) {
            return;
        }
        if (row.bedrooms() >= 2 && row.distanceToSchool() <= 800 && row.distanceToKindergarten() <= 800) {
            addObjectAssertion(axioms, "suitableForProfile", property, "FamilyProfile");
        }
        if (row.distanceToUniversity() <= 1500 && row.distanceToTransport() <= 500) {
            addObjectAssertion(axioms, "suitableForProfile", property, "StudentProfile");
        }
        if ((row.hasElevator() || row.floor() <= 2) && row.distanceToPharmacy() <= 700 && row.distanceToTransport() <= 500) {
            addObjectAssertion(axioms, "suitableForProfile", property, "RetiredProfile");
        }
        if (row.pricePerSqM().compareTo(new BigDecimal("1600")) <= 0 && row.areaSqM().compareTo(new BigDecimal("130")) <= 0) {
            addObjectAssertion(axioms, "suitableForProfile", property, "InvestorProfile");
        }
        if (row.distanceToTransport() <= 500 && (row.hasParking() || row.distanceToPark() <= 1000)) {
            addObjectAssertion(axioms, "suitableForProfile", property, "YoungProfessionalProfile");
        }
    }

    private void removeSubjectAssertions(OWLNamedIndividual property) {
        Set<OWLAxiom> axioms = ontology.axioms(property).collect(java.util.stream.Collectors.toSet());
        manager.removeAxioms(ontology, axioms);
    }

    private void replaceDataProperty(OWLNamedIndividual subject, String propertyName, org.semanticweb.owlapi.model.OWLLiteral literal) {
        OWLDataProperty dataProperty = dataProperty(propertyName);
        Set<OWLAxiom> existing = ontology.dataPropertyAssertionAxioms(subject)
                .filter(axiom -> axiom.getProperty().equals(dataProperty))
                .collect(java.util.stream.Collectors.toSet());
        manager.removeAxioms(ontology, existing);
        manager.addAxiom(ontology, dataFactory.getOWLDataPropertyAssertionAxiom(dataProperty, subject, literal));
    }

    private void addObjectAssertion(List<OWLAxiom> axioms, String propertyName, OWLNamedIndividual subject, String objectName) {
        axioms.add(dataFactory.getOWLObjectPropertyAssertionAxiom(
                objectProperty(propertyName),
                subject,
                individual(objectName)
        ));
    }

    private void addString(List<OWLAxiom> axioms, OWLNamedIndividual subject, String propertyName, String value) {
        axioms.add(dataFactory.getOWLDataPropertyAssertionAxiom(dataProperty(propertyName), subject, value));
    }

    private void addDecimal(List<OWLAxiom> axioms, OWLNamedIndividual subject, String propertyName, BigDecimal value) {
        axioms.add(dataFactory.getOWLDataPropertyAssertionAxiom(
                dataProperty(propertyName),
                subject,
                dataFactory.getOWLLiteral(value.toPlainString(), OWL2Datatype.XSD_DECIMAL)
        ));
    }

    private void addInteger(List<OWLAxiom> axioms, OWLNamedIndividual subject, String propertyName, int value) {
        axioms.add(dataFactory.getOWLDataPropertyAssertionAxiom(dataProperty(propertyName), subject, value));
    }

    private void addBoolean(List<OWLAxiom> axioms, OWLNamedIndividual subject, String propertyName, boolean value) {
        axioms.add(dataFactory.getOWLDataPropertyAssertionAxiom(dataProperty(propertyName), subject, value));
    }

    private PropertyType propertyType(OWLNamedIndividual property) {
        boolean house = ontology.classAssertionAxioms(property)
                .anyMatch(axiom -> axiom.getClassExpression().equals(owlClass("House")));
        return house ? PropertyType.HOUSE : PropertyType.APARTMENT;
    }

    private District district(OWLNamedIndividual property) {
        String token = objectValueToken(property, "locatedInNeighborhood", "Neighborhood_");
        for (District district : District.values()) {
            if (district.csvValue().equals(token)) {
                return district;
            }
        }
        throw new IllegalStateException("Unknown district individual token " + token);
    }

    private ConstructionType constructionType(OWLNamedIndividual property) {
        return ConstructionType.valueOf(objectValueToken(property, "hasConstructionType", "ConstructionType_"));
    }

    private HeatingType heatingType(OWLNamedIndividual property) {
        return HeatingType.valueOf(objectValueToken(property, "hasHeatingType", "HeatingType_"));
    }

    private Set<String> suitableProfiles(OWLNamedIndividual property) {
        return EntitySearcher.getObjectPropertyValues(property, objectProperty("suitableForProfile"), ontology)
                .filter(value -> value.asOWLNamedIndividual().isNamed())
                .map(value -> shortForm(value.asOWLNamedIndividual().getIRI()))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private String objectValueToken(OWLNamedIndividual property, String objectPropertyName, String prefix) {
        return EntitySearcher.getObjectPropertyValues(property, objectProperty(objectPropertyName), ontology)
                .findFirst()
                .map(value -> shortForm(value.asOWLNamedIndividual().getIRI()).replaceFirst("^" + prefix, ""))
                .orElseThrow(() -> new IllegalStateException("Missing object property " + objectPropertyName));
    }

    private String stringValue(OWLNamedIndividual property, String propertyName) {
        return literal(property, propertyName).getLiteral();
    }

    private BigDecimal decimalValue(OWLNamedIndividual property, String propertyName) {
        return new BigDecimal(literal(property, propertyName).getLiteral());
    }

    private int integerValue(OWLNamedIndividual property, String propertyName) {
        return Integer.parseInt(literal(property, propertyName).getLiteral());
    }

    private boolean booleanValue(OWLNamedIndividual property, String propertyName) {
        return Boolean.parseBoolean(literal(property, propertyName).getLiteral());
    }

    private org.semanticweb.owlapi.model.OWLLiteral literal(OWLNamedIndividual property, String propertyName) {
        return EntitySearcher.getDataPropertyValues(property, dataProperty(propertyName), ontology)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing data property " + propertyName));
    }

    private OWLNamedIndividual propertyIndividual(String propertyId) {
        return individual("Property_" + propertyId);
    }

    private OWLClass owlClass(String name) {
        return dataFactory.getOWLClass(iri(name));
    }

    private OWLDataProperty dataProperty(String name) {
        return dataFactory.getOWLDataProperty(iri(name));
    }

    private OWLObjectProperty objectProperty(String name) {
        return dataFactory.getOWLObjectProperty(iri(name));
    }

    private OWLNamedIndividual individual(String name) {
        return dataFactory.getOWLNamedIndividual(iri(name));
    }

    private IRI iri(String localName) {
        return IRI.create(BASE_IRI + "#" + localName);
    }

    private String shortForm(IRI iri) {
        return iri.getShortForm();
    }

    private String individualToken(Enum<?> value) {
        return value.name();
    }

    public synchronized List<String> findAllPropertyIds() {
        return ontology.classAssertionAxioms(owlClass("Property"))
                .map(org.semanticweb.owlapi.model.OWLClassAssertionAxiom::getIndividual)
                .filter(org.semanticweb.owlapi.model.OWLIndividual::isNamed)
                .map(ind -> shortForm(ind.asOWLNamedIndividual().getIRI()).replaceFirst("^Property_", ""))
                .distinct()
                .toList();
    }
}
