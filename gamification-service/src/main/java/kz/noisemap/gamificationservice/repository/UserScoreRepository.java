package kz.noisemap.gamificationservice.repository;

import kz.noisemap.gamificationservice.model.UserScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserScoreRepository extends JpaRepository<UserScore, UUID> {

    @Query("SELECT u FROM UserScore u ORDER BY u.totalPoints DESC")
    List<UserScore> findTopByPoints(Pageable pageable);
}
