
package com.rkdevblog.redis.dto;

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
    void defaults_areNull() {
        OtpValidateRequest v = new OtpValidateRequest();
        assertNull(v.getKey());
        assertNull(v.getOtp());
    }

    @Test
    void reflection_canPopulateFields_gettersReturnValues() throws Exception {
        OtpValidateRequest v = new OtpValidateRequest();
        Field key = OtpValidateRequest.class.getDeclaredField("key");
        Field otp = OtpValidateRequest.class.getDeclaredField("otp");
        key.setAccessible(true);
        otp.setAccessible(true);
        key.set(v, "user@example.com");
        otp.set(v, "123456");
        assertEquals("user@example.com", v.getKey());
        assertEquals("123456", v.getOtp());
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
        Method[] methods = OtpValidateRequest.class.getDeclaredMethods();
        for (Method m : methods) {
            String name = m.getName();
            assertNotEquals("setKey", name, "Setter setKey should not exist");
            assertNotEquals("setOtp", name, "Setter setOtp should not exist");

        }
    }
}