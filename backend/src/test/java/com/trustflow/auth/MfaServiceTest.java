package com.trustflow.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MfaServiceTest {

    private final MfaService mfa = new MfaService(30);

    @Test
    void codeIsSixDigitsAndStableWithinAStep() {
        String c = MfaService.code("DEVSECRET000002", 1000);
        assertTrue(c.matches("\\d{6}"));
        assertEquals(c, MfaService.code("DEVSECRET000002", 1000));
    }

    @Test
    void codeChangesWithTheCounterAndTheSecret() {
        assertNotEquals(MfaService.code("DEVSECRET000002", 1000), MfaService.code("DEVSECRET000002", 1001));
        assertNotEquals(MfaService.code("DEVSECRET000002", 1000), MfaService.code("ADMINSECRET0001", 1000));
    }

    @Test
    void currentCodeVerifies() {
        assertTrue(mfa.verify("DEVSECRET000002", mfa.currentCode("DEVSECRET000002")));
    }

    @Test
    void wrongOrMalformedCodesAreRejected() {
        String right = mfa.currentCode("DEVSECRET000002");
        String wrong = right.equals("000000") ? "111111" : "000000";
        assertFalse(mfa.verify("DEVSECRET000002", wrong));
        assertFalse(mfa.verify("DEVSECRET000002", "12ab56"));
        assertFalse(mfa.verify("DEVSECRET000002", null));
    }
}
