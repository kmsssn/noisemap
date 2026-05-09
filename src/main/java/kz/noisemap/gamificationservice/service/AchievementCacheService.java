package kz.noisemap.gamificationservice.service;

import kz.noisemap.gamificationservice.model.AchievementDefinition;
import kz.noisemap.gamificationservice.repository.AchievementDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Кэш определений ачивок.
 * Ачивки меняются редко — кэшируем чтобы не ходить в БД при каждой проверке.
 * Кэш инвалидируется когда Admin добавляет/изменяет/деактивирует ачивку.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementCacheService {

    private final AchievementDefinitionRepository definitionRepository;

    @Cacheable("achievement_definitions")
    public List<AchievementDefinition> getAllActive() {
        log.debug("Loading achievement definitions from DB (cache miss)");
        return definitionRepository.findByActiveTrue();
    }

    @CacheEvict(value = "achievement_definitions", allEntries = true)
    public AchievementDefinition save(AchievementDefinition definition) {
        log.info("Achievement definition saved, cache evicted: {}", definition.getCode());
        return definitionRepository.save(definition);
    }

    @CacheEvict(value = "achievement_definitions", allEntries = true)
    public void deactivate(String code) {
        definitionRepository.findById(code).ifPresent(def -> {
            def.setActive(false);
            definitionRepository.save(def);
            log.info("Achievement deactivated, cache evicted: {}", code);
        });
    }
}
