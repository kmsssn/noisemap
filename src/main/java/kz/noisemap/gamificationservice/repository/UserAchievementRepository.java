package kz.noisemap.gamificationservice.repository;

import kz.noisemap.gamificationservice.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    /**
     * Все ачивки пользователя — одним запросом.
     * Используется в AchievementEngine для загрузки Set<String> кодов.
     */
    List<UserAchievement> findByUserId(UUID userId);

    /**
     * Проверка наличия ачивки — используется только в edge cases.
     * В основном потоке заменена на Set.contains() из loadUnlockedCodes().
     */
    boolean existsByUserIdAndAchievementCode(UUID userId, String achievementCode);

    /**
     * Получить ачивки с join на определения — для отображения профиля.
     * Загружает definition за один запрос через JOIN FETCH.
     */
    @Query("SELECT ua FROM UserAchievement ua " +
           "JOIN FETCH ua.definition d " +
           "WHERE ua.userId = :userId " +
           "ORDER BY ua.unlockedAt DESC")
    List<UserAchievement> findByUserIdWithDefinitions(UUID userId);
}
