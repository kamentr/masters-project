package com.plovdiv.advisor.persistence;

import com.plovdiv.advisor.dto.ImportBatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatchEntity, Long> {

    default ImportBatchResult create(String fileName) {
        ImportBatchEntity entity = new ImportBatchEntity();
        entity.setFileName(fileName);
        entity.setStatus("PENDING");
        return toResult(save(entity));
    }

    default ImportBatchResult complete(long batchId, int totalRows, int importedRows) {
        ImportBatchEntity entity = getReferenceById(batchId);
        entity.setTotalRows(totalRows);
        entity.setImportedRows(importedRows);
        entity.setFailedRows(0);
        entity.setStatus("SUCCESS");
        entity.setErrorSummary(null);
        return toResult(save(entity));
    }

    default ImportBatchResult fail(long batchId, int totalRows, String errorSummary) {
        ImportBatchEntity entity = getReferenceById(batchId);
        entity.setTotalRows(totalRows);
        entity.setImportedRows(0);
        entity.setFailedRows(totalRows);
        entity.setStatus("FAILED");
        entity.setErrorSummary(errorSummary);
        return toResult(save(entity));
    }

    private static ImportBatchResult toResult(ImportBatchEntity entity) {
        return new ImportBatchResult(
                entity.getId(),
                entity.getStatus(),
                entity.getTotalRows(),
                entity.getImportedRows(),
                entity.getFailedRows(),
                entity.getErrorSummary()
        );
    }
}
