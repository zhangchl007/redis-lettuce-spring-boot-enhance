package com.rkdevblog.controller;

import com.rkdevblog.redis.controller.Controller;
import com.rkdevblog.redis.dto.OtpRequest;
import com.rkdevblog.redis.dto.OtpValidateRequest;
import com.rkdevblog.redis.repository.CacheRepository;
import com.rkdevblog.redis.util.OtpGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Controller without starting Spring context.
 */
@ExtendWith(MockitoExtension.class)
class ControllerTest {

    @Mock CacheRepository cacheRepository;
    @Mock OtpGenerator otpGenerator;
    @InjectMocks Controller controller;

    @Test
    void generateOtp_success() {
        OtpRequest req = mock(OtpRequest.class);
        when(req.getEmail()).thenReturn("user@example.com");
        when(otpGenerator.generateOtp()).thenReturn(123456);

        var resp = controller.addToCache(req);

        assertEquals(200, resp.getStatusCodeValue());
        assertTrue(resp.getBody().contains("123456"));
        verify(otpGenerator).generateOtp();
        verify(cacheRepository).put("user@example.com", 123456);
        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void verify_success_removesKey() {
        OtpValidateRequest v = mock(OtpValidateRequest.class);
        when(v.getKey()).thenReturn("user2@example.com");
        when(v.getOtp()).thenReturn("123456");
        when(cacheRepository.get("user2@example.com")).thenReturn(Optional.of("123456"));

        var resp = controller.removeFromCache(v);

        assertEquals(200, resp.getStatusCodeValue());
        verify(cacheRepository).get("user2@example.com");
        verify(cacheRepository).remove("user2@example.com");
        verifyNoMoreInteractions(cacheRepository);
    }

    @Test
    void verify_wrongOtp_badRequest() {
        OtpValidateRequest v = mock(OtpValidateRequest.class);
        when(v.getKey()).thenReturn("user3@example.com");
        when(v.getOtp()).thenReturn("000000");
        when(cacheRepository.get("user3@example.com")).thenReturn(Optional.of("123456"));

        var resp = controller.removeFromCache(v);

        assertEquals(400, resp.getStatusCodeValue());
        verify(cacheRepository).get("user3@example.com");
        verify(cacheRepository, never()).remove(anyString());
    }

    @Test
    void verify_missingKey_badRequest() {
        OtpValidateRequest v = mock(OtpValidateRequest.class);
        when(v.getKey()).thenReturn("absent@example.com");
        when(cacheRepository.get("absent@example.com")).thenReturn(Optional.empty());

        var resp = controller.removeFromCache(v);

        assertEquals(400, resp.getStatusCodeValue());
        verify(cacheRepository).get("absent@example.com");
        verify(cacheRepository, never()).remove(anyString());
    }

    @Test
    void generate_multipleCalls_updatesStoredOtp() {
        OtpRequest req = mock(OtpRequest.class);
        when(req.getEmail()).thenReturn("multi@example.com");
        when(otpGenerator.generateOtp()).thenReturn(100000, 100001);

        controller.addToCache(req);
        controller.addToCache(req);

        verify(otpGenerator, times(2)).generateOtp();
        verify(cacheRepository).put("multi@example.com", 100000);
        verify(cacheRepository).put("multi@example.com", 100001);
    }
}