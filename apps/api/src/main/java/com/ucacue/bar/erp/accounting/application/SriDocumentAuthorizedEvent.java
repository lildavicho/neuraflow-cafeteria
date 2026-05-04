package com.ucacue.bar.erp.accounting.application;

public record SriDocumentAuthorizedEvent(
        Long tenantId,
        Long taxDocumentId,
        String accessKey,
        String authorizationCode) {
}
