package com.greenmobility.payment;

import com.greenmobility.modules.payment.gateway.HmacUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HmacUtilTest {

    @Test
    @DisplayName("R7: Constant-time string equals prevents timing attacks")
    void testConstantTimeEquals() {
        String a = "a1b2c3d4e5f6";
        String b = "a1b2c3d4e5f6";
        String c = "a1b2c3d4e5f7";

        assertTrue(HmacUtil.constantTimeEquals(a, b));
        assertFalse(HmacUtil.constantTimeEquals(a, c));
        assertFalse(HmacUtil.constantTimeEquals(a, null));
        assertFalse(HmacUtil.constantTimeEquals(null, b));
    }

    @Test
    @DisplayName("HMAC-SHA512 outputs correct deterministic hex string")
    void testHmacSha512() {
        String secret = "TEST_SECRET_KEY";
        String data = "vnp_Amount=1000000&vnp_Command=pay&vnp_TxnRef=123456";

        String hash1 = HmacUtil.hmacSha512(secret, data);
        String hash2 = HmacUtil.hmacSha512(secret, data);

        assertNotNull(hash1);
        assertEquals(128, hash1.length()); // SHA-512 is 64 bytes = 128 hex chars
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("HMAC-SHA256 outputs correct deterministic hex string")
    void testHmacSha256() {
        String secret = "TEST_MOMO_SECRET";
        String data = "partnerCode=MOMO&orderId=123456&amount=50000";

        String hash1 = HmacUtil.hmacSha256(secret, data);
        String hash2 = HmacUtil.hmacSha256(secret, data);

        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 is 32 bytes = 64 hex chars
        assertEquals(hash1, hash2);
    }
}

