package com.ucacue.bar.config;

import com.algolia.search.DefaultSearchClient;
import com.algolia.search.SearchClient;
import com.algolia.search.SearchIndex;
import com.ucacue.bar.dto.ProductDTO;
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
        return DefaultSearchClient.create(appId, apiKey);
    }

    @Bean
    public SearchIndex<ProductDTO> productIndex(SearchClient searchClient) {
        return searchClient.initIndex("products", ProductDTO.class);
    }
}
