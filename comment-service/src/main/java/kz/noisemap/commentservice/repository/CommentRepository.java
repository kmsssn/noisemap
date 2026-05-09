package kz.noisemap.commentservice.repository;

import kz.noisemap.commentservice.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    Page<Comment> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Comment> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Комментарии в bounding box на карте.
     */
    @Query("{ 'location': { $geoWithin: { $box: [ [?0, ?1], [?2, ?3] ] } }, 'deleted': false }")
    List<Comment> findInBoundingBox(double minLng, double minLat, double maxLng, double maxLat);

    /**
     * Комментарии рядом с точкой.
     */
    @Query("{ 'location': { $nearSphere: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } }, 'deleted': false }")
    List<Comment> findNearPoint(double longitude, double latitude, double maxDistanceMeters);
}
