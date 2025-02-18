package org.example.testtask.config;

import org.example.testtask.domain.service.ProductService;
import org.example.testtask.domain.service.TradeService;
import org.example.testtask.infrastructure.cache.CacheConfiguration;
import org.example.testtask.infrastructure.cache.RedisProductCache;
import org.example.testtask.infrastructure.parser.CsvParser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public CsvParser csvParser() {
        return new CsvParser();
    }

    @Bean
    @Primary
    public TradeService tradeService() {
        return new TradeService();
    }

    @Bean
    @Primary
    public ProductService productService() {
        return new ProductService(redisProductCache());
    }

    @Bean
    @Primary
    public RedisProductCache redisProductCache() {
        return new RedisProductCache(redisTemplate(), cacheConfiguration());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return new RedisTemplate<>();
    }

    @Bean
    public CacheConfiguration cacheConfiguration() {
        return new CacheConfiguration();
    }
}
