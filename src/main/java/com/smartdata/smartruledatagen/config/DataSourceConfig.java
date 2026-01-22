package com.smartdata.smartruledatagen.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // DB1 DataSource
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.db1")
    public DataSource db1DataSource() { // Bean name is "db1DataSource"
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate db1JdbcTemplate(@Qualifier("db1DataSource") DataSource dataSource) { // Bean name is "db1JdbcTemplate"
        return new JdbcTemplate(dataSource);
    }

    // DB2 DataSource
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.db2")
    public DataSource db2DataSource() { // Bean name is "db2DataSource"
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate db2JdbcTemplate(@Qualifier("db2DataSource") DataSource dataSource) { // Bean name is "db2JdbcTemplate"
        return new JdbcTemplate(dataSource);
    }

    // 可以添加更多数据源
}
