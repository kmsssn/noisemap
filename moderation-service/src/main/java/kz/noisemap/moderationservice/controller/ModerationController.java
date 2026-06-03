package kz.noisemap.moderationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.moderationservice.dto.ModerationDto;
import kz.noisemap.moderationservice.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Tag(name = "Модерация", description = "Управление качеством данных. "
        + "Автоматическое обнаружение аномалий + ручная модерация. "
        + "Доступ: только MODERATOR и ADMIN.")
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/queue")
    @Operation(summary = "Очередь / история модерации",
            description = "Без параметра status — записи в ожидании (PENDING). "
                    + "С параметром status=APPROVED|REJECTED|ALL — соответствующая выборка (история). "
                    + "Причины флага: out_of_bounds, spam_pattern.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список записей"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (нужен MODERATOR/ADMIN)")
    })
    public ResponseEntity<Page<ModerationDto.QueueItem>> getQueue(
            @RequestHeader("X-User-Role") String role,
            @Parameter(description = "Статус: PENDING, APPROVED, REJECTED, ALL. По умолчанию PENDING.")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        validateModeratorAccess(role);
        // Обратная совместимость: без параметра — старое поведение (только PENDING)
        if (status == null || status.isBlank()) {
            return ResponseEntity.ok(moderationService.getPendingQueue(pageable));
        }
        return ResponseEntity.ok(moderationService.getQueue(status, pageable));
    }

    @PutMapping("/queue/{id}/review")
    @Operation(summary = "Принять решение по записи",
            description = "Модератор одобряет (approve) или отклоняет (reject) запись. "
                    + "Можно добавить комментарий с обоснованием.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Решение принято"),
            @ApiResponse(responseCode = "400", description = "Невалидное решение (допустимо: approve, reject)"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<Void> reviewRecord(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @Parameter(description = "ID записи в очереди модерации") @PathVariable String id,
            @RequestBody ModerationDto.ReviewRequest request) {
        validateModeratorAccess(role);
        moderationService.reviewRecord(id, userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика очереди модерации",
            description = "Количество записей в ожидании проверки")
    public ResponseEntity<ModerationDto.QueueStats> getStats(
            @RequestHeader("X-User-Role") String role) {
        validateModeratorAccess(role);
        return ResponseEntity.ok(moderationService.getQueueStats());
    }

    private void validateModeratorAccess(String role) {
        if (!"MODERATOR".equals(role) && !"ADMIN".equals(role)) {
            throw new SecurityException("Access denied. Moderator or Admin role required.");
        }
    }
}
