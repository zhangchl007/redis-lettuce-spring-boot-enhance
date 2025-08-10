package com.rkdevblog.configuration;

import com.rkdevblog.redis.configuration.RedisConfigurationBean;
import io.lettuce.core.ClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RedisConfigurationBeanTest {

    // Fallback reflective field fetch
    private Object reflectField(Object target, String... candidateNames) {
        for (String name : candidateNames) {
            Class<?> c = target.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    private RedisStandaloneConfiguration standaloneConfig(LettuceConnectionFactory f) {
        try {
            Method m = LettuceConnectionFactory.class.getMethod("getStandaloneConfiguration");
            return (RedisStandaloneConfiguration) m.invoke(f);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }
        Object val = reflectField(f, "standaloneConfig");
        return (RedisStandaloneConfiguration) val;
    }

    private RedisClusterConfiguration clusterConfig(LettuceConnectionFactory f) {
        try {
            Method m = LettuceConnectionFactory.class.getMethod("getClusterConfiguration");
            return (RedisClusterConfiguration) m.invoke(f);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }
        Object val = reflectField(f, "clusterConfiguration", "configuration"); // legacy names
        return (RedisClusterConfiguration) val;
    }

    @Test
    void clientOptionsConfigured() {
        RedisConfigurationBean bean = new RedisConfigurationBean("redis.local", 6379, "false", "standalone", "pass");
        ClientOptions opts = bean.clientOptions();
        assertEquals(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS, opts.getDisconnectedBehavior());
        assertTrue(opts.isAutoReconnect());
    }

    @Test
    void standaloneNoSslConfiguration() {
        RedisConfigurationBean bean = new RedisConfigurationBean("redis.example", 6380, "false", "standalone", "s3cr3t");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        RedisStandaloneConfiguration sc = standaloneConfig(factory);
        assertNotNull(sc, "Standalone config should be present");
        assertEquals("redis.example", sc.getHostName());
        assertEquals(6380, sc.getPort());
        assertNotNull(sc.getPassword());
        LettuceClientConfiguration clientCfg = factory.getClientConfiguration();
        assertFalse(clientCfg.isUseSsl());
    }

    @Test
    void standaloneWithSslConfiguration() {
        RedisConfigurationBean bean = new RedisConfigurationBean("redis.example", 6381, "true", "standalone", "pw");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        RedisStandaloneConfiguration sc = standaloneConfig(factory);
        assertNotNull(sc);
        assertEquals(6381, sc.getPort());
        assertNotNull(sc.getPassword());
        assertTrue(factory.getClientConfiguration().isUseSsl());
    }

    @Test
    void clusterNoSslConfiguration() {
        RedisConfigurationBean bean = new RedisConfigurationBean("ignoredHost", 7000, "false", "cluster", "clusterPw");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        RedisClusterConfiguration cc = clusterConfig(factory);
        assertNotNull(cc, "Cluster config should be present");
        assertEquals(1, cc.getClusterNodes().size());
        assertTrue(cc.getClusterNodes().stream().anyMatch(n -> n.getHost().equals("localhost") && n.getPort() == 6379));
        assertNotNull(cc.getPassword());
        assertFalse(factory.getClientConfiguration().isUseSsl());
    }

    @Test
    void clusterWithSslConfiguration() {
        RedisConfigurationBean bean = new RedisConfigurationBean("ignoredHost", 7001, "true", "cluster", "clusterPw2");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        RedisClusterConfiguration cc = clusterConfig(factory);
        assertNotNull(cc);
        assertEquals(1, cc.getClusterNodes().size());
        assertTrue(factory.getClientConfiguration().isUseSsl());
    }

    @Test
    void redisTemplateUsesProvidedFactory() {
        RedisConfigurationBean bean = new RedisConfigurationBean("h", 6379, "false", "standalone", "");
        LettuceConnectionFactory mockFactory = mock(LettuceConnectionFactory.class);
        RedisTemplate<String, String> tpl = bean.redisTemplate(mockFactory);
        assertNotNull(tpl);
        assertSame(mockFactory, tpl.getConnectionFactory());
        assertTrue(tpl instanceof StringRedisTemplate);
    }
}