// Pseudocode:
// - Fix package to com.rkdevblog.redis.ota to match directory and resolve mismatch.
// - Keep tests identical.

package com.rkdevblog.redis.ota;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;
public class OtpValidateRequest {

    private String key;
    private String otp;

    public String getKey() {
        return key;
    }

    public String getOtp() {
        return otp;
    }

}


class OtpValidateRequestTest {

    @Test
    void defaultValues_null() {
        OtpValidateRequest req = new OtpValidateRequest();
        assertNull(req.getKey(), "key should be null by default");
        assertNull(req.getOtp(), "otp should be null by default");
    }

    @Test
    void reflectiveSetFields_gettersReturnValues() throws Exception {
        OtpValidateRequest req = new OtpValidateRequest();

        Field keyField = OtpValidateRequest.class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(req, "user@example.com");

        Field otpField = OtpValidateRequest.class.getDeclaredField("otp");
        otpField.setAccessible(true);
        otpField.set(req, "123456");

        assertEquals("user@example.com", req.getKey());
        assertEquals("123456", req.getOtp());
    }

    @Test
    void fieldsArePrivate() throws Exception {
        Field keyField = OtpValidateRequest.class.getDeclaredField("key");
        Field otpField = OtpValidateRequest.class.getDeclaredField("otp");
        assertTrue(Modifier.isPrivate(keyField.getModifiers()), "key must be private");
        assertTrue(Modifier.isPrivate(otpField.getModifiers()), "otp must be private");
    }

    @Test
    void noSettersPresent() {
        Method[] methods = OtpValidateRequest.class.getMethods();
        for (Method m : methods) {
            String name = m.getName();
            assertNotEquals("setKey", name, "Setter setKey should not exist");
            assertNotEquals("setOtp", name, "Setter setOtp should not exist");

        }
    }
}