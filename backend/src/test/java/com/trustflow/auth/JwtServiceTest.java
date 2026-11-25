package com.trustflow.auth;

import com.trustflow.policy.PolicyAction;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    @Test
    void accessTokenRoundTrip() {
        JwtService jwt = new JwtService(SECRET, 15);
        var claims = jwt.parse(jwt.accessToken("dev", List.of("DEVELOPER")));
        assertEquals("dev", claims.getSubject());
        assertEquals(JwtService.ACCESS, claims.get("tt", String.class));
        assertEquals(List.of("DEVELOPER"), claims.get("roles", List.class));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1);
        assertThrows(ExpiredJwtException.class, () -> jwt.parse(jwt.accessToken("dev", List.of())));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 15);
        String token = jwt.accessToken("dev", List.of("DEVELOPER"));
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        assertThrows(JwtException.class, () -> jwt.parse(tampered));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtService("another-secret-another-secret-0123456789ab", 15).accessToken("dev", List.of());
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 15).parse(token));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 15));
    }

    @Test
    void mfaTokenCarriesResourceAndAction() {
        JwtService jwt = new JwtService(SECRET, 15);
        var c = jwt.parse(jwt.mfaToken("dev", 2L, PolicyAction.READ));
        assertEquals(JwtService.MFA, c.get("tt", String.class));
        assertEquals(2L, JwtService.longClaim(c, "rid"));
        assertEquals("READ", c.get("act", String.class));
        assertNotNull(c.getId());
    }
}
