package kz.noisemap.moderationservice.repository;

import kz.noisemap.moderationservice.model.ModerationRecord;
import kz.noisemap.moderationservice.model.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ModerationRepository extends MongoRepository<ModerationRecord, String> {

    Page<ModerationRecord> findByStatus(ModerationStatus status, Pageable pageable);

    long countByStatus(ModerationStatus status);

    long countByUserId(UUID userId);

    long countByUserIdAndFlaggedAtAfter(UUID userId, Instant since);
}