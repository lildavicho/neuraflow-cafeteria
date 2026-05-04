package com.ucacue.bar.erp.vision.infrastructure.http;

import com.ucacue.bar.erp.shared.exception.ServiceUnavailableException;
import com.ucacue.bar.erp.vision.application.VisionAiGateway;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.ConversionQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.FootfallQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.InsightsQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.PeakHoursQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.PredictionsQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.RangeQueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class FastApiVisionAiGateway implements VisionAiGateway {

    private final RestClient restClient;
    private final VisionAiProperties properties;

    @Override
    public FootfallQueryResponse footfall(RangeQueryRequest request) {
        return post(properties.getFootfallPath(), request, FootfallQueryResponse.class, "afluencia");
    }

    @Override
    public ConversionQueryResponse conversion(RangeQueryRequest request) {
        return post(properties.getConversionPath(), request, ConversionQueryResponse.class, "conversion");
    }

    @Override
    public PeakHoursQueryResponse peakHours(RangeQueryRequest request) {
        return post(properties.getPeakHoursPath(), request, PeakHoursQueryResponse.class, "horas pico");
    }

    @Override
    public InsightsQueryResponse insights(RangeQueryRequest request) {
        return post(properties.getInsightsPath(), request, InsightsQueryResponse.class, "insights");
    }

    @Override
    public PredictionsQueryResponse predictions(RangeQueryRequest request) {
        return post(properties.getPredictionsPath(), request, PredictionsQueryResponse.class, "predicciones");
    }

    private <T> T post(String path, Object requestBody, Class<T> responseType, String operation) {
        if (!properties.isEnabled()) {
            throw new ServiceUnavailableException("La integracion Vision AI esta deshabilitada");
        }

        try {
            return restClient.post()
                    .uri(path)
                    .headers(this::applyHeaders)
                    .body(requestBody)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException ex) {
            throw new ServiceUnavailableException(
                    "Sistema 2 respondio con error durante " + operation + " (" + ex.getRawStatusCode() + ")",
                    ex);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("No fue posible contactar a Sistema 2 para " + operation, ex);
        }
    }

    private void applyHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.ACCEPT, "application/json");
        if (StringUtils.hasText(properties.getApiKey())) {
            headers.set("X-API-Key", properties.getApiKey());
        }
    }
}
