package com.plovdiv.advisor.web;

import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.PropertyImportRow;
import com.plovdiv.advisor.dto.PropertyType;
import com.plovdiv.advisor.ontology.PropertyOntologyRecord;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PropertyEditForm {

    @NotBlank
    private String id;

    @NotBlank
    private String title;

    @NotNull
    private PropertyType type;

    @NotNull
    private District district;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal priceEUR;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal areaSqM;

    @Min(1)
    private int rooms;

    @Min(0)
    private int bedrooms;

    @Min(0)
    private int floor;

    @Min(1)
    private int totalFloors;

    @NotNull
    private ConstructionType constructionType;

    @Min(1850)
    @Max(2027)
    private int yearBuilt;

    @NotNull
    private HeatingType heatingType;

    private boolean hasElevator;
    private boolean hasParking;
    private boolean hasBalcony;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    private boolean available;

    @Min(0)
    private int distanceToSchool;

    @Min(0)
    private int distanceToKindergarten;

    @Min(0)
    private int distanceToUniversity;

    @Min(0)
    private int distanceToPark;

    @Min(0)
    private int distanceToTransport;

    @Min(0)
    private int distanceToHospital;

    @Min(0)
    private int distanceToPharmacy;

    public static PropertyEditForm fromRecord(PropertyOntologyRecord record) {
        PropertyEditForm form = new PropertyEditForm();
        form.id = record.id();
        form.title = record.title();
        form.type = record.type();
        form.district = record.district();
        form.priceEUR = record.priceEUR();
        form.areaSqM = record.areaSqM();
        form.rooms = record.rooms();
        form.bedrooms = record.bedrooms();
        form.floor = record.floor();
        form.totalFloors = record.totalFloors();
        form.constructionType = record.constructionType();
        form.yearBuilt = record.yearBuilt();
        form.heatingType = record.heatingType();
        form.hasElevator = record.hasElevator();
        form.hasParking = record.hasParking();
        form.hasBalcony = record.hasBalcony();
        form.latitude = record.latitude();
        form.longitude = record.longitude();
        form.available = record.available();
        form.distanceToSchool = record.distanceToSchool();
        form.distanceToKindergarten = record.distanceToKindergarten();
        form.distanceToUniversity = record.distanceToUniversity();
        form.distanceToPark = record.distanceToPark();
        form.distanceToTransport = record.distanceToTransport();
        form.distanceToHospital = record.distanceToHospital();
        form.distanceToPharmacy = record.distanceToPharmacy();
        return form;
    }

    public PropertyImportRow toImportRow() {
        return new PropertyImportRow(
                id,
                title,
                type,
                district,
                priceEUR,
                areaSqM,
                rooms,
                bedrooms,
                floor,
                totalFloors,
                constructionType,
                yearBuilt,
                heatingType,
                hasElevator,
                hasParking,
                hasBalcony,
                latitude,
                longitude,
                available,
                distanceToSchool,
                distanceToKindergarten,
                distanceToUniversity,
                distanceToPark,
                distanceToTransport,
                distanceToHospital,
                distanceToPharmacy
        );
    }

    public boolean hasConsistentFloors() {
        return floor <= totalFloors;
    }
}
