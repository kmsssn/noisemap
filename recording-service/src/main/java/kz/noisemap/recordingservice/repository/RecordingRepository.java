package kz.noisemap.recordingservice.repository;

import kz.noisemap.recordingservice.model.Recording;
import kz.noisemap.recordingservice.model.RecordingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecordingRepository extends MongoRepository<Recording, String> {
    Page<Recording> findByUserId(UUID userId, Pageable pageable);
    Page<Recording> findByStatus(RecordingStatus status, Pageable pageable);
    long countByUserId(UUID userId);
}
