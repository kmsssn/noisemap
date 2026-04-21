package kz.noisemap.statisticsservice.repository;

import kz.noisemap.statisticsservice.model.NoiseMeasurement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NoiseMeasurementRepository extends MongoRepository<NoiseMeasurement, String> {

    List<NoiseMeasurement> findByUserId(UUID userId);

    List<NoiseMeasurement> findByRecordedAtBetween(Instant from, Instant to);

    long countByUserId(UUID userId);

    List<NoiseMeasurement> findByUserIdAndRecordedAtBetween(UUID userId, Instant from, Instant to);
}
