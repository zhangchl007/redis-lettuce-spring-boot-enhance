package com.rkdevblog.redis.repository;

import com.rkdevblog.redis.exception.OTPServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class OTPCacheRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String,String> valueOps;
    private RedisConnectionFactory connectionFactory;
    private RedisConnection connection;
    private OTPCacheRepository repository;
    private final long TTL = 120L;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = (ValueOperations<String,String>) mock(ValueOperations.class);
        connectionFactory = mock(RedisConnectionFactory.class);
        connection = mock(RedisConnection.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);

        repository = new OTPCacheRepository(redisTemplate, TTL);
    }

    @Test
    void put_success_storesValue_and_setsExpire() {
        repository.put("k1", 123456);
        verify(valueOps).set("k1", "123456");
        verify(redisTemplate).expire("k1", TTL, TimeUnit.SECONDS);
    }

    @Test
    void put_runtimeException_wrappedInOTPServiceException() {
        doThrow(new RuntimeException("boom")).when(valueOps).set(anyString(), anyString());
        OTPServiceException ex = assertThrows(OTPServiceException.class,
                () -> repository.put("kErr", 42));
        assertTrue(ex.getMessage().contains("saving"));
    }

    @Test
    void get_present_returnsOptionalWithValue() {
        when(redisTemplate.hasKey("k2")).thenReturn(true);
        when(valueOps.get("k2")).thenReturn("888999");
        Optional<String> result = repository.get("k2");
        assertTrue(result.isPresent());
        assertEquals("888999", result.get());
    }

    @Test
    void get_absent_returnsEmptyOptional() {
        when(redisTemplate.hasKey("missing")).thenReturn(false);
        Optional<String> result = repository.get("missing");
        assertTrue(result.isEmpty());
        verify(valueOps, never()).get("missing");
    }

    @Test
    void get_exception_wrapped() {
        when(redisTemplate.hasKey("err")).thenThrow(new RuntimeException("fail"));
        OTPServiceException ex = assertThrows(OTPServiceException.class,
                () -> repository.get("err"));
        assertTrue(ex.getMessage().contains("retrieving"));
    }

    @Test
    void remove_success_deletesKey() {
        repository.remove("delKey");
        verify(redisTemplate).delete("delKey");
    }

    @Test
    void remove_exception_wrapped() {
        doThrow(new RuntimeException("dFail")).when(redisTemplate).delete("bad");
        OTPServiceException ex = assertThrows(OTPServiceException.class,
                () -> repository.remove("bad"));
        assertTrue(ex.getMessage().contains("removing"));
    }

    @Test
    void ping_success_returnsPong() {
        when(connection.ping()).thenReturn("PONG");
        String r = repository.ping();
        assertEquals("PONG", r);
        verify(connection).ping();
    }

    @Test
    void ping_exception_wrapped() {
        when(connection.ping()).thenThrow(new RuntimeException("redis down"));
        OTPServiceException ex = assertThrows(OTPServiceException.class, repository::ping);
        assertTrue(ex.getMessage().contains("pinging"));
    }

    @Test
    void put_capturesArgumentsCorrectly() {
        repository.put("captureKey", 777);
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCap.capture(), valCap.capture());
        assertEquals("captureKey", keyCap.getValue());
        assertEquals("777", valCap.getValue());
    }
}