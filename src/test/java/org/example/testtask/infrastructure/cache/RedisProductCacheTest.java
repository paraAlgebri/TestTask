package org.example.testtask.infrastructure.cache;

import org.example.testtask.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisProductCacheTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisProductCacheTest.class);

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private CacheConfiguration cacheConfig;

    @InjectMocks
    private RedisProductCache redisProductCache;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleProduct = new Product();
        sampleProduct.setProductId("123");
        sampleProduct.setProductName("Test Product");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(cacheConfig.getTimeoutHours()).thenReturn(24L);
    }

    @Test
    void testCacheProduct() {
        logger.info("Тест кешування продукту");

        when(valueOperations.setIfAbsent(eq("lock:product:123"), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        redisProductCache.cacheProduct(sampleProduct);

        verify(valueOperations).set(eq("product:123"), eq(sampleProduct), eq(24L), eq(TimeUnit.HOURS));
        verify(redisTemplate).delete(eq("lock:product:123"));

        logger.info("Продукт успішно закешовано та блокування знято");
    }

    @Test
    void testGetProduct_Cached() {
        logger.info("Тест отримання кешованого продукту");

        when(valueOperations.get("product:123")).thenReturn(sampleProduct);

        Optional<Product> retrievedProduct = redisProductCache.getProduct("123");

        assertTrue(retrievedProduct.isPresent(), "Продукт повинен бути в кеші");
        assertEquals(sampleProduct, retrievedProduct.get(), "Отриманий продукт не відповідає очікуваному");

        logger.info("Продукт успішно отримано з кешу: {}", retrievedProduct.get());
    }

    @Test
    void testGetProduct_NotCached() {
        logger.info("Тест отримання продукту, якого немає в кеші");

        when(valueOperations.get("product:999")).thenReturn(null);

        Optional<Product> retrievedProduct = redisProductCache.getProduct("999");

        assertFalse(retrievedProduct.isPresent(), "Продукт не повинен бути в кеші");

        logger.info("Продукт відсутній у кеші");
    }

    @Test
    void testInvalidateCache() {
        logger.info("Тест видалення продукту з кешу");

        redisProductCache.invalidateCache("123");

        verify(redisTemplate).delete("product:123");

        logger.info("Продукт успішно видалено з кешу");
    }
}
