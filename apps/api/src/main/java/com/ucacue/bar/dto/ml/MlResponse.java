package com.ucacue.bar.dto.ml;

public record MlResponse<T>(String status, String message, Double confidenceScore, T data) {

    private static final String STATUS_OK = "ok";
    private static final String STATUS_INSUFFICIENT = "insufficient_data";
    private static final String MESSAGE_OK = "ok";
    private static final String MESSAGE_INSUFFICIENT = "insufficient data";

    public static <T> MlResponse<T> ok(T data, double confidenceScore) {
        return new MlResponse<>(STATUS_OK, MESSAGE_OK, round(confidenceScore), data);
    }

    public static <T> MlResponse<T> insufficient() {
        return new MlResponse<>(STATUS_INSUFFICIENT, MESSAGE_INSUFFICIENT, 0.0, null);
    }

    private static double round(double value) {
        return Math.round(Math.max(0.0, Math.min(1.0, value)) * 10000.0) / 10000.0;
    }
}
