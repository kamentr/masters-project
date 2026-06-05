package com.plovdiv.advisor.dto;

public enum District {
    CENTER("Center"),
    OLD_TOWN("OldTown"),
    KAPANA("Kapana"),
    KAMENITSA("Kamenitsa"),
    KARSHIYAKA("Karshiyaka"),
    TRAKIA("Trakia"),
    KUCHUK_PARIS("KuchukParis"),
    SMIRNENSKI("Smirnenski"),
    KOMATEVO("Komatevo"),
    PROSLAV("Proslav"),
    OSTROMILA("Ostromila"),
    BELOMORSKI("Belomorski");

    private final String csvValue;

    District(String csvValue) {
        this.csvValue = csvValue;
    }

    public String csvValue() {
        return csvValue;
    }
}
