package com.rkdevblog.controller;

import com.rkdevblog.redis.controller.Controller;
import com.rkdevblog.redis.dto.OtpRequest;
import com.rkdevblog.redis.dto.OtpValidateRequest;
import com.rkdevblog.redis.repository.CacheRepository;
import com.rkdevblog.redis.util.OtpGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Controller without starting Spring context.
 */
class ControllerTest {

    private FakeCacheRepository cacheRepository;
    private FixedOtpGenerator fixedOtpGenerator;
    private Controller controller;

    @BeforeEach
    void setup() {
        cacheRepository = new FakeCacheRepository();
        fixedOtpGenerator = new FixedOtpGenerator(123456);
        controller = createControllerInstance();
        injectDependencies(controller, cacheRepository, fixedOtpGenerator);
    }

    private Controller createControllerInstance() {
        try {
            // Try to find a constructor that we can use
            Constructor<?>[] constructors = Controller.class.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                constructor.setAccessible(true);
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == 0) {
                    return (Controller) constructor.newInstance();
                } else {
                    // Create null arguments for the constructor
                    Object[] args = new Object[paramTypes.length];
                    return (Controller) constructor.newInstance(args);
                }
            }
            throw new RuntimeException("No suitable constructor found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Controller instance", e);
        }
    }

    private void injectDependencies(Controller target, CacheRepository repo, OtpGenerator gen) {
        try {
            Field repoField = Controller.class.getDeclaredField("cacheRepository");
            repoField.setAccessible(true);
            repoField.set(target, repo);
            Field genField = Controller.class.getDeclaredField("otpGenerator");
            genField.setAccessible(true);
            genField.set(target, gen);
        } catch (Exception e) {
            throw new RuntimeException("Dependency injection failed", e);
        }
    }

    @Test
    void generateOtp_success() throws Exception {
        OtpRequest request = buildOtpRequest("user@example.com");
        var response = controller.addToCache(request);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("123456"));
        assertEquals(Optional.of("123456"), cacheRepository.get("user@example.com"));
    }

    @Test
    void verify_success_removesKey() throws Exception {
        OtpRequest request = buildOtpRequest("user2@example.com");
        controller.addToCache(request);
        OtpValidateRequest validate = buildOtpValidateRequest("user2@example.com", "123456");
        var response = controller.removeFromCache(validate);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(Optional.empty(), cacheRepository.get("user2@example.com"));
    }

    @Test
    void verify_wrongOtp_badRequest() throws Exception {
        OtpRequest request = buildOtpRequest("user3@example.com");
        controller.addToCache(request);
        OtpValidateRequest validate = buildOtpValidateRequest("user3@example.com", "000000");
        var response = controller.removeFromCache(validate);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals(Optional.of("123456"), cacheRepository.get("user3@example.com"));
    }

    @Test
    void verify_missingKey_badRequest() throws Exception {
        OtpValidateRequest validate = buildOtpValidateRequest("absent@example.com", "999999");
        var response = controller.removeFromCache(validate);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void generate_multipleCalls_updatesStoredOtp() throws Exception {
        // swap generator to incrementing one
        IncrementingOtpGenerator inc = new IncrementingOtpGenerator(100000);
        injectDependencies(controller, cacheRepository, inc);
        OtpRequest req = buildOtpRequest("multi@example.com");
        controller.addToCache(req);
        assertEquals(Optional.of("100000"), cacheRepository.get("multi@example.com"));
        controller.addToCache(req);
        assertEquals(Optional.of("100001"), cacheRepository.get("multi@example.com"));
    }

    // ---------- DTO builders (reflection tolerant) ----------

    private OtpRequest buildOtpRequest(String email) throws Exception {
        return (OtpRequest) buildDto(
                OtpRequest.class,
                new String[]{"email"},
                new String[]{email},
                new String[]{"setEmail"}
        );
    }

    private OtpValidateRequest buildOtpValidateRequest(String key, String otp) throws Exception {
        return (OtpValidateRequest) buildDto(
                OtpValidateRequest.class,
                new String[]{"key", "otp"},
                new String[]{key, otp},
                new String[]{"setKey", "setOtp"}
        );
    }

    private Object buildDto(Class<?> clazz,
                            String[] fieldNames,
                            String[] values,
                            String[] setterNames) throws Exception {
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == values.length) {
                boolean allString = true;
                for (Class<?> t : ctor.getParameterTypes()) {
                    if (!t.equals(String.class)) { allString = false; break; }
                }
                if (allString) {
                    ctor.setAccessible(true);
                    return ctor.newInstance((Object[]) values);
                }
            }
        }
        Object instance = clazz.getDeclaredConstructor().newInstance();
        for (int i = 0; i < fieldNames.length; i++) {
            boolean set = false;
            try {
                Method m = clazz.getMethod(setterNames[i], String.class);
                m.invoke(instance, values[i]);
                set = true;
            } catch (NoSuchMethodException ignored) {}
            if (!set) {
                try {
                    Field f = clazz.getDeclaredField(fieldNames[i]);
                    f.setAccessible(true);
                    f.set(instance, values[i]);
                } catch (NoSuchFieldException ignored) {}
            }
        }
        return instance;
    }

    // ---------- Test doubles ----------

    static class FakeCacheRepository implements CacheRepository {
        private final Map<String, String> store = new ConcurrentHashMap<>();
        @Override public void put(String key, Integer value) { store.put(key, String.valueOf(value)); }
        @Override public Optional<String> get(String key) { return Optional.ofNullable(store.get(key)); }
        @Override public void remove(String key) { store.remove(key); }
        @Override public String ping() { return "PONG"; }
    }

    static class FixedOtpGenerator extends OtpGenerator {
        private final int fixed;
        FixedOtpGenerator(int fixed) { this.fixed = fixed; }
        @Override public int generateOtp() { return fixed; }
    }

    static class IncrementingOtpGenerator extends OtpGenerator {
        private int current;
        IncrementingOtpGenerator(int start) { this.current = start; }
        @Override public int generateOtp() { return current++; }
    }
}