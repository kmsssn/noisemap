package kz.noisemap.recordingservice.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ от ML-сервиса одногруппника (FastAPI POST /predict).
 * Пример ответа:
 * {
 *   "label": "transport",
 *   "confidence": 0.977,
 *   "chunks_count": 3,
 *   "duration_seconds": 14.711,
 *   "noise_level_dba": 72.3
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlClassificationResponse {
    private String label;
    private Double confidence;
    private Integer chunksCount;
    private Double durationSeconds;

    private Double noiseLevelDba;
}
