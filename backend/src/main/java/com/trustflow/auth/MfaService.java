package com.trustflow.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;

/** Simulated TOTP (RFC 6238 style: HMAC-SHA1 over a time counter, 6 digits). No SMS cost, realistic flow. */
@Service
public class MfaService {

    private final long stepSeconds;

    public MfaService(@Value("${trustflow.mfa.step-seconds:30}") long stepSeconds) {
        this.stepSeconds = stepSeconds;
    }

    public String currentCode(String secret) {
        return code(secret, counter(Instant.now()));
    }

    public long secondsRemaining() {
        return stepSeconds - (Instant.now().getEpochSecond() % stepSeconds);
    }

    /** Accepts the current and the previous time step to tolerate small clock drift. */
    public boolean verify(String secret, String submitted) {
        if (submitted == null || !submitted.matches("\\d{6}")) return false;
        long c = counter(Instant.now());
        return constantTimeEquals(code(secret, c), submitted) || constantTimeEquals(code(secret, c - 1), submitted);
    }

    long counter(Instant at) {
        return at.getEpochSecond() / stepSeconds;
    }

    public static String code(String secret, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int off = h[h.length - 1] & 0x0f;
            int bin = ((h[off] & 0x7f) << 24) | ((h[off + 1] & 0xff) << 16) | ((h[off + 2] & 0xff) << 8) | (h[off + 3] & 0xff);
            return String.format("%06d", bin % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
