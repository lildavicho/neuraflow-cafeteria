package com.ucacue.bar.erp.vision.infrastructure.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "integrations.vision-ai")
public class VisionAiProperties {

    private boolean enabled = true;

    private String baseUrl = "http://localhost:8000";

    private String apiKey;

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration readTimeout = Duration.ofSeconds(5);

    private String footfallPath = "/api/v1/traffic/footfall/query";

    private String conversionPath = "/api/v1/traffic/conversion/query";

    private String peakHoursPath = "/api/v1/traffic/peak-hours/query";

    private String insightsPath = "/api/v1/traffic/insights/query";

    private String predictionsPath = "/api/v1/predictions/query";
}
