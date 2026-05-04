package com.ucacue.bar.erp.accounting.infrastructure.sri;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class SriTextSanitizer {

    public String clean(String value) {
        if (value == null) {
            return "";
        }
        String withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutMarks
                .replace('&', 'Y')
                .replace('<', ' ')
                .replace('>', ' ')
                .replace('"', ' ')
                .replace('\'', ' ')
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String cleanOrNull(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    public String cleanAndLimit(String value, int maxLength) {
        String cleaned = clean(value);
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength).trim();
    }
}
