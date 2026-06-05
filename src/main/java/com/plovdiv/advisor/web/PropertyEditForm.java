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

import java.math.BigDecimal;

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public PropertyType getType() { return type; }
    public void setType(PropertyType type) { this.type = type; }
    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }
    public BigDecimal getPriceEUR() { return priceEUR; }
    public void setPriceEUR(BigDecimal priceEUR) { this.priceEUR = priceEUR; }
    public BigDecimal getAreaSqM() { return areaSqM; }
    public void setAreaSqM(BigDecimal areaSqM) { this.areaSqM = areaSqM; }
    public int getRooms() { return rooms; }
    public void setRooms(int rooms) { this.rooms = rooms; }
    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public int getTotalFloors() { return totalFloors; }
    public void setTotalFloors(int totalFloors) { this.totalFloors = totalFloors; }
    public ConstructionType getConstructionType() { return constructionType; }
    public void setConstructionType(ConstructionType constructionType) { this.constructionType = constructionType; }
    public int getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(int yearBuilt) { this.yearBuilt = yearBuilt; }
    public HeatingType getHeatingType() { return heatingType; }
    public void setHeatingType(HeatingType heatingType) { this.heatingType = heatingType; }
    public boolean isHasElevator() { return hasElevator; }
    public void setHasElevator(boolean hasElevator) { this.hasElevator = hasElevator; }
    public boolean isHasParking() { return hasParking; }
    public void setHasParking(boolean hasParking) { this.hasParking = hasParking; }
    public boolean isHasBalcony() { return hasBalcony; }
    public void setHasBalcony(boolean hasBalcony) { this.hasBalcony = hasBalcony; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getDistanceToSchool() { return distanceToSchool; }
    public void setDistanceToSchool(int distanceToSchool) { this.distanceToSchool = distanceToSchool; }
    public int getDistanceToKindergarten() { return distanceToKindergarten; }
    public void setDistanceToKindergarten(int distanceToKindergarten) { this.distanceToKindergarten = distanceToKindergarten; }
    public int getDistanceToUniversity() { return distanceToUniversity; }
    public void setDistanceToUniversity(int distanceToUniversity) { this.distanceToUniversity = distanceToUniversity; }
    public int getDistanceToPark() { return distanceToPark; }
    public void setDistanceToPark(int distanceToPark) { this.distanceToPark = distanceToPark; }
    public int getDistanceToTransport() { return distanceToTransport; }
    public void setDistanceToTransport(int distanceToTransport) { this.distanceToTransport = distanceToTransport; }
    public int getDistanceToHospital() { return distanceToHospital; }
    public void setDistanceToHospital(int distanceToHospital) { this.distanceToHospital = distanceToHospital; }
    public int getDistanceToPharmacy() { return distanceToPharmacy; }
    public void setDistanceToPharmacy(int distanceToPharmacy) { this.distanceToPharmacy = distanceToPharmacy; }
}
