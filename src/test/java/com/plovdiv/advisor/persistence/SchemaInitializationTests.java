package com.plovdiv.advisor.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaInitializationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsApplicationTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM sqlite_master
                        WHERE type = 'table'
                        AND name IN ('users', 'search_history', 'favorites', 'feedback', 'import_batches', 'agent_logs')
                        """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(6);
    }
}
