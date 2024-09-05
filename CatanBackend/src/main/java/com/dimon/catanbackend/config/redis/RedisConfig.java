package com.dimon.catanbackend.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration class for setting up Redis in the application. This class configures a custom
 * {@link RedisTemplate} bean that will be used to interact with Redis.
 *
 * The {@link RedisTemplate} is configured with:
 * - {@link StringRedisSerializer} for serializing the Redis keys as strings.
 * - {@link GenericJackson2JsonRedisSerializer} for serializing and deserializing the values as JSON.
 *
 * Annotations used:
 * - {@link Configuration} to indicate that this class contains Spring bean definitions.
 * - {@link Bean} to define a Spring-managed {@link RedisTemplate} bean.
 *
 * Methods:
 * - {@code redisTemplate}: Configures and returns a {@link RedisTemplate} that connects to the Redis server
 *   using the provided {@link RedisConnectionFactory}.
 *
 * Example usage:
 * <pre>
 * {@code
 * @Autowired
 * private RedisTemplate<String, Object> redisTemplate;
 *
 * redisTemplate.opsForValue().set("key", "value");
 * Object value = redisTemplate.opsForValue().get("key");
 * }
 * </pre>
 *
 * @see RedisTemplate
 * @see RedisConnectionFactory
 * @see StringRedisSerializer
 * @see GenericJackson2JsonRedisSerializer
 * @see Configuration
 * @see Bean
 *
 */
@Configuration
public class RedisConfig {

    /**
     * Configures and returns a {@link RedisTemplate} with custom key and value serializers.
     *
     * @param connectionFactory the Redis connection factory used to establish a connection to Redis
     * @return a configured {@link RedisTemplate} for interacting with Redis
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
