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

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Tag(name = "Модерация", description = "Управление качеством данных. Доступ: только MODERATOR и ADMIN.")
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/queue")
    @Operation(summary = "Очередь / история модерации",
            description = "Без status — PENDING. С status=APPROVED|REJECTED|ALL — соответствующая выборка.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список записей"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<Page<ModerationDto.QueueItem>> getQueue(
            @RequestHeader("X-User-Role") String role,
            @Parameter(description = "PENDING, APPROVED, REJECTED, ALL") @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        validateModeratorAccess(role);
        if (status == null || status.isBlank()) {
            return ResponseEntity.ok(moderationService.getPendingQueue(pageable));
        }
        return ResponseEntity.ok(moderationService.getQueue(status, pageable));
    }

    @PutMapping("/queue/{id}/review")
    @Operation(summary = "Принять решение по записи")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Решение принято"),
            @ApiResponse(responseCode = "400", description = "Невалидное решение"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<Void> reviewRecord(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Display-Name", required = false) String displayNameRaw,
            @Parameter(description = "ID записи") @PathVariable String id,
            @RequestBody ModerationDto.ReviewRequest request) {
        validateModeratorAccess(role);
        String reviewerName;
        try {
            reviewerName = displayNameRaw != null
                    ? URLDecoder.decode(displayNameRaw, StandardCharsets.UTF_8)
                    : "Moderator";
        } catch (Exception e) {
            reviewerName = displayNameRaw != null ? displayNameRaw : "Moderator";
        }
        moderationService.reviewRecord(id, userId, reviewerName, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика очереди модерации")
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