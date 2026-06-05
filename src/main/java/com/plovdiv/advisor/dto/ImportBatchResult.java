package com.plovdiv.advisor.dto;

public record ImportBatchResult(
        long batchId,
        String status,
        int totalRows,
        int importedRows,
        int failedRows,
        String errorSummary
) {
    public boolean successful() {
        return "SUCCESS".equals(status);
    }
}
