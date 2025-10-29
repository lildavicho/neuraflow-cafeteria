package com.ucacue.bar.config;

import com.algolia.api.SearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlgoliaConfig {

    @Value("${algolia.application-id}")
    private String appId;

    @Value("${algolia.api-key}")
    private String apiKey;

    @Bean
    public SearchClient searchClient() {
        // Algolia Java v4 client
        return new SearchClient(appId, apiKey);
    }
}
