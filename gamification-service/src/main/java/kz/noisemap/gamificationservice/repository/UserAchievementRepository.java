package kz.noisemap.gamificationservice.repository;

import kz.noisemap.gamificationservice.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {
    List<UserAchievement> findByUserId(UUID userId);
    boolean existsByUserIdAndAchievementCode(UUID userId, String achievementCode);
}
