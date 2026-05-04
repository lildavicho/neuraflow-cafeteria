package com.ucacue.bar.erp.accounting.infrastructure.sri;

import com.ucacue.bar.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class SriKeyService {

    public String generarClave(String fecha,
                               String tipoComprobante,
                               String ruc,
                               String ambiente,
                               String serie,
                               String secuencial,
                               String codigoNumerico,
                               String tipoEmision) {
        requireDigits("fecha", fecha, 8);
        requireDigits("tipoComprobante", tipoComprobante, 2);
        requireDigits("ruc", ruc, 13);
        requireDigits("ambiente", ambiente, 1);
        requireDigits("serie", serie, 6);
        requireDigits("secuencial", secuencial, 9);
        requireDigits("codigoNumerico", codigoNumerico, 8);
        requireDigits("tipoEmision", tipoEmision, 1);

        String base = fecha
                + tipoComprobante
                + ruc
                + ambiente
                + serie
                + secuencial
                + codigoNumerico
                + tipoEmision;
        if (base.length() != 48) {
            throw new BadRequestException("La base de la clave de acceso SRI debe tener 48 digitos");
        }
        String accessKey = base + calculateModulo11Digit(base);
        if (accessKey.length() != 49) {
            throw new BadRequestException("La clave de acceso SRI debe tener exactamente 49 digitos");
        }
        return accessKey;
    }

    public int calculateModulo11Digit(String value) {
        requireDigits("baseModulo11", value, 48);
        int factor = 2;
        int sum = 0;
        for (int index = value.length() - 1; index >= 0; index--) {
            sum += Character.digit(value.charAt(index), 10) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }

        int digit = 11 - (sum % 11);
        if (digit == 11) {
            return 0;
        }
        if (digit == 10) {
            return 1;
        }
        return digit;
    }

    private void requireDigits(String field, String value, int length) {
        if (value == null || value.length() != length || !value.chars().allMatch(Character::isDigit)) {
            throw new BadRequestException("El campo " + field + " debe tener " + length + " digitos");
        }
    }
}
