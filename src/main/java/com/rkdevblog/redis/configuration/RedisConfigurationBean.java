package com.rkdevblog.redis.configuration;

import io.lettuce.core.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.UnaryOperator;
@Configuration
public class RedisConfigurationBean {

    private final String url;
    private final int port;
    private final String password;
    private final String useSSL;


    public RedisConfigurationBean(@Value("${spring.redis.host}") String url,
                                  @Value("${spring.redis.port}") int port,
                                  @Value("${spring.redis.ssl}")  String useSSL,
                                  @Value("${spring.redis.password}") String password) {
        this.url = url;
        this.port = port;
        this.useSSL = useSSL;
        this.password = password;

    }
    
	@Bean(destroyMethod = "shutdown")
	ClientResources clientResources() {
		UnaryOperator<HostAndPort> mappingFunction = hostAndPort -> {
			InetAddress[] addresses = new InetAddress[0];
			try {
				addresses = DnsResolvers.JVM_DEFAULT.resolve(url);
			}
			catch (UnknownHostException e) {
				e.printStackTrace();
			}
			String cacheIP = addresses[0].getHostAddress();
			HostAndPort finalAddress = hostAndPort;

			if (hostAndPort.hostText.equals(cacheIP))
				finalAddress = HostAndPort.of(url, hostAndPort.getPort());
			return finalAddress;
		};

		MappingSocketAddressResolver resolver = MappingSocketAddressResolver.create(DnsResolvers.JVM_DEFAULT,
				mappingFunction);

		return ClientResources.builder().nettyCustomizer(new NettyCustomizer() {
			@Override
			public void afterBootstrapInitialized(Bootstrap bootstrap) {
				bootstrap.option(EpollChannelOption.TCP_KEEPIDLE, 15);
				bootstrap.option(EpollChannelOption.TCP_KEEPINTVL, 5);
				bootstrap.option(EpollChannelOption.TCP_KEEPCNT, 3);
				bootstrap.option(EpollChannelOption.TCP_USER_TIMEOUT, 100000);
				bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			}
		}).socketAddressResolver(resolver).build();
	}

    
    /**
     * Redis configuration
     *
     * @return redisStandaloneConfiguration
     */

    @Bean
	public RedisConnectionFactory redisConnectionFactory() {
//		RedisNode redisNode = RedisNode.newRedisNode().listeningAt(redisHost, Integer.parseInt(redisPort)).build();

		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(url);
		config.setPassword(password);
		config.setPort(port);

		LettuceClientConfiguration clientConfig;

		if("false".equals(useSSL)) {
			 clientConfig = LettuceClientConfiguration.builder().clientOptions(clientOptions())
																				.clientResources(clientResources()).build();
		} else {
			clientConfig = LettuceClientConfiguration.builder().clientOptions(clientOptions()).clientResources(clientResources()).useSsl().build();
		}

		return new LettuceConnectionFactory(config, clientConfig);
	}

    /**
     * Client Options
     * Reject requests when redis is in disconnected state and
     * Redis will retry to connect automatically when redis server is down
     *
     * @return client options
     */
    @Bean
    public ClientOptions clientOptions() {
        return ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build();
    }

    
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    @Primary
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

}


