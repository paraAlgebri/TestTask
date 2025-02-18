package org.example.testtask.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.testtask.domain.model.Product;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisProductCache {
    private static final String CACHE_PREFIX = "product:";
    private static final String LOCK_PREFIX = "lock:product:";
    private static final String PRODUCTS_KEY = "products";


    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheConfiguration cacheConfig;
    private final Map<String, Product> localCache = new ConcurrentHashMap<>();


    public void cacheProduct(Product product) {
        String key = CACHE_PREFIX + product.getProductId();
        String lockKey = LOCK_PREFIX + product.getProductId();

        try {
            boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "locked", 5, TimeUnit.SECONDS));

            if (locked) {
                try {
                    redisTemplate.opsForValue().set(
                            key,
                            product,
                            cacheConfig.getTimeoutHours(),
                            TimeUnit.HOURS
                    );
                    localCache.put(product.getProductId(), product);
                    log.debug("Cached product: {}", product.getProductId());
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }
        } catch (Exception e) {
            log.error("Error caching product: {}", product.getProductId(), e);
            localCache.put(product.getProductId(), product);
        }
    }


    public Optional<Product> getProduct(String productId) {
        // First check local cache
        Product localProduct = localCache.get(productId);
        if (localProduct != null) {
            log.debug("Product {} found in local cache", productId);
            return Optional.of(localProduct);
        }

        String key = CACHE_PREFIX + productId;

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Product product) {
                // Update local cache
                localCache.put(productId, product);
                log.debug("Product {} found in Redis cache", productId);
                return Optional.of(product);
            }
        } catch (Exception e) {
            log.error("Error retrieving product from cache: {}", productId, e);
        }

        return Optional.empty();
    }

    public Mono<Product> getProductReactive(String productId) {
        return Mono.fromSupplier(() -> getProduct(productId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .retryWhen(Retry.fixedDelay(
                        cacheConfig.getMaxRetries(),
                        Duration.ofMillis(cacheConfig.getRetryDelayMs())
                ))
                .doOnError(e -> log.error("Error in reactive cache access: {}", productId, e));
    }

    public void invalidateCache(String productId) {
        String key = CACHE_PREFIX + productId;
        try {
            redisTemplate.delete(key);
            localCache.remove(productId);
            log.debug("Invalidated cache for product: {}", productId);
        } catch (Exception e) {
            log.error("Error invalidating cache for product: {}", productId, e);
        }
    }

    public void bulkCache(Map<String, Product> products) {
        String lockKey = LOCK_PREFIX + "bulk";

        try {
            boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "locked", 30, TimeUnit.SECONDS));

            if (locked) {
                try {
                    products.forEach((id, product) -> {
                        String key = CACHE_PREFIX + id;
                        redisTemplate.opsForValue().set(
                                key,
                                product,
                                cacheConfig.getTimeoutHours(),
                                TimeUnit.HOURS
                        );
                    });
                    localCache.putAll(products);
                    log.debug("Bulk cached {} products", products.size());
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }
        } catch (Exception e) {
            log.error("Error in bulk caching", e);
            localCache.putAll(products);
        }
    }

    public void saveProduct(Product product) {
        redisTemplate.opsForHash().put(PRODUCTS_KEY, product.getProductId(), product);
    }




}