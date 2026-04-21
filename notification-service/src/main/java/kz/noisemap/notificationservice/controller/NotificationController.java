package kz.noisemap.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.notificationservice.dto.NotificationDto;
import kz.noisemap.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Уведомления", description = "Push-уведомления: ачивки, алерты шума (>85 дБА), модерация")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Мои уведомления",
               description = "Список уведомлений пользователя с пагинацией, отсортированных по дате (новые первыми)")
    public ResponseEntity<Page<NotificationDto.Response>> getMyNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Счётчик непрочитанных",
               description = "Количество непрочитанных уведомлений для бейджа в UI")
    public ResponseEntity<NotificationDto.UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new NotificationDto.UnreadCountResponse(count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Отметить уведомление прочитанным")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "ID уведомления") @PathVariable String id) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Отметить все уведомления прочитанными")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
