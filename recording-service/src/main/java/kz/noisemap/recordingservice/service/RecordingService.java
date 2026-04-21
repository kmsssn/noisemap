package kz.noisemap.recordingservice.service;

import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.recordingservice.dto.RecordingDto;
import kz.noisemap.recordingservice.model.Recording;
import kz.noisemap.recordingservice.model.RecordingStatus;
import kz.noisemap.recordingservice.repository.RecordingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RecordingRepository recordingRepository;
    private final FileStorageService fileStorageService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Основной метод: принимает аудио, сохраняет, публикует событие.
     * Клиент получает 202 Accepted и не ждёт ML обработки.
     */
    public RecordingDto.Response uploadRecording(
            UUID userId,
            MultipartFile audioFile,
            RecordingDto.UploadRequest request) throws IOException {

        // 1. Валидация файла
        validateAudioFile(audioFile);

        // 2. Сохранение файла
        String fileUrl = fileStorageService.store(audioFile, userId);

        // 3. Сохранение метаданных в MongoDB
        Recording recording = Recording.builder()
                .userId(userId)
                .audioFileUrl(fileUrl)
                .location(new double[]{request.getLongitude(), request.getLatitude()}) // GeoJSON: [lng, lat]
                .deviceModel(request.getDeviceModel())
                .status(RecordingStatus.PENDING)
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now())
                .build();

        recording = recordingRepository.save(recording);

        // 4. Публикация события в RabbitMQ → ML, Moderation, Gamification
        publishRecordingCreated(recording);

        log.info("Recording uploaded: id={}, userId={}, location=[{}, {}]",
                recording.getId(), userId, request.getLatitude(), request.getLongitude());

        return toResponse(recording);
    }

    public Page<RecordingDto.Response> getMyRecordings(UUID userId, Pageable pageable) {
        return recordingRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    public RecordingDto.Response getById(String recordingId) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found: " + recordingId));
        return toResponse(recording);
    }

    public long countByUser(UUID userId) {
        return recordingRepository.countByUserId(userId);
    }

    /**
     * Вызывается из event listener при получении classification.completed.
     * Обновляет запись результатами ML классификации.
     */
    public void updateClassification(String recordingId, Double noiseLevelDba,
                                      String noiseClass, Double confidenceScore) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found: " + recordingId));

        recording.setNoiseLevelDba(noiseLevelDba);
        recording.setNoiseClass(noiseClass);
        recording.setConfidenceScore(confidenceScore);
        recording.setStatus(RecordingStatus.CLASSIFIED);

        recordingRepository.save(recording);
        log.info("Recording classified: id={}, class={}, dba={}",
                recordingId, noiseClass, noiseLevelDba);
    }

    private void publishRecordingCreated(Recording recording) {
        RecordingCreatedEvent event = RecordingCreatedEvent.builder()
                .recordingId(recording.getId())
                .userId(recording.getUserId())
                .audioFileUrl(recording.getAudioFileUrl())
                .latitude(recording.getLocation()[1])  // location = [lng, lat]
                .longitude(recording.getLocation()[0])
                .deviceModel(recording.getDeviceModel())
                .calibrationOffset(recording.getCalibrationOffset())
                .recordedAt(recording.getRecordedAt())
                .publishedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitConstants.EXCHANGE,
                RabbitConstants.RK_RECORDING_CREATED,
                event
        );

        log.debug("Published recording.created event for recording: {}", recording.getId());
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }

        // Максимум 10 МБ
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Audio file size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("audio/") && !contentType.equals("application/octet-stream"))) {
            throw new IllegalArgumentException("Invalid file type. Audio files only.");
        }
    }

    private RecordingDto.Response toResponse(Recording recording) {
        return RecordingDto.Response.builder()
                .id(recording.getId())
                .latitude(recording.getLocation()[1])
                .longitude(recording.getLocation()[0])
                .status(recording.getStatus().name())
                .noiseLevelDba(recording.getNoiseLevelDba())
                .noiseClass(recording.getNoiseClass())
                .confidenceScore(recording.getConfidenceScore())
                .recordedAt(recording.getRecordedAt())
                .createdAt(recording.getCreatedAt())
                .build();
    }
}
