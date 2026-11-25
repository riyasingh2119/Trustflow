package com.trustflow.auth;

import com.trustflow.policy.PolicyAction;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates three kinds of signed JWT (HS256), distinguished by the "tt" claim:
 * access (15 min login token), challenge (5 min, issued with MFA_REQUIRED) and mfa (2 min, single use).
 * Keeping challenges in signed tokens keeps the backend stateless across replicas.
 */
@Service
public class JwtService {

    public static final String ACCESS = "access";
    public static final String CHALLENGE = "challenge";
    public static final String MFA = "mfa";

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(@Value("${trustflow.jwt.secret}") String secret,
                      @Value("${trustflow.jwt.expiry-minutes}") long expiryMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiryMinutes = expiryMinutes;
    }

    public long accessTtlSeconds() {
        return Duration.ofMinutes(expiryMinutes).toSeconds();
    }

    public String accessToken(String username, Collection<String> roles) {
        return base(username, Duration.ofMinutes(expiryMinutes)).claim("tt", ACCESS).claim("roles", roles).compact();
    }

    public String challengeToken(String username, Long resourceId, PolicyAction action) {
        return base(username, Duration.ofMinutes(5)).claim("tt", CHALLENGE)
                .claim("rid", resourceId).claim("act", action.name()).compact();
    }

    public String mfaToken(String username, Long resourceId, PolicyAction action) {
        return base(username, Duration.ofMinutes(2)).claim("tt", MFA)
                .claim("rid", resourceId).claim("act", action.name()).compact();
    }

    /** Throws io.jsonwebtoken.JwtException when the token is invalid, tampered with or expired. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** JSON numbers come back as Integer or Long depending on size, so read them through Number. */
    public static long longClaim(Claims c, String name) {
        Object v = c.get(name);
        return v instanceof Number n ? n.longValue() : -1L;
    }

    private io.jsonwebtoken.JwtBuilder base(String subject, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(subject)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl))).signWith(key);
    }
}
