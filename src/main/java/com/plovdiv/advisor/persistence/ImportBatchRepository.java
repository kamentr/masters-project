package com.plovdiv.advisor.persistence;

import com.plovdiv.advisor.dto.ImportBatchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class ImportBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public ImportBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ImportBatchResult create(String fileName) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO import_batches (file_name, total_rows, imported_rows, failed_rows, status)
                            VALUES (?, 0, 0, 0, 'PENDING')
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, fileName);
            return ps;
        }, keyHolder);
        return new ImportBatchResult(keyHolder.getKey().longValue(), "PENDING", 0, 0, 0, null);
    }

    public ImportBatchResult complete(long batchId, int totalRows, int importedRows) {
        jdbcTemplate.update(
                """
                        UPDATE import_batches
                        SET total_rows = ?, imported_rows = ?, failed_rows = 0, status = 'SUCCESS', error_summary = NULL
                        WHERE id = ?
                        """,
                totalRows,
                importedRows,
                batchId
        );
        return new ImportBatchResult(batchId, "SUCCESS", totalRows, importedRows, 0, null);
    }

    public ImportBatchResult fail(long batchId, int totalRows, String errorSummary) {
        jdbcTemplate.update(
                """
                        UPDATE import_batches
                        SET total_rows = ?, imported_rows = 0, failed_rows = ?, status = 'FAILED', error_summary = ?
                        WHERE id = ?
                        """,
                totalRows,
                totalRows,
                errorSummary,
                batchId
        );
        return new ImportBatchResult(batchId, "FAILED", totalRows, 0, totalRows, errorSummary);
    }
}
