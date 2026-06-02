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

    @Query("{ 'deleted': false, 'hidden': { $ne: true } }")
    Page<Comment> findVisible(Pageable pageable);


    @Query("{ 'userId': ?0, 'deleted': false, 'hidden': { $ne: true } }")
    Page<Comment> findVisibleByUser(UUID userId, Pageable pageable);


    @Query("{ 'deleted': false }")
    Page<Comment> findForModeration(Pageable pageable);


    @Query("{ 'location': { $geoWithin: { $box: [ [?0, ?1], [?2, ?3] ] } }, 'deleted': false, 'hidden': { $ne: true } }")
    List<Comment> findInBoundingBox(double minLng, double minLat, double maxLng, double maxLat);


    @Query("{ 'location': { $nearSphere: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } }, 'deleted': false, 'hidden': { $ne: true } }")
    List<Comment> findNearPoint(double longitude, double latitude, double maxDistanceMeters);
}
