package kz.noisemap.commentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.noisemap.commentservice.dto.CommentDto;
import kz.noisemap.commentservice.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Комментарии", description = "Комментарии пользователей на карте шума")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Оставить комментарий на карте",
            description = "Пользователь оставляет комментарий с координатами. "
                    + "Опционально можно указать тип шума и уровень дБА.")
    @ApiResponse(responseCode = "201", description = "Комментарий создан")
    public ResponseEntity<CommentDto.Response> create(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Имя пользователя (URL-encoded)")
            @RequestHeader(value = "X-Display-Name", required = false) String displayNameRaw,
            @Valid @RequestBody CommentDto.CreateRequest request) {
        String displayName;
        try {
            displayName = displayNameRaw != null
                    ? java.net.URLDecoder.decode(displayNameRaw, java.nio.charset.StandardCharsets.UTF_8)
                    : "User";
        } catch (Exception e) {
            displayName = displayNameRaw != null ? displayNameRaw : "User";
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(userId, displayName, request));
    }

    @GetMapping
    @Operation(summary = "Все комментарии", description = "Список видимых комментариев с пагинацией (без удалённых и скрытых)")
    public ResponseEntity<Page<CommentDto.Response>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getAll(pageable));
    }

    @GetMapping("/moderation")
    @Operation(summary = "Список для модерации (Moderator/Admin)",
            description = "Все не удалённые комментарии, включая скрытые. "
                    + "Поле hidden показывает, скрыт ли комментарий. Требует роль MODERATOR или ADMIN.")
    public ResponseEntity<Page<CommentDto.Response>> getForModeration(
            @Parameter(description = "Роль пользователя") @RequestHeader("X-User-Role") String userRole,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getForModeration(userRole, pageable));
    }

    @GetMapping("/area")
    @Operation(summary = "Комментарии в области карты",
            description = "Получить комментарии в заданном bounding box — для отображения на карте")
    public ResponseEntity<List<CommentDto.Response>> getInArea(
            @RequestParam Double minLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLat,
            @RequestParam Double maxLng) {
        return ResponseEntity.ok(commentService.getInArea(minLat, minLng, maxLat, maxLng));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Комментарии рядом с точкой")
    public ResponseEntity<List<CommentDto.Response>> getNearby(
            @Parameter(description = "Широта", example = "43.238") @RequestParam Double lat,
            @Parameter(description = "Долгота", example = "76.945") @RequestParam Double lng,
            @Parameter(description = "Радиус в метрах", example = "500") @RequestParam(defaultValue = "500") Double radius) {
        return ResponseEntity.ok(commentService.getNearby(lat, lng, radius));
    }

    @GetMapping("/my")
    @Operation(summary = "Мои комментарии")
    public ResponseEntity<Page<CommentDto.Response>> getMyComments(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.getMyComments(userId, pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить комментарий",
            description = "Пользователь может удалить только свой. Модератор и Admin — любой. Необратимо (soft delete).")
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Роль пользователя") @RequestHeader("X-User-Role") String userRole,
            @PathVariable String id) {
        commentService.delete(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/hide")
    @Operation(summary = "Скрыть комментарий (Moderator/Admin)",
            description = "Скрывает комментарий с карты и из публичных списков. Обратимо — можно показать снова.")
    public ResponseEntity<CommentDto.Response> hide(
            @Parameter(description = "Роль пользователя") @RequestHeader("X-User-Role") String userRole,
            @PathVariable String id) {
        return ResponseEntity.ok(commentService.hide(id, userRole));
    }

    @PutMapping("/{id}/unhide")
    @Operation(summary = "Снова показать комментарий (Moderator/Admin)",
            description = "Возвращает ранее скрытый комментарий на карту и в публичные списки.")
    public ResponseEntity<CommentDto.Response> unhide(
            @Parameter(description = "Роль пользователя") @RequestHeader("X-User-Role") String userRole,
            @PathVariable String id) {
        return ResponseEntity.ok(commentService.unhide(id, userRole));
    }
}
