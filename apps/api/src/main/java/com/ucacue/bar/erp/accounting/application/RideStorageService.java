package com.ucacue.bar.erp.accounting.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.erp.accounting.infrastructure.persistence.entity.TaxDocumentEntity;
import com.ucacue.bar.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Service
public class RideStorageService {

    private final SriProperties sriProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public RideStorageService(SriProperties sriProperties, ObjectMapper objectMapper) {
        this.sriProperties = sriProperties;
        this.objectMapper = objectMapper;
    }

    public StoredRideFile storeRidePdf(TaxDocumentEntity document, byte[] content) {
        if (document.getIssueDate() == null) {
            throw new BadRequestException("El documento no tiene fecha de emision para almacenar el RIDE");
        }
        LocalDate issueDate = document.getIssueDate();
        String fileName = buildRideFileName(document);
        Path relative = Path.of(
                String.valueOf(document.getTenantId()),
                String.valueOf(issueDate.getYear()),
                "%02d".formatted(issueDate.getMonthValue()),
                fileName);
        storeBytes(relative, content, "application/pdf");
        return new StoredRideFile(relative.toString().replace('\\', '/'), publicUrl(relative));
    }

    public StoredRideFile storeSignedXml(TaxDocumentEntity document, String xml) {
        if (document.getIssueDate() == null) {
            throw new BadRequestException("El documento no tiene fecha de emision para almacenar el XML");
        }
        if (xml == null || xml.isBlank()) {
            throw new BadRequestException("El documento no tiene XML firmado para almacenar");
        }
        LocalDate issueDate = document.getIssueDate();
        Path relative = Path.of(
                String.valueOf(document.getTenantId()),
                String.valueOf(issueDate.getYear()),
                "%02d".formatted(issueDate.getMonthValue()),
                buildXmlFileName(document));
        storeBytes(relative, xml.getBytes(StandardCharsets.UTF_8), "application/xml; charset=UTF-8");
        return new StoredRideFile(relative.toString().replace('\\', '/'), publicUrl(relative));
    }

    public StoredRideFile storeLogo(Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El logo es obligatorio");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new BadRequestException("El logo debe ser una imagen");
        }
        String extension = resolveImageExtension(contentType, file.getOriginalFilename());
        Path relative = Path.of("assets", String.valueOf(tenantId), "logo" + extension);
        try {
            storeBytes(relative, file.getBytes(), contentType);
            return new StoredRideFile(relative.toString().replace('\\', '/'), publicUrl(relative));
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo almacenar el logo RIDE: " + ex.getMessage());
        }
    }

    private Path basePath() {
        return Path.of(sriProperties.getRide().getStoragePath()).toAbsolutePath().normalize();
    }

    private void storeBytes(Path relative, byte[] content, String contentType) {
        if (useSupabaseStorage()) {
            uploadToSupabase(relative, content, contentType);
            return;
        }
        Path absolute = basePath().resolve(relative).normalize();
        writeFile(absolute, content);
    }

    private void writeFile(Path absolute, byte[] content) {
        try {
            if (!absolute.startsWith(basePath())) {
                throw new BadRequestException("Ruta de almacenamiento RIDE invalida");
            }
            Files.createDirectories(absolute.getParent());
            Files.write(absolute, content);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo almacenar archivo RIDE: " + ex.getMessage());
        }
    }

    private void uploadToSupabase(Path relative, byte[] content, String contentType) {
        try {
            String objectPath = relative.toString().replace('\\', '/');
            String baseUrl = cleanSupabaseUrl();
            String bucket = sriProperties.getRide().getSupabaseBucket();
            String key = sriProperties.getRide().getSupabaseServiceRoleKey();
            URI uri = URI.create(baseUrl + "/storage/v1/object/" + bucket + "/" + encodePath(objectPath));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + key)
                    .header("apikey", key)
                    .header("Content-Type", contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
                    .header("x-upsert", "true")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("Supabase Storage rechazo el archivo RIDE: HTTP " + response.statusCode());
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo subir archivo RIDE a Supabase: " + ex.getMessage());
        }
    }

    private String publicUrl(Path relative) {
        String baseUrl = sriProperties.getRide().getPublicBaseUrl();
        String objectPath = relative.toString().replace('\\', '/');
        if (baseUrl != null && !baseUrl.isBlank()) {
            String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return cleanBase + "/" + objectPath;
        }
        if (useSupabaseStorage()) {
            return signSupabaseUrl(objectPath, sriProperties.getRide().getSignedUrlTtlSeconds());
        }
        return null;
    }

    public String signedUrl(String objectPath) {
        return signedUrl(objectPath, sriProperties.getRide().getSignedUrlTtlSeconds());
    }

    public java.time.Instant signedUrlExpiresAt() {
        return java.time.Instant.now().plusSeconds(sriProperties.getRide().getSignedUrlTtlSeconds());
    }

    public String signedUrl(String objectPath, long ttlSeconds) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }
        String normalized = objectPath.replace('\\', '/');
        if (!useSupabaseStorage()) {
            String baseUrl = sriProperties.getRide().getPublicBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                return null;
            }
            String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return cleanBase + "/" + normalized;
        }
        String publicBase = sriProperties.getRide().getPublicBaseUrl();
        if (publicBase != null && !publicBase.isBlank()) {
            String cleanBase = publicBase.endsWith("/") ? publicBase.substring(0, publicBase.length() - 1) : publicBase;
            return cleanBase + "/" + normalized;
        }
        return signSupabaseUrl(normalized, ttlSeconds);
    }

    private String signSupabaseUrl(String objectPath, long ttlSeconds) {
        try {
            String baseUrl = cleanSupabaseUrl();
            String bucket = sriProperties.getRide().getSupabaseBucket();
            String key = sriProperties.getRide().getSupabaseServiceRoleKey();
            URI uri = URI.create(baseUrl + "/storage/v1/object/sign/" + bucket + "/" + encodePath(objectPath));
            String body = objectMapper.writeValueAsString(Map.of("expiresIn", Math.max(60L, ttlSeconds)));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + key)
                    .header("apikey", key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            Map<?, ?> parsed = objectMapper.readValue(response.body(), Map.class);
            Object signed = parsed.get("signedURL");
            if (signed == null) {
                signed = parsed.get("signedUrl");
            }
            if (!(signed instanceof String s) || s.isBlank()) {
                return null;
            }
            String relativeSigned = s.startsWith("/") ? s : "/" + s;
            return baseUrl + (relativeSigned.startsWith("/storage/v1") ? relativeSigned : "/storage/v1" + relativeSigned);
        } catch (Exception ex) {
            return null;
        }
    }

    private String encodePath(String objectPath) {
        StringBuilder sb = new StringBuilder(objectPath.length() + 8);
        for (String segment : objectPath.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    private String buildRideFileName(TaxDocumentEntity document) {
        String accessKey = firstNonBlank(document.getAccessKey(), String.valueOf(document.getId()));
        return sanitize(accessKey) + ".pdf";
    }

    private String buildXmlFileName(TaxDocumentEntity document) {
        String accessKey = firstNonBlank(document.getAccessKey(), String.valueOf(document.getId()));
        return sanitize(accessKey) + ".xml";
    }

    private String resolveImageExtension(String contentType, String originalName) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> {
                String name = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp")) {
                    yield name.substring(name.lastIndexOf('.'));
                }
                yield ".img";
            }
        };
    }

    private String sanitize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private boolean useSupabaseStorage() {
        SriProperties.Ride ride = sriProperties.getRide();
        return "SUPABASE".equalsIgnoreCase(ride.getStorageProvider())
                || (!isBlank(ride.getSupabaseUrl()) && !isBlank(ride.getSupabaseServiceRoleKey()));
    }

    private String cleanSupabaseUrl() {
        String url = sriProperties.getRide().getSupabaseUrl();
        if (isBlank(url)) {
            throw new BadRequestException("Configura app.sri.ride.supabase-url para usar Supabase Storage");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record StoredRideFile(
            String path,
            String publicUrl) {
    }
}
