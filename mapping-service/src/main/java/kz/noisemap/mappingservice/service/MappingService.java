package kz.noisemap.mappingservice.service;

import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.mappingservice.dto.MapDto;
import kz.noisemap.mappingservice.model.NoiseTile;
import kz.noisemap.mappingservice.repository.NoiseTileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingService {

    private final NoiseTileRepository tileRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final double TILE_SIZE_DEGREES = 0.001; // ~111 метров
    private static final String CACHE_PREFIX = "heatmap:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Получить тайлы тепловой карты в bounding box.
     * Сначала проверяет Redis кэш, при промахе — запрос в MongoDB.
     */
    public MapDto.HeatmapResponse getHeatmap(MapDto.HeatmapRequest request) {
        String cacheKey = buildCacheKey(request);

        // Проверяем кэш
        @SuppressWarnings("unchecked")
        MapDto.HeatmapResponse cached = (MapDto.HeatmapResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Heatmap cache hit: {}", cacheKey);
            return cached;
        }

        // Запрос в MongoDB
        List<NoiseTile> tiles = tileRepository.findTilesInBoundingBox(
                request.getMinLng(), request.getMinLat(),
                request.getMaxLng(), request.getMaxLat()
        );

        List<MapDto.TileResponse> tileResponses = tiles.stream()
                .map(this::toTileResponse)
                .collect(Collectors.toList());

        double overallAvg = tiles.stream()
                .filter(t -> t.getAvgNoiseLevelDba() != null)
                .mapToDouble(NoiseTile::getAvgNoiseLevelDba)
                .average()
                .orElse(0.0);

        int totalMeasurements = tiles.stream()
                .mapToInt(t -> t.getMeasurementCount() != null ? t.getMeasurementCount() : 0)
                .sum();

        MapDto.HeatmapResponse response = MapDto.HeatmapResponse.builder()
                .tiles(tileResponses)
                .overallAvgDba(overallAvg)
                .totalMeasurements(totalMeasurements)
                .boundingBox(MapDto.BoundingBox.builder()
                        .minLat(request.getMinLat())
                        .minLng(request.getMinLng())
                        .maxLat(request.getMaxLat())
                        .maxLng(request.getMaxLng())
                        .build())
                .build();

        // Кэшируем
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);

        return response;
    }

    /**
     * Найти тайлы рядом с точкой.
     */
    public List<MapDto.TileResponse> getTilesNearPoint(double lat, double lng, double radiusMeters) {
        return tileRepository.findTilesNearPoint(lng, lat, radiusMeters).stream()
                .map(this::toTileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Обновить тайл при поступлении нового classification.completed.
     * Вызывается из event listener.
     */
    public void updateTileWithMeasurement(ClassificationCompletedEvent event) {
        String tileKey = calculateTileKey(event.getLongitude(), event.getLatitude());

        NoiseTile tile = tileRepository.findByTileKey(tileKey)
                .orElse(NoiseTile.builder()
                        .tileKey(tileKey)
                        .center(new double[]{event.getLongitude(), event.getLatitude()})
                        .measurementCount(0)
                        .avgNoiseLevelDba(0.0)
                        .minNoiseLevelDba(Double.MAX_VALUE)
                        .maxNoiseLevelDba(Double.MIN_VALUE)
                        .noiseClassDistribution(new HashMap<>())
                        .build());

        // Пересчёт средних (инкрементальное среднее)
        int newCount = tile.getMeasurementCount() + 1;
        double newAvg = tile.getAvgNoiseLevelDba()
                + (event.getNoiseLevelDba() - tile.getAvgNoiseLevelDba()) / newCount;

        tile.setMeasurementCount(newCount);
        tile.setAvgNoiseLevelDba(newAvg);
        tile.setMinNoiseLevelDba(Math.min(tile.getMinNoiseLevelDba(), event.getNoiseLevelDba()));
        tile.setMaxNoiseLevelDba(Math.max(tile.getMaxNoiseLevelDba(), event.getNoiseLevelDba()));

        // Обновить распределение классов
        Map<String, Integer> distribution = tile.getNoiseClassDistribution();
        if (distribution == null) distribution = new HashMap<>();
        distribution.merge(event.getNoiseClass(), 1, Integer::sum);
        tile.setNoiseClassDistribution(distribution);

        // Определить доминирующий класс
        tile.setDominantNoiseClass(
                distribution.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("unknown")
        );

        tile.setLastUpdated(Instant.now());
        tileRepository.save(tile);

        // Инвалидировать кэш для этой области
        invalidateCacheForTile(event.getLatitude(), event.getLongitude());

        log.debug("Updated tile {}: avg={}dBA, count={}", tileKey, newAvg, newCount);
    }

    /**
     * Вычисление ключа тайла из координат.
     * Простая сетка: делим координаты на TILE_SIZE_DEGREES.
     */
    private String calculateTileKey(double longitude, double latitude) {
        long x = Math.round(longitude / TILE_SIZE_DEGREES);
        long y = Math.round(latitude / TILE_SIZE_DEGREES);
        return x + "_" + y;
    }

    private void invalidateCacheForTile(double lat, double lng) {
        // Простая стратегия: удаляем кэш по паттерну (в продакшене — через Redis SCAN)
        String pattern = CACHE_PREFIX + "*";
        // Для MVP удаляем весь кэш тепловой карты
        // В продакшене: удалять только затронутые bounding box'ы
        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    private String buildCacheKey(MapDto.HeatmapRequest request) {
        return CACHE_PREFIX + String.format("%.4f_%.4f_%.4f_%.4f_%s_%s",
                request.getMinLat(), request.getMinLng(),
                request.getMaxLat(), request.getMaxLng(),
                request.getNoiseClassFilter(),
                request.getTimeFilter());
    }

    private MapDto.TileResponse toTileResponse(NoiseTile tile) {
        return MapDto.TileResponse.builder()
                .tileKey(tile.getTileKey())
                .latitude(tile.getCenter()[1])
                .longitude(tile.getCenter()[0])
                .avgNoiseLevelDba(tile.getAvgNoiseLevelDba())
                .measurementCount(tile.getMeasurementCount())
                .dominantNoiseClass(tile.getDominantNoiseClass())
                .noiseClassDistribution(tile.getNoiseClassDistribution())
                .build();
    }
}
