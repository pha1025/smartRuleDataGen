package com.smartdata.smartruledatagen.service.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JdbcExecutor {

    private static final Logger log = LoggerFactory.getLogger(JdbcExecutor.class);

    private final Map<String, JdbcTemplate> jdbcTemplates; // 存储多个JdbcTemplate

    // 使用 @Qualifier 注解明确指定要注入哪个 JdbcTemplate Bean
    public JdbcExecutor(
            @Qualifier("db1JdbcTemplate") JdbcTemplate db1JdbcTemplate,
            @Qualifier("db2JdbcTemplate") JdbcTemplate db2JdbcTemplate
            // 如果有更多数据源，也需要在这里添加 @Qualifier 注解
    ) {
        this.jdbcTemplates = new HashMap<>();
        this.jdbcTemplates.put("db1", db1JdbcTemplate);
        this.jdbcTemplates.put("db2", db2JdbcTemplate);
        // ... 添加更多数据源
    }

    public void executeSql(String dbKey, String sql) {
        JdbcTemplate jdbcTemplate = jdbcTemplates.get(dbKey);
        if (jdbcTemplate == null) {
            log.error("No JdbcTemplate found for key: {}", dbKey);
            throw new IllegalArgumentException("Invalid database key: " + dbKey);
        }
        log.info("Executing SQL in {}: {}", dbKey, sql.substring(0, Math.min(sql.length(), 200)) + (sql.length() > 200 ? "..." : ""));
        try {
            jdbcTemplate.execute(sql);
            log.info("SQL executed successfully.");
        } catch (Exception e) {
            log.error("Failed to execute SQL in {}: {}", dbKey, sql, e);
            throw new RuntimeException("SQL execution failed.", e);
        }
    }

    public void executeBatchSql(String dbKey, List<String> sqlList) {
        JdbcTemplate jdbcTemplate = jdbcTemplates.get(dbKey);
        if (jdbcTemplate == null) {
            log.error("No JdbcTemplate found for key: {}", dbKey);
            throw new IllegalArgumentException("Invalid database key: " + dbKey);
        }
        log.info("Executing batch SQL in {}. Total {} statements.", dbKey, sqlList.size());
        try {
            // Spring JDBC 的 batchUpdate 可以直接接受 String[]
            String[] sqlArray = sqlList.toArray(new String[0]);
            jdbcTemplate.batchUpdate(sqlArray);
            log.info("Batch SQL executed successfully.");
        } catch (Exception e) {
            log.error("Failed to execute batch SQL in {}.", dbKey, e);
            throw new RuntimeException("Batch SQL execution failed.", e);
        }
    }
}