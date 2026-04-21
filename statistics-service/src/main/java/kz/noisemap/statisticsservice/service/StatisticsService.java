package kz.noisemap.statisticsservice.service;

import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.statisticsservice.dto.StatsDto;
import kz.noisemap.statisticsservice.model.NoiseMeasurement;
import kz.noisemap.statisticsservice.repository.NoiseMeasurementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final NoiseMeasurementRepository measurementRepository;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    /**
     * Сохранить результат классификации для аналитики.
     */
    public void saveMeasurement(ClassificationCompletedEvent event) {
        ZonedDateTime zdt = event.getRecordedAt().atZone(ZoneId.of("Asia/Almaty"));

        NoiseMeasurement measurement = NoiseMeasurement.builder()
                .recordingId(event.getRecordingId())
                .userId(event.getUserId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .noiseLevelDba(event.getNoiseLevelDba())
                .noiseClass(event.getNoiseClass())
                .confidenceScore(event.getConfidenceScore())
                .recordedAt(event.getRecordedAt())
                .classifiedAt(event.getClassifiedAt())
                .hourOfDay(zdt.getHour())
                .dayOfWeek(zdt.getDayOfWeek().getValue())
                .month(zdt.getMonthValue())
                .build();

        measurementRepository.save(measurement);

        // Инвалидировать кэш статистики
        redisTemplate.delete("stats:city");
    }

    /**
     * Общая статистика по городу.
     */
    public StatsDto.CityStatsResponse getCityStats() {
        @SuppressWarnings("unchecked")
        StatsDto.CityStatsResponse cached =
                (StatsDto.CityStatsResponse) redisTemplate.opsForValue().get("stats:city");
        if (cached != null) return cached;

        List<NoiseMeasurement> all = measurementRepository.findAll();

        if (all.isEmpty()) {
            return StatsDto.CityStatsResponse.builder()
                    .avgNoiseLevelDba(0.0)
                    .totalMeasurements(0L)
                    .totalContributors(0L)
                    .build();
        }

        DoubleSummaryStatistics dbaStats = all.stream()
                .filter(m -> m.getNoiseLevelDba() != null)
                .mapToDouble(NoiseMeasurement::getNoiseLevelDba)
                .summaryStatistics();

        Map<String, Long> byClass = all.stream()
                .filter(m -> m.getNoiseClass() != null)
                .collect(Collectors.groupingBy(NoiseMeasurement::getNoiseClass, Collectors.counting()));

        long uniqueUsers = all.stream()
                .map(NoiseMeasurement::getUserId)
                .distinct()
                .count();

        // Почасовые средние
        List<StatsDto.HourlyAverage> hourly = all.stream()
                .filter(m -> m.getHourOfDay() != null && m.getNoiseLevelDba() != null)
                .collect(Collectors.groupingBy(NoiseMeasurement::getHourOfDay))
                .entrySet().stream()
                .map(e -> StatsDto.HourlyAverage.builder()
                        .hour(e.getKey())
                        .avgDba(e.getValue().stream()
                                .mapToDouble(NoiseMeasurement::getNoiseLevelDba)
                                .average().orElse(0))
                        .measurementCount((long) e.getValue().size())
                        .build())
                .sorted(Comparator.comparingInt(StatsDto.HourlyAverage::getHour))
                .collect(Collectors.toList());

        StatsDto.CityStatsResponse response = StatsDto.CityStatsResponse.builder()
                .avgNoiseLevelDba(dbaStats.getAverage())
                .maxNoiseLevelDba(dbaStats.getMax())
                .minNoiseLevelDba(dbaStats.getMin())
                .totalMeasurements((long) all.size())
                .totalContributors(uniqueUsers)
                .measurementsByNoiseClass(byClass)
                .hourlyAverages(hourly)
                .build();

        redisTemplate.opsForValue().set("stats:city", response, CACHE_TTL);
        return response;
    }

    /**
     * Персональная статистика пользователя.
     */
    public StatsDto.UserStatsResponse getUserStats(UUID userId) {
        List<NoiseMeasurement> userMeasurements = measurementRepository.findByUserId(userId);

        if (userMeasurements.isEmpty()) {
            return StatsDto.UserStatsResponse.builder()
                    .totalRecordings(0L)
                    .avgExposureDba(0.0)
                    .recommendation("Сделайте первую запись, чтобы увидеть статистику!")
                    .build();
        }

        DoubleSummaryStatistics dbaStats = userMeasurements.stream()
                .filter(m -> m.getNoiseLevelDba() != null)
                .mapToDouble(NoiseMeasurement::getNoiseLevelDba)
                .summaryStatistics();

        Map<String, Long> byClass = userMeasurements.stream()
                .filter(m -> m.getNoiseClass() != null)
                .collect(Collectors.groupingBy(NoiseMeasurement::getNoiseClass, Collectors.counting()));

        String recommendation = generateRecommendation(dbaStats.getAverage());

        return StatsDto.UserStatsResponse.builder()
                .totalRecordings((long) userMeasurements.size())
                .avgExposureDba(dbaStats.getAverage())
                .maxExposureDba(dbaStats.getMax())
                .recordingsByNoiseClass(byClass)
                .recommendation(recommendation)
                .build();
    }

    private String generateRecommendation(double avgDba) {
        if (avgDba < 55) {
            return "Ваше окружение в пределах нормы ВОЗ. Отличная акустическая обстановка!";
        } else if (avgDba < 70) {
            return "Умеренный уровень шума. Рекомендуется делать перерывы в тихих местах.";
        } else if (avgDba < 85) {
            return "Повышенный уровень шума. Длительное воздействие может влиять на здоровье.";
        } else {
            return "Опасный уровень шума! Рекомендуется использовать средства защиты слуха.";
        }
    }
}
