package com.plovdiv.advisor.dto;

public enum BuyerProfile {
    STUDENT("Student"),
    YOUNG_PROFESSIONAL("Young Professional"),
    FAMILY("Family"),
    RETIRED_PERSON("Retired Person"),
    INVESTOR("Investor");

    private final String displayName;

    BuyerProfile(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
