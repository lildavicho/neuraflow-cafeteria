package com.ucacue.bar.erp.accounting.infrastructure.sri;

import com.ucacue.bar.erp.accounting.infrastructure.config.SriProperties;
import com.ucacue.bar.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SriCredentialCryptoService {

    private static final String PREFIX = "v1:";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 120_000;

    private final SriProperties sriProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String encrypt(String plainText, String saltBase64) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(saltBase64), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo cifrar la clave de la firma SRI");
        }
    }

    public String decrypt(String encryptedText, String saltBase64) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }
        if (!encryptedText.startsWith(PREFIX)) {
            throw new BadRequestException("La clave de la firma SRI no esta cifrada con el formato soportado");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                throw new BadRequestException("La clave cifrada de la firma SRI es invalida");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(saltBase64), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo descifrar la clave de la firma SRI");
        }
    }

    private SecretKeySpec key(String saltBase64) throws Exception {
        String configuredKey = sriProperties.getSignature().getPasswordMasterKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new BadRequestException("Configura SRI_SIGNATURE_PASSWORD_MASTER_KEY para cifrar claves de firmas SRI");
        }
        if (saltBase64 == null || saltBase64.isBlank()) {
            throw new BadRequestException("La configuracion SRI del tenant no tiene salt de cifrado");
        }
        byte[] salt = decodeSalt(saltBase64);
        PBEKeySpec spec = new PBEKeySpec(configuredKey.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
            return new SecretKeySpec(key, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private byte[] decodeSalt(String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            if (salt.length < SALT_LENGTH) {
                throw new BadRequestException("El salt de cifrado SRI del tenant es invalido");
            }
            return salt;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("El salt de cifrado SRI del tenant no es Base64 valido");
        }
    }
}
