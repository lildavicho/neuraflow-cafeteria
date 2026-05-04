package com.ucacue.bar.config;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateQueryCountingConfig {

    @Bean
    public HibernatePropertiesCustomizer queryCountingCustomizer(
            @Value("${app.performance.query-count-enabled:false}") boolean enabled) {
        return properties -> {
            if (enabled) {
                properties.put("hibernate.session_factory.statement_inspector",
                        (StatementInspector) sql -> {
                            SqlQueryCounter.increment();
                            return sql;
                        });
            }
        };
    }
}
