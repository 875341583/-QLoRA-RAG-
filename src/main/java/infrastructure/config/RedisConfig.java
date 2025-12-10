package com.navigation.system.infrastructure.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis配置类
 * 负责配置Redis连接和缓存策略，包括连接池参数、序列化方式、过期时间等设置
 * 
 * @author Alex
 * @version 1.0
 */
@Configuration
public class RedisConfig {

    /**
     * Redis服务器主机地址
     */
    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    /**
     * Redis服务器端口号
     */
    @Value("${spring.redis.port:6379}")
    private int redisPort;

    /**
     * Redis数据库索引
     */
    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    /**
     * Redis连接密码
     */
    @Value("${spring.redis.password:}")
    private String redisPassword;

    /**
     * 连接超时时间（毫秒）
     */
    @Value("${spring.redis.timeout:2000}")
    private long redisTimeout;

    /**
     * 连接池最大连接数
     */
    @Value("${spring.redis.lettuce.pool.max-active:8}")
    private int maxActive;

    /**
     * 连接池最大空闲连接数
     */
    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    /**
     * 连接池最小空闲连接数
     */
    @Value("${spring.redis.lettuce.pool.min-idle:0}")
    private int minIdle;

    /**
     * 连接池最大等待时间（毫秒）
     */
    @Value("${spring.redis.lettuce.pool.max-wait:-1}")
    private long maxWait;

    /**
     * 默认缓存过期时间（秒）
     */
    @Value("${spring.redis.expiration-time:3600}")
    private int expirationTime;

    /**
     * 配置Redis连接工厂
     * 使用Lettuce客户端，支持高性能的异步操作和连接池管理
     * 
     * @return RedisConnectionFactory Redis连接工厂实例
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 配置Redis服务器基本信息
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        // 配置连接池参数
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));

        // 配置Lettuce客户端参数，包含连接池配置
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .shutdownTimeout(Duration.ofSeconds(10))
                .poolConfig(poolConfig)
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * 配置Redis模板
     * 设置键和值的序列化方式，使用JSON格式存储对象
     * 
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate<String, Object> Redis模板实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 使用GenericJackson2JsonRedisSerializer来序列化和反序列化redis的value值
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 获取默认缓存过期时间
     * 
     * @return int 过期时间（秒）
     */
    public int getExpirationTime() {
        return expirationTime;
    }

    /**
     * 获取连接池参数信息
     * 
     * @return String 连接池参数描述
     */
    public String getConnectionPoolParams() {
        return String.format("max-active=%d, max-idle=%d, min-idle=%d, max-wait=%dms", 
                maxActive, maxIdle, minIdle, maxWait);
    }

    /**
     * 获取序列化方式信息
     * 
     * @return String 序列化方式描述
     */
    public String getSerializationMethod() {
        return "Key: StringRedisSerializer, Value: GenericJackson2JsonRedisSerializer";
    }
}


// 内容由AI生成，仅供参考