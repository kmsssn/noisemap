package kz.noisemap.gamificationservice.repository;

import kz.noisemap.gamificationservice.model.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, String> {

    // Все активные ачивки — загружаются при проверке условий
    List<AchievementDefinition> findByActiveTrue();

    // По типу триггера — для эффективной проверки
    List<AchievementDefinition> findByTriggerTypeAndActiveTrue(String triggerType);
}
