package kz.noisemap.recordingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.recordingservice.dto.RecordingDto;
import kz.noisemap.recordingservice.service.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recordings")
@RequiredArgsConstructor
@Tag(name = "Записи шума", description = "Загрузка аудиозаписей, получение результатов классификации")
public class RecordingController {

    private final RecordingService recordingService;

    /**
     * Загрузка записи. Используем плоские @RequestParam вместо @ModelAttribute с DTO,
     * чтобы Swagger UI рисовал отдельные form-fields для каждого параметра.
     * SpringDoc/Swagger UI не умеет правильно показывать @ModelAttribute как плоские поля
     * в multipart-запросе — рендерит ссылку на объект, и Swagger UI выдаёт там JSON-ввод.
     *
     * С @RequestParam форма получается ровно та, что ожидается:
     *   audio (file), latitude, longitude, deviceModel, recordedAt — каждое отдельным полем.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить аудиозапись шума",
               description = "Принимает аудиофайл + метаданные (координаты, устройство). "
                       + "Возвращает 202 Accepted — ML классификация выполняется асинхронно через RabbitMQ. "
                       + "Максимальный размер файла: 10 МБ.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Запись принята, классификация в процессе"),
            @ApiResponse(responseCode = "400", description = "Невалидный файл или координаты")
    })
    public ResponseEntity<RecordingDto.Response> upload(
            @Parameter(description = "UUID пользователя", required = true)
                @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Аудиофайл (mp3/wav/m4a, до 10 МБ)", required = true)
                @RequestParam("audio") MultipartFile audioFile,
            @Parameter(description = "Широта (-90..90)", example = "43.238", required = true)
                @RequestParam("latitude") Double latitude,
            @Parameter(description = "Долгота (-180..180)", example = "76.945", required = true)
                @RequestParam("longitude") Double longitude,
            @Parameter(description = "Модель устройства", example = "Samsung Galaxy S24")
                @RequestParam(value = "deviceModel", required = false) String deviceModel,
            @Parameter(description = "Когда была сделана запись (ISO-8601 UTC)",
                       example = "2026-05-09T10:00:00Z")
                @RequestParam(value = "recordedAt", required = false) Instant recordedAt
    ) throws IOException {

        // Базовая валидация координат — раньше делалась через @Min/@Max в DTO
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be in [-90, 90]");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be in [-180, 180]");
        }

        RecordingDto.UploadRequest metadata = RecordingDto.UploadRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .deviceModel(deviceModel)
                .recordedAt(recordedAt)
                .build();

        RecordingDto.Response response = recordingService.uploadRecording(userId, audioFile, metadata);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Мои записи", description = "Список записей текущего пользователя с пагинацией")
    public ResponseEntity<Page<RecordingDto.Response>> getMyRecordings(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recordingService.getMyRecordings(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Запись по ID", description = "Получить одну запись с результатом классификации")
    public ResponseEntity<RecordingDto.Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(recordingService.getById(id));
    }

    @GetMapping("/my/count")
    @Operation(summary = "Количество моих записей")
    public ResponseEntity<Long> getMyCount(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(recordingService.countByUser(userId));
    }
}