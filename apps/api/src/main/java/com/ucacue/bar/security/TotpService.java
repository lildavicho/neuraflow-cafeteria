package com.ucacue.bar.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class TotpService {

    private static final int SECRET_SIZE = 20;
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int WINDOW = 1;
    private static final String ALGORITHM = "HmacSHA1";
    private static final SecureRandom RNG = new SecureRandom();
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        RNG.nextBytes(buffer);
        return base32Encode(buffer);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        try {
            int codeInt = Integer.parseInt(code);
            long currentInterval = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

            for (int i = -WINDOW; i <= WINDOW; i++) {
                int expected = generateCode(secret, currentInterval + i);
                if (expected == codeInt) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    public String buildOtpAuthUri(String secret, String email, String issuer) {
        return "otpauth://totp/" + issuer + ":" + email
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1"
                + "&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    private int generateCode(String base32Secret, long timeInterval) {
        byte[] key = base32Decode(base32Secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeInterval).array();

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % (int) Math.pow(10, CODE_DIGITS);
        } catch (Exception e) {
            throw new IllegalStateException("Error generating TOTP code", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    private byte[] base32Decode(String base32) {
        String upper = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int[] lookup = new int[128];
        for (int i = 0; i < BASE32_CHARS.length(); i++) {
            lookup[BASE32_CHARS.charAt(i)] = i;
        }

        byte[] result = new byte[upper.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : upper.toCharArray()) {
            buffer = (buffer << 5) | lookup[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
