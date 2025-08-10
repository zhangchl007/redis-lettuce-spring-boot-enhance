package com.rkdevblog.configuration;

import com.rkdevblog.redis.configuration.RedisConfigurationBean;
import io.lettuce.core.ClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Refactored to avoid brittle reflection of internal LettuceConnectionFactory fields.
 * We now assert only on public, stable APIs: clientOptions() and SSL flag in client configuration.
 * For deeper host/cluster assertions prefer an integration test (@SpringBootTest) with real properties,
 * or add explicit getters in production code if truly required.
 */
class RedisConfigurationBeanTest {

    @Test
    void clientOptionsConfigured() {
        RedisConfigurationBean bean = new RedisConfigurationBean("redis.local", 6379, "false", "standalone", "pass");
        ClientOptions opts = bean.clientOptions();
        assertEquals(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS, opts.getDisconnectedBehavior());
        assertTrue(opts.isAutoReconnect());
    }

    @Test
    void standaloneNoSsl_setsNonSsl() {
        RedisConfigurationBean bean = new RedisConfigurationBean("redis.example", 6380, "false", "standalone", "s3cr3t");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        assertNotNull(factory);
        assertFalse(factory.getClientConfiguration().isUseSsl());
    }

    @Test
    void standaloneWithSsl_setsSsl() {
        RedisConfigurationBean bean = new RedisConfigurationBean("redis.example", 6381, "true", "standalone", "pw");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        assertNotNull(factory);
        assertTrue(factory.getClientConfiguration().isUseSsl());
    }

    @Test
    void clusterNoSsl_setsNonSsl() {
        RedisConfigurationBean bean = new RedisConfigurationBean("ignoredHost", 7000, "false", "cluster", "clusterPw");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        assertNotNull(factory);
        LettuceClientConfiguration cfg = factory.getClientConfiguration();
        assertFalse(cfg.isUseSsl());
    }

    @Test
    void clusterWithSsl_setsSsl() {
        RedisConfigurationBean bean = new RedisConfigurationBean("ignoredHost", 7001, "true", "cluster", "clusterPw2");
        LettuceConnectionFactory factory = (LettuceConnectionFactory) bean.redisConnectionFactory();
        assertNotNull(factory);
        assertTrue(factory.getClientConfiguration().isUseSsl());
    }

    @Test
    void redisTemplateUsesProvidedFactory() {
        RedisConfigurationBean bean = new RedisConfigurationBean("h", 6379, "false", "standalone", "");
        LettuceConnectionFactory mockFactory = mock(LettuceConnectionFactory.class);
        RedisTemplate<String, String> template = bean.redisTemplate(mockFactory);
        assertNotNull(template);
        assertSame(mockFactory, template.getConnectionFactory());
        assertTrue(template instanceof StringRedisTemplate);
    }
}
