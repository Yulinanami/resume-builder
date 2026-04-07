package com.resumebuilder.springaibackend.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PgVectorConfig {

    @Bean(name = "pgVectorDataSource")
    DataSource pgVectorDataSource(PgVectorProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return dataSource;
    }

    @Bean(name = "pgVectorJdbcTemplate")
    JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    VectorStore vectorStore(
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel
    ) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .initializeSchema(true)
                .build();
    }
}
