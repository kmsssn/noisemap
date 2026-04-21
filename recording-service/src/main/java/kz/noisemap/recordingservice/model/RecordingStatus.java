package kz.noisemap.recordingservice.model;

public enum RecordingStatus {
    PENDING,        // загружена, ждёт ML обработки
    CLASSIFIED,     // ML обработал
    FLAGGED,        // помечена модерацией как подозрительная
    REJECTED        // отклонена модерацией
}
