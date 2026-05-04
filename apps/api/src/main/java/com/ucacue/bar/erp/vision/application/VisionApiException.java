package com.ucacue.bar.erp.vision.application;

import org.springframework.http.HttpStatus;

public class VisionApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public VisionApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
