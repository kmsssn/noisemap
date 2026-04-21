package kz.noisemap.recordingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recordings")
@RequiredArgsConstructor
@Tag(name = "Записи шума", description = "Загрузка аудиозаписей, получение результатов классификации")
public class RecordingController {

    private final RecordingService recordingService;

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
            @RequestHeader("X-User-Id") UUID userId,
            @RequestPart("audio") MultipartFile audioFile,
            @RequestPart("metadata") @Valid RecordingDto.UploadRequest metadata) throws IOException {

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
