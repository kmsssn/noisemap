package kz.noisemap.gamificationservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Кэш в памяти с Caffeine.
 * Определения ачивок меняются редко — кэшируем на 10 минут.
 * При изменении через Admin API — кэш инвалидируется принудительно.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("achievement_definitions");
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats() // для мониторинга hit/miss
        );
        return manager;
    }
}
