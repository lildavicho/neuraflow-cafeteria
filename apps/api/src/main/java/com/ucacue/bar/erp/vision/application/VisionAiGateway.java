package com.ucacue.bar.erp.vision.application;

import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.ConversionQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.FootfallQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.InsightsQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.PeakHoursQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.PredictionsQueryResponse;
import com.ucacue.bar.erp.vision.infrastructure.http.VisionSystem2Contracts.RangeQueryRequest;

public interface VisionAiGateway {

    FootfallQueryResponse footfall(RangeQueryRequest request);

    ConversionQueryResponse conversion(RangeQueryRequest request);

    PeakHoursQueryResponse peakHours(RangeQueryRequest request);

    InsightsQueryResponse insights(RangeQueryRequest request);

    PredictionsQueryResponse predictions(RangeQueryRequest request);
}
