package kz.noisemap.notificationservice.repository;

import kz.noisemap.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndReadFalse(UUID userId);
    @Update("{ '$set': { 'read': true } }")
    long findAndUpdateByUserIdAndReadFalse(UUID userId);
}