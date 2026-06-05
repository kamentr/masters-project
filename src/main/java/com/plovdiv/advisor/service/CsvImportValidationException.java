package com.plovdiv.advisor.service;

import java.util.List;

public class CsvImportValidationException extends RuntimeException {

    private final List<String> errors;

    public CsvImportValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
