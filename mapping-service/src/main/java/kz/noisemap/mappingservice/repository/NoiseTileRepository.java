package kz.noisemap.mappingservice.repository;

import kz.noisemap.mappingservice.model.NoiseTile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoiseTileRepository extends MongoRepository<NoiseTile, String> {

    Optional<NoiseTile> findByTileKey(String tileKey);

    /**
     * Найти тайлы в bounding box.
     * center хранится как [lng, lat], запрос по GeoJSON box.
     */
    @Query("{ 'center': { $geoWithin: { $box: [ [?0, ?1], [?2, ?3] ] } } }")
    List<NoiseTile> findTilesInBoundingBox(
            double minLng, double minLat,
            double maxLng, double maxLat);

    /**
     * Найти тайлы рядом с точкой в радиусе (метры).
     */
    @Query("{ 'center': { $nearSphere: { $geometry: { type: 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
    List<NoiseTile> findTilesNearPoint(double longitude, double latitude, double maxDistanceMeters);
}
