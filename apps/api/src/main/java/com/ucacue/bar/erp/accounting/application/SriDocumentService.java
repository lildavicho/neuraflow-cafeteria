package com.ucacue.bar.erp.accounting.application;

import com.ucacue.bar.config.RequestTraceFilter;
import com.ucacue.bar.erp.accounting.domain.AccountingTypes.TaxDocumentStatus;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.SriConfigurationEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTaxLineEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentTransmissionEntity;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.SriConfigurationRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentTaxLineRepository;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.repository.TaxDocumentTransmissionRepository;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriCredentialCryptoService;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriSoapClient;
import com.ucacue.bar.erp.accounting.infrastructure.sri.SriXmlSignatureService;
import com.ucacue.bar.erp.shared.application.TenantContextResolver;
import com.ucacue.bar.erp.shared.exception.ServiceUnavailableException;
import com.ucacue.bar.exception.BadRequestException;
import com.ucacue.bar.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SriDocumentService {

    private static final String SOURCE_TYPE_ORDER_SALE = "ORDER_SALE";

    private final TenantContextResolver tenantContextResolver;
    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxDocumentTaxLineRepository taxDocumentTaxLineRepository;
    private final TaxDocumentTransmissionRepository taxDocumentTransmissionRepository;
    private final SriConfigurationRepository sriConfigurationRepository;
    private final SriDocumentPreparationService sriDocumentPreparationService;
    private final SriCredentialCryptoService sriCredentialCryptoService;
    private final SriXmlSignatureService sriXmlSignatureService;
    private final SriSoapClient sriSoapClient;
    private final SriProperties sriProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<TaxDocumentSummary> list(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return taxDocumentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 200)).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SriConfigurationView getConfiguration(String tenantCode) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return sriConfigurationRepository.findByTenantId(tenantId)
                .map(this::toConfigurationView)
                .orElseThrow(() -> new NotFoundException("No existe configuracion SRI para el tenant solicitado"));
    }

    @Transactional
    public SriConfigurationView upsertConfiguration(String tenantCode, UpsertSriConfigurationCommand command) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        validateConfiguration(command);
        SriConfigurationEntity entity = sriConfigurationRepository.findByTenantId(tenantId)
                .orElseGet(SriConfigurationEntity::new);
        entity.setTenantId(tenantId);
        entity.setEnvironmentCode(normalizeEnvironmentCode(command.environmentCode()));
        entity.setEmissionCode(normalizeEmissionCode(command.emissionCode()));
        entity.setIssuerRuc(digitsOnly(command.issuerRuc()));
        entity.setIssuerLegalName(command.issuerLegalName().trim());
        entity.setIssuerTradeName(trimToNull(command.issuerTradeName()));
        entity.setMatrixAddress(command.matrixAddress().trim());
        entity.setEstablishmentAddress(command.establishmentAddress().trim());
        entity.setSpecialTaxpayer(trimToNull(command.specialTaxpayer()));
        entity.setObligatedAccounting(normalizeObligatedAccounting(command.obligatedAccounting()));
        entity.setRimpeContributor(Boolean.TRUE.equals(command.rimpeContributor()));
        entity.setRetentionAgent(Boolean.TRUE.equals(command.retentionAgent()));
        entity.setRetentionAgentResolution(trimToNull(command.retentionAgentResolution()));
        entity.setActive(command.active() == null || command.active());
        applySignatureConfiguration(entity, command);
        return toConfigurationView(sriConfigurationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TaxDocumentTransmissionSummary> listTransmissions(String tenantCode, Long taxDocumentId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        return taxDocumentTransmissionRepository.findByTaxDocumentIdOrderByAttemptedAtDesc(document.getId()).stream()
                .map(this::toTransmissionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxDocumentTransmissionDetailView getTransmission(String tenantCode, Long taxDocumentId, Long transmissionId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        TaxDocumentTransmissionEntity transmission = taxDocumentTransmissionRepository
                .findByIdAndTaxDocumentId(transmissionId, document.getId())
                .orElseThrow(() -> new NotFoundException("Transmision SRI no encontrada"));
        return toTransmissionDetail(transmission);
    }

    @Transactional(readOnly = true)
    public TaxDocumentStatusView getStatus(String tenantCode, Long taxDocumentId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        return buildStatusView(document);
    }

    @Transactional
    @CacheEvict(cacheNames = "sri-health", allEntries = true)
    public TaxDocumentSummary emitBySaleId(String tenantCode, Long saleId) {
        if (saleId == null) {
            throw new BadRequestException("El id de la venta es obligatorio");
        }
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        TaxDocumentEntity document = taxDocumentRepository
                .findByTenantIdAndSourceTypeAndSourceId(tenantId, SOURCE_TYPE_ORDER_SALE, saleId)
                .orElseThrow(() -> new NotFoundException("No existe factura tributaria para la venta solicitada"));
        return emit(tenantCode, document.getId());
    }

    @Transactional(readOnly = true)
    public TaxDocumentXmlView getXml(String tenantCode, Long taxDocumentId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        String xml = document.getSignedXmlPayload() != null && !document.getSignedXmlPayload().isBlank()
                ? document.getSignedXmlPayload()
                : document.getXmlPayload();
        if (xml == null || xml.isBlank()) {
            throw new NotFoundException("El documento aun no tiene XML generado");
        }
        return new TaxDocumentXmlView(document.getId(), document.getAccessKey(), document.getStatus().name(), xml);
    }

    @Transactional(readOnly = true)
    public TaxDocumentDiagnosticsView getDiagnostics(String tenantCode, Long taxDocumentId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        SriConfigurationEntity configuration = requireConfiguration(document.getTenantId());
        TaxDocumentStatusView status = buildStatusView(document);
        List<String> blockers = new ArrayList<>();
        List<String> manualChecks = new ArrayList<>();

        if (!"2".equals(configuration.getEnvironmentCode())) {
            blockers.add("El emisor sigue configurado fuera de ambiente 2.");
        }
        if (configuration.getIssuerRuc() == null || configuration.getIssuerRuc().length() != 13) {
            blockers.add("El RUC emisor configurado no es valido.");
        }
        if (document.getAccessKey() == null || document.getAccessKey().isBlank()) {
            blockers.add("El documento aun no tiene clave de acceso generada.");
        }
        if (document.getStatus() == TaxDocumentStatus.REJECTED) {
            blockers.add("El documento sigue rechazado y requiere correccion antes del piloto.");
        }
        if ("PKCS12".equals(normalizeSignatureMode(configuration.getSignatureMode()))
                && (configuration.getP12Content() == null || configuration.getP12Content().length == 0
                || isBlank(configuration.getP12PasswordEncrypted())
                || isBlank(configuration.getSriEncryptionSalt()))) {
            blockers.add("El tenant no tiene certificado PKCS12, clave cifrada y salt configurados.");
        }
        if (!"PKCS12".equals(normalizeSignatureMode(configuration.getSignatureMode()))) {
            manualChecks.add("La firma SRI del tenant no esta en modo PKCS12; solo PKCS12 debe usarse para emision real.");
        }

        manualChecks.add("Confirmar que el certificado PKCS12 vigente corresponde al RUC emisor configurado.");
        manualChecks.add("Guardar XML firmado, request SOAP, response SOAP y trace id del ultimo intento.");
        manualChecks.add("Validar manualmente en ambiente 2 el ciclo recepcion -> autorizacion con un documento real.");
        manualChecks.add("Registrar numero de autorizacion, fecha y evidencia PDF/XML antes del uso con cliente.");

        return new TaxDocumentDiagnosticsView(
                status,
                blockers,
                manualChecks,
                status.recommendedAction(),
                status.lastTraceId());
    }

    @Transactional
    @CacheEvict(cacheNames = "sri-health", allEntries = true)
    public TaxDocumentSummary validate(String tenantCode, Long taxDocumentId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        SriConfigurationEntity configuration = requireConfiguration(document.getTenantId());
        return toSummary(applyPreparation(document, configuration));
    }

    @Transactional
    @CacheEvict(cacheNames = "sri-health", allEntries = true)
    public TaxDocumentSummary emit(String tenantCode, Long taxDocumentId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        SriConfigurationEntity configuration = requireConfiguration(document.getTenantId());
        if (document.getStatus() == TaxDocumentStatus.AUTHORIZED) {
            return toSummary(document);
        }
        if (document.getStatus() == TaxDocumentStatus.CANCELLED) {
            throw new BadRequestException("No se puede emitir un documento tributario cancelado");
        }

        if (shouldQueryAuthorization(document)) {
            return pollAuthorizationInternal(document, configuration);
        }

        if (document.getStatus() != TaxDocumentStatus.READY_TO_SEND) {
            document = applyPreparation(document, configuration);
        }
        if (document.getStatus() != TaxDocumentStatus.READY_TO_SEND) {
            return toSummary(document);
        }

        return submitReception(document, configuration);
    }

    @Transactional
    @CacheEvict(cacheNames = "sri-health", allEntries = true)
    public TaxDocumentSummary retry(String tenantCode, Long taxDocumentId, String mode) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        SriConfigurationEntity configuration = requireConfiguration(document.getTenantId());
        RetryMode retryMode = parseRetryMode(mode);

        if (document.getStatus() == TaxDocumentStatus.AUTHORIZED) {
            return toSummary(document);
        }
        if (document.getStatus() == TaxDocumentStatus.CANCELLED) {
            throw new BadRequestException("No se puede reenviar un documento tributario cancelado");
        }

        if (retryMode == RetryMode.AUTHORIZATION) {
            return pollAuthorizationInternal(document, configuration);
        }

        if (retryMode == RetryMode.AUTO && shouldProbeAuthorizationBeforeReceptionRetry(document)) {
            TaxDocumentSummary probed = pollAuthorizationInternal(document, configuration);
            TaxDocumentEntity refreshed = taxDocumentRepository.findByTenantIdAndId(document.getTenantId(), document.getId())
                    .orElseThrow(() -> new NotFoundException("Documento tributario no encontrado tras reintento"));
            if (refreshed.getStatus() == TaxDocumentStatus.AUTHORIZED || shouldQueryAuthorization(refreshed)) {
                return probed;
            }
            document = refreshed;
        }

        if (retryMode != RetryMode.RECEPTION && shouldQueryAuthorization(document)) {
            return pollAuthorizationInternal(document, configuration);
        }

        if (document.getStatus() != TaxDocumentStatus.READY_TO_SEND) {
            document = applyPreparation(document, configuration);
        }
        if (document.getStatus() != TaxDocumentStatus.READY_TO_SEND) {
            return toSummary(document);
        }

        return submitReception(document, configuration);
    }

    private TaxDocumentSummary submitReception(TaxDocumentEntity document, SriConfigurationEntity configuration) {
        String signedXml = sriXmlSignatureService.sign(document.getXmlPayload(), buildSigningCredentials(configuration));
        document.setSignedXmlPayload(signedXml);

        SriProperties.Endpoint endpoints = resolveEndpoints(configuration.getEnvironmentCode());
        SriSoapClient.ReceiptResponse reception = sriSoapClient.submitReceipt(endpoints.getReceptionUrl(), signedXml);
        saveTransmission(document, TaxDocumentTransmissionEntity.TransmissionPhase.RECEPTION, reception.endpointUrl(),
                reception.requestPayload(), reception.responsePayload(), reception.providerStatus(),
                reception.providerMessage(), reception.httpStatus(),
                reception.success() && !reception.transportError(), reception.transportError());

        document.setReceptionStatus(reception.providerStatus());
        document.setLastProviderMessage(reception.providerMessage());
        document.setLastStatusAt(LocalDateTime.now());

        if (reception.transportError()) {
            taxDocumentRepository.save(document);
            throw new ServiceUnavailableException("No se pudo conectar con el WS de recepcion del SRI: " + reception.providerMessage());
        }

        if ("RECIBIDA".equalsIgnoreCase(reception.providerStatus())) {
            document.setStatus(TaxDocumentStatus.SENT);
            document.setSentAt(LocalDateTime.now());
            taxDocumentRepository.save(document);
            if (sriProperties.isAutoQueryAfterReception()) {
                applyAuthorizationDelay();
                return pollAuthorizationInternal(document, configuration);
            }
            return toSummary(taxDocumentRepository.save(document));
        }

        document.setStatus(TaxDocumentStatus.REJECTED);
        document.setValidationErrors(reception.providerMessage());
        return toSummary(taxDocumentRepository.save(document));
    }

    @Transactional
    @CacheEvict(cacheNames = "sri-health", allEntries = true)
    public TaxDocumentSummary pollAuthorization(String tenantCode, Long taxDocumentId) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        SriConfigurationEntity configuration = requireConfiguration(document.getTenantId());
        return pollAuthorizationInternal(document, configuration);
    }

    @Transactional
    @CacheEvict(cacheNames = "sri-health", allEntries = true)
    public TaxDocumentSummary markAuthorized(String tenantCode, Long taxDocumentId, String authorizationCode) {
        TaxDocumentEntity document = requireDocument(tenantCode, taxDocumentId);
        document.setAuthorizationCode(authorizationCode);
        document.setAuthorizedAt(LocalDateTime.now());
        document.setStatus(TaxDocumentStatus.AUTHORIZED);
        document.setAuthorizationStatus("MANUAL");
        document.setLastProviderMessage("Autorizacion registrada manualmente");
        document.setLastStatusAt(LocalDateTime.now());
        TaxDocumentEntity saved = taxDocumentRepository.save(document);
        publishAuthorized(saved);
        return toSummary(saved);
    }

    private TaxDocumentSummary pollAuthorizationInternal(TaxDocumentEntity document, SriConfigurationEntity configuration) {
        if (document.getAccessKey() == null || document.getAccessKey().isBlank()) {
            throw new BadRequestException("El documento debe validarse antes de consultar autorizacion");
        }

        SriProperties.Endpoint endpoints = resolveEndpoints(configuration.getEnvironmentCode());
        SriSoapClient.AuthorizationResponse authorization = sriSoapClient.queryAuthorization(
                endpoints.getAuthorizationUrl(), document.getAccessKey());

        saveTransmission(document, TaxDocumentTransmissionEntity.TransmissionPhase.AUTHORIZATION, authorization.endpointUrl(),
                authorization.requestPayload(), authorization.responsePayload(), authorization.providerStatus(),
                authorization.providerMessage(), authorization.httpStatus(),
                authorization.success() && !authorization.transportError(), authorization.transportError());

        document.setAuthorizationStatus(authorization.providerStatus());
        document.setLastProviderMessage(authorization.providerMessage());
        document.setLastStatusAt(LocalDateTime.now());

        if (authorization.transportError()) {
            taxDocumentRepository.save(document);
            throw new ServiceUnavailableException("No se pudo consultar la autorizacion del SRI: " + authorization.providerMessage());
        }

        if ("AUTORIZADO".equalsIgnoreCase(authorization.providerStatus())) {
            document.setStatus(TaxDocumentStatus.AUTHORIZED);
            document.setAuthorizationCode(authorization.authorizationNumber());
            document.setAuthorizedAt(authorization.authorizedAt() != null ? authorization.authorizedAt() : LocalDateTime.now());
            if (authorization.authorizedXml() != null && !authorization.authorizedXml().isBlank()) {
                document.setSignedXmlPayload(authorization.authorizedXml());
            }
            document.setValidationErrors(null);
        } else if ("NO AUTORIZADO".equalsIgnoreCase(authorization.providerStatus())
                || "RECHAZADO".equalsIgnoreCase(authorization.providerStatus())) {
            document.setStatus(TaxDocumentStatus.REJECTED);
            document.setValidationErrors(authorization.providerMessage());
        } else {
            document.setStatus(TaxDocumentStatus.SENT);
        }

        TaxDocumentEntity saved = taxDocumentRepository.save(document);
        publishAuthorized(saved);
        return toSummary(saved);
    }

    private void publishAuthorized(TaxDocumentEntity document) {
        if (document != null && document.getStatus() == TaxDocumentStatus.AUTHORIZED) {
            eventPublisher.publishEvent(new SriDocumentAuthorizedEvent(
                    document.getTenantId(),
                    document.getId(),
                    document.getAccessKey(),
                    document.getAuthorizationCode()));
        }
    }

    private TaxDocumentEntity applyPreparation(TaxDocumentEntity document, SriConfigurationEntity configuration) {
        List<TaxDocumentTaxLineEntity> taxLines = taxDocumentTaxLineRepository.findByTaxDocumentId(document.getId());
        SriDocumentPreparationService.PreparedDocument prepared = sriDocumentPreparationService.prepare(document, configuration, taxLines);
        if (prepared.errors().isEmpty()) {
            document.setEnvironmentCode(normalizeEnvironmentCode(configuration.getEnvironmentCode()));
            document.setAccessKey(prepared.accessKey());
            document.setXmlPayload(prepared.xmlPayload());
            document.setValidationErrors(null);
            document.setStatus(TaxDocumentStatus.READY_TO_SEND);
        } else {
            document.setStatus(TaxDocumentStatus.REJECTED);
            document.setValidationErrors(String.join(" | ", prepared.errors()));
        }
        document.setLastStatusAt(LocalDateTime.now());
        return taxDocumentRepository.save(document);
    }

    private SriXmlSignatureService.SigningCredentials buildSigningCredentials(SriConfigurationEntity configuration) {
        String mode = normalizeSignatureMode(configuration.getSignatureMode());
        if (!"PKCS12".equals(mode)) {
            return new SriXmlSignatureService.SigningCredentials(mode, null, null, configuration.getP12KeyAlias());
        }
        if (configuration.getP12Content() == null || configuration.getP12Content().length == 0) {
            throw new BadRequestException("El tenant no tiene certificado PKCS12 configurado para firma SRI");
        }
        if (isBlank(configuration.getP12PasswordEncrypted())) {
            throw new BadRequestException("El tenant no tiene clave cifrada para la firma SRI");
        }
        String password = sriCredentialCryptoService.decrypt(
                configuration.getP12PasswordEncrypted(),
                configuration.getSriEncryptionSalt()
        );
        return new SriXmlSignatureService.SigningCredentials(
                mode,
                configuration.getP12Content(),
                password,
                configuration.getP12KeyAlias()
        );
    }

    private void applySignatureConfiguration(SriConfigurationEntity entity, UpsertSriConfigurationCommand command) {
        String requestedMode = command.signatureMode() != null
                ? normalizeSignatureMode(command.signatureMode())
                : normalizeSignatureMode(entity.getSignatureMode());

        if (Boolean.TRUE.equals(command.clearSignature())) {
            entity.setP12Content(null);
            entity.setP12PasswordEncrypted(null);
            entity.setSriEncryptionSalt(null);
            entity.setP12KeyAlias(null);
            entity.setSignatureUpdatedAt(LocalDateTime.now());
            if (command.signatureMode() == null) {
                requestedMode = "NONE";
            }
        }

        if (command.p12KeyAlias() != null) {
            entity.setP12KeyAlias(trimToNull(command.p12KeyAlias()));
        }

        boolean hasCertificateUpload = !isBlank(command.p12Base64());
        boolean hasPasswordUpdate = !isBlank(command.p12Password());
        if (hasCertificateUpload) {
            if (!hasPasswordUpdate) {
                throw new BadRequestException("La carga de un certificado SRI requiere la clave del archivo .p12");
            }
            byte[] p12Content = decodeP12Base64(command.p12Base64());
            sriXmlSignatureService.validatePkcs12(p12Content, command.p12Password(), entity.getP12KeyAlias());
            String salt = sriCredentialCryptoService.generateSalt();
            entity.setP12Content(p12Content);
            entity.setSriEncryptionSalt(salt);
            entity.setP12PasswordEncrypted(sriCredentialCryptoService.encrypt(command.p12Password(), salt));
            entity.setSignatureMode("PKCS12");
            entity.setSignatureUpdatedAt(LocalDateTime.now());
            requestedMode = "PKCS12";
        } else if (hasPasswordUpdate) {
            if (entity.getP12Content() == null || entity.getP12Content().length == 0) {
                throw new BadRequestException("No puedes actualizar la clave sin un certificado .p12 guardado");
            }
            sriXmlSignatureService.validatePkcs12(entity.getP12Content(), command.p12Password(), entity.getP12KeyAlias());
            String salt = sriCredentialCryptoService.generateSalt();
            entity.setSriEncryptionSalt(salt);
            entity.setP12PasswordEncrypted(sriCredentialCryptoService.encrypt(command.p12Password(), salt));
            entity.setSignatureUpdatedAt(LocalDateTime.now());
        } else if (command.p12KeyAlias() != null
                && "PKCS12".equals(requestedMode)
                && entity.getP12Content() != null
                && !isBlank(entity.getP12PasswordEncrypted())) {
            String password = sriCredentialCryptoService.decrypt(
                    entity.getP12PasswordEncrypted(),
                    entity.getSriEncryptionSalt()
            );
            sriXmlSignatureService.validatePkcs12(entity.getP12Content(), password, entity.getP12KeyAlias());
            entity.setSignatureUpdatedAt(LocalDateTime.now());
        }

        entity.setSignatureMode(requestedMode);
        if ("PKCS12".equals(requestedMode)) {
            if (entity.getP12Content() == null || entity.getP12Content().length == 0
                    || isBlank(entity.getP12PasswordEncrypted())
                    || isBlank(entity.getSriEncryptionSalt())) {
                throw new BadRequestException("El modo PKCS12 requiere certificado .p12 y clave cifrada por tenant");
            }
        }
    }

    private byte[] decodeP12Base64(String p12Base64) {
        String value = p12Base64.trim();
        int commaIndex = value.indexOf(',');
        if (value.startsWith("data:") && commaIndex >= 0) {
            value = value.substring(commaIndex + 1);
        }
        try {
            byte[] decoded = Base64.getMimeDecoder().decode(value);
            if (decoded.length == 0) {
                throw new BadRequestException("El certificado SRI esta vacio");
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("El certificado .p12 debe enviarse en Base64 valido");
        }
    }

    private void saveTransmission(TaxDocumentEntity document,
                                  TaxDocumentTransmissionEntity.TransmissionPhase phase,
                                  String endpointUrl,
                                  String requestPayload,
                                  String responsePayload,
                                  String providerStatus,
                                  String providerMessage,
                                  Integer httpStatus,
                                  boolean success,
                                  boolean transportError) {
        TaxDocumentTransmissionEntity transmission = new TaxDocumentTransmissionEntity();
        transmission.setTaxDocument(document);
        transmission.setPhase(phase);
        transmission.setEndpointUrl(endpointUrl);
        transmission.setRequestPayload(requestPayload);
        transmission.setResponsePayload(responsePayload);
        transmission.setProviderStatus(providerStatus);
        transmission.setProviderMessage(providerMessage);
        transmission.setHttpStatus(httpStatus);
        transmission.setAttemptNumber((int) taxDocumentTransmissionRepository.countByTaxDocumentIdAndPhase(document.getId(), phase) + 1);
        transmission.setTraceId(MDC.get(RequestTraceFilter.TRACE_ID_KEY));
        transmission.setTransportError(transportError);
        transmission.setSuccess(success);
        transmission.setCompletedAt(LocalDateTime.now());
        taxDocumentTransmissionRepository.save(transmission);
    }

    private TaxDocumentEntity requireDocument(String tenantCode, Long taxDocumentId) {
        Long tenantId = tenantContextResolver.resolve(tenantCode).getId();
        return taxDocumentRepository.findByTenantIdAndId(tenantId, taxDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento tributario no encontrado"));
    }

    private SriConfigurationEntity requireConfiguration(Long tenantId) {
        return sriConfigurationRepository.findByTenantId(tenantId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new BadRequestException("Debes configurar el emisor SRI del tenant antes de emitir documentos"));
    }

    private SriProperties.Endpoint resolveEndpoints(String environmentCode) {
        return "2".equals(normalizeEnvironmentCode(environmentCode))
                ? sriProperties.getProduction()
                : sriProperties.getTest();
    }

    private boolean shouldQueryAuthorization(TaxDocumentEntity document) {
        return document.getStatus() == TaxDocumentStatus.SENT
                || "RECIBIDA".equalsIgnoreCase(document.getReceptionStatus())
                || "PROCESSING".equalsIgnoreCase(document.getAuthorizationStatus());
    }

    private boolean shouldProbeAuthorizationBeforeReceptionRetry(TaxDocumentEntity document) {
        if (document.getAccessKey() == null || document.getAccessKey().isBlank()) {
            return false;
        }
        return taxDocumentTransmissionRepository
                .findTopByTaxDocumentIdAndPhaseOrderByAttemptedAtDesc(document.getId(),
                        TaxDocumentTransmissionEntity.TransmissionPhase.RECEPTION)
                .map(latest -> Boolean.TRUE.equals(latest.getTransportError()) || "HTTP_ERROR".equalsIgnoreCase(latest.getProviderStatus()))
                .orElse(false);
    }

    private RetryMode parseRetryMode(String mode) {
        try {
            return RetryMode.valueOf((mode == null || mode.isBlank() ? "AUTO" : mode.trim()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Modo de reintento SRI no soportado. Usa AUTO, RECEPTION o AUTHORIZATION");
        }
    }

    private void validateConfiguration(UpsertSriConfigurationCommand command) {
        if (command == null) {
            throw new BadRequestException("La configuracion SRI es obligatoria");
        }
        if (digitsOnly(command.issuerRuc()).length() != 13) {
            throw new BadRequestException("El RUC emisor debe tener 13 digitos");
        }
        if (isBlank(command.issuerLegalName()) || isBlank(command.matrixAddress()) || isBlank(command.establishmentAddress())) {
            throw new BadRequestException("La configuracion SRI requiere razon social y direcciones completas");
        }
    }

    private void applyAuthorizationDelay() {
        if (sriProperties.getAuthorizationPollDelayMs() <= 0) {
            return;
        }
        try {
            Thread.sleep(sriProperties.getAuthorizationPollDelayMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.debug("Authorization poll delay interrupted");
        }
    }

    private SriConfigurationView toConfigurationView(SriConfigurationEntity entity) {
        return new SriConfigurationView(entity.getId(), entity.getEnvironmentCode(), entity.getEmissionCode(),
                entity.getIssuerRuc(), entity.getIssuerLegalName(), entity.getIssuerTradeName(),
                entity.getMatrixAddress(), entity.getEstablishmentAddress(), entity.getSpecialTaxpayer(),
                entity.getObligatedAccounting(), entity.getRimpeContributor(), entity.getRetentionAgent(),
                entity.getRetentionAgentResolution(), entity.getActive(), entity.getSignatureMode(),
                entity.getP12KeyAlias(), entity.getP12Content() != null && entity.getP12Content().length > 0,
                entity.getSignatureUpdatedAt(), entity.getUpdatedAt());
    }

    private TaxDocumentSummary toSummary(TaxDocumentEntity entity) {
        return new TaxDocumentSummary(entity.getId(), entity.getDocumentType().getDocumentCode(), entity.getStatus().name(),
                entity.getIssueDate(), entity.getBuyerIdentification(), entity.getBuyerName(), entity.getSequentialNumber(),
                entity.getAccessKey(), entity.getAuthorizationCode(), entity.getSubtotalAmount(), entity.getTaxAmount(),
                entity.getTotalAmount(), entity.getValidationErrors(), entity.getReceptionStatus(),
                entity.getAuthorizationStatus(), entity.getLastProviderMessage(), entity.getRidePdfUrl(),
                entity.getRideEmailStatus(), entity.getRideEmailError(), entity.getRideEmailAttempts(),
                entity.getRideEmailSentAt(), entity.getSentAt(),
                entity.getAuthorizedAt(), entity.getLastStatusAt(), entity.getCreatedAt());
    }

    private TaxDocumentTransmissionSummary toTransmissionSummary(TaxDocumentTransmissionEntity item) {
        return new TaxDocumentTransmissionSummary(
                item.getId(),
                item.getPhase().name(),
                item.getEndpointUrl(),
                item.getProviderStatus(),
                item.getProviderMessage(),
                item.getHttpStatus(),
                Boolean.TRUE.equals(item.getSuccess()),
                Boolean.TRUE.equals(item.getTransportError()),
                item.getAttemptNumber(),
                item.getTraceId(),
                item.getAttemptedAt(),
                item.getCompletedAt());
    }

    private TaxDocumentTransmissionDetailView toTransmissionDetail(TaxDocumentTransmissionEntity item) {
        return new TaxDocumentTransmissionDetailView(
                item.getId(),
                item.getPhase().name(),
                item.getEndpointUrl(),
                item.getProviderStatus(),
                item.getProviderMessage(),
                item.getHttpStatus(),
                Boolean.TRUE.equals(item.getSuccess()),
                Boolean.TRUE.equals(item.getTransportError()),
                item.getAttemptNumber(),
                item.getTraceId(),
                item.getRequestPayload(),
                item.getResponsePayload(),
                item.getAttemptedAt(),
                item.getCompletedAt());
    }

    private TaxDocumentStatusView buildStatusView(TaxDocumentEntity document) {
        long receptionAttempts = taxDocumentTransmissionRepository.countByTaxDocumentIdAndPhase(
                document.getId(),
                TaxDocumentTransmissionEntity.TransmissionPhase.RECEPTION);
        long authorizationAttempts = taxDocumentTransmissionRepository.countByTaxDocumentIdAndPhase(
                document.getId(),
                TaxDocumentTransmissionEntity.TransmissionPhase.AUTHORIZATION);
        TaxDocumentTransmissionEntity latestTransmission = taxDocumentTransmissionRepository
                .findTopByTaxDocumentIdOrderByAttemptedAtDesc(document.getId())
                .orElse(null);

        return new TaxDocumentStatusView(
                document.getId(),
                document.getDocumentType().getDocumentCode(),
                document.getStatus().name(),
                document.getEnvironmentCode(),
                "2".equals(document.getEnvironmentCode()) ? "AMBIENTE_2" : "AMBIENTE_1",
                document.getAccessKey(),
                document.getReceptionStatus(),
                document.getAuthorizationStatus(),
                document.getLastProviderMessage(),
                receptionAttempts,
                authorizationAttempts,
                recommendedAction(document, latestTransmission),
                latestTransmission != null ? latestTransmission.getPhase().name() : null,
                latestTransmission != null ? latestTransmission.getHttpStatus() : null,
                latestTransmission != null ? latestTransmission.getTraceId() : null,
                latestTransmission != null && Boolean.TRUE.equals(latestTransmission.getTransportError()),
                latestTransmission != null ? latestTransmission.getAttemptedAt() : null,
                document.getSentAt(),
                document.getAuthorizedAt(),
                document.getLastStatusAt());
    }

    private String recommendedAction(TaxDocumentEntity document, TaxDocumentTransmissionEntity latestTransmission) {
        if (document.getStatus() == TaxDocumentStatus.AUTHORIZED) {
            return "READY_FOR_PILOT_EVIDENCE";
        }
        if (document.getStatus() == TaxDocumentStatus.CANCELLED) {
            return "MANUAL_REVIEW";
        }
        if (document.getStatus() == TaxDocumentStatus.SENT || "RECIBIDA".equalsIgnoreCase(document.getReceptionStatus())
                || "PROCESSING".equalsIgnoreCase(document.getAuthorizationStatus())) {
            return "POLL_AUTHORIZATION";
        }
        if (document.getStatus() == TaxDocumentStatus.READY_TO_SEND) {
            return "EMIT_DOCUMENT";
        }
        if (document.getStatus() == TaxDocumentStatus.REJECTED) {
            return "FIX_DATA_AND_REVALIDATE";
        }
        if (latestTransmission != null && Boolean.TRUE.equals(latestTransmission.getTransportError())) {
            return latestTransmission.getPhase() == TaxDocumentTransmissionEntity.TransmissionPhase.AUTHORIZATION
                    ? "RETRY_AUTHORIZATION"
                    : "RETRY_RECEPTION";
        }
        return "VALIDATE_DOCUMENT";
    }

    private String normalizeEnvironmentCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("El ambiente SRI es obligatorio");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "TEST", "TESTING" -> "1";
            case "2", "PROD", "PRODUCTION" -> "2";
            default -> throw new BadRequestException("Ambiente SRI invalido: " + value);
        };
    }

    private String normalizeEmissionCode(String value) {
        String digits = digitsOnly(value);
        return digits.isBlank() ? "1" : digits.substring(0, 1);
    }

    private String normalizeSignatureMode(String value) {
        String normalized = value == null || value.isBlank() ? "NONE" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NONE", "PRESIGNED", "PKCS12" -> normalized;
            default -> throw new BadRequestException("Modo de firma SRI invalido: " + value);
        };
    }

    private String normalizeObligatedAccounting(String value) {
        return "SI".equalsIgnoreCase(value != null ? value.trim() : "") ? "SI" : "NO";
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record TaxDocumentSummary(
            Long id,
            String documentCode,
            String status,
            LocalDate issueDate,
            String buyerIdentification,
            String buyerName,
            String sequentialNumber,
            String accessKey,
            String authorizationCode,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            String validationErrors,
            String receptionStatus,
            String authorizationStatus,
            String providerMessage,
            String ridePdfUrl,
            String rideEmailStatus,
            String rideEmailError,
            Integer rideEmailAttempts,
            LocalDateTime rideEmailSentAt,
            LocalDateTime sentAt,
            LocalDateTime authorizedAt,
            LocalDateTime lastStatusAt,
            LocalDateTime createdAt) {
    }

    public record UpsertSriConfigurationCommand(
            String environmentCode,
            String emissionCode,
            String issuerRuc,
            String issuerLegalName,
            String issuerTradeName,
            String matrixAddress,
            String establishmentAddress,
            String specialTaxpayer,
            String obligatedAccounting,
            Boolean rimpeContributor,
            Boolean retentionAgent,
            String retentionAgentResolution,
            String signatureMode,
            String p12Base64,
            String p12Password,
            String p12KeyAlias,
            Boolean clearSignature,
            Boolean active) {
    }

    public record SriConfigurationView(
            Long id,
            String environmentCode,
            String emissionCode,
            String issuerRuc,
            String issuerLegalName,
            String issuerTradeName,
            String matrixAddress,
            String establishmentAddress,
            String specialTaxpayer,
            String obligatedAccounting,
            Boolean rimpeContributor,
            Boolean retentionAgent,
            String retentionAgentResolution,
            Boolean active,
            String signatureMode,
            String p12KeyAlias,
            Boolean hasP12Certificate,
            LocalDateTime signatureUpdatedAt,
            LocalDateTime updatedAt) {
    }

    public record TaxDocumentTransmissionSummary(
            Long id,
            String phase,
            String endpointUrl,
            String providerStatus,
            String providerMessage,
            Integer httpStatus,
            boolean success,
            boolean transportError,
            Integer attemptNumber,
            String traceId,
            LocalDateTime attemptedAt,
            LocalDateTime completedAt) {
    }

    public record TaxDocumentTransmissionDetailView(
            Long id,
            String phase,
            String endpointUrl,
            String providerStatus,
            String providerMessage,
            Integer httpStatus,
            boolean success,
            boolean transportError,
            Integer attemptNumber,
            String traceId,
            String requestPayload,
            String responsePayload,
            LocalDateTime attemptedAt,
            LocalDateTime completedAt) {
    }

    public record TaxDocumentStatusView(
            Long id,
            String documentCode,
            String status,
            String environmentCode,
            String environmentLabel,
            String accessKey,
            String receptionStatus,
            String authorizationStatus,
            String providerMessage,
            long receptionAttempts,
            long authorizationAttempts,
            String recommendedAction,
            String lastPhase,
            Integer lastHttpStatus,
            String lastTraceId,
            boolean lastTransportError,
            LocalDateTime lastAttemptedAt,
            LocalDateTime sentAt,
            LocalDateTime authorizedAt,
            LocalDateTime lastStatusAt) {
    }

    public record TaxDocumentXmlView(
            Long id,
            String accessKey,
            String status,
            String xml) {
    }

    public record TaxDocumentDiagnosticsView(
            TaxDocumentStatusView status,
            List<String> blockers,
            List<String> manualChecks,
            String nextStep,
            String traceId) {
    }

    private enum RetryMode {
        AUTO,
        RECEPTION,
        AUTHORIZATION
    }
}
