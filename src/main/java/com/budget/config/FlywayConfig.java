package com.budget.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class FlywayConfig {
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/budgetapp");
        dataSource.setUsername("budgetapp");
        dataSource.setPassword("password");
        return dataSource;
    }

    @Bean
    public Flyway flyway(DataSource dataSource) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:/flyway/migrations")
                .schemas("budget")
                .cleanDisabled(false);

        Flyway flyway = new Flyway(configuration);
        flyway.migrate();  // Run migrations automatically on startup if needed
        return flyway;
    }
}