package kz.noisemap.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.userservice.dto.AdminDto;
import kz.noisemap.userservice.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Администрирование", description = "Управление пользователями. Доступ: только ADMIN.")
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Список всех пользователей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public ResponseEntity<Page<AdminDto.UserResponse>> getAllUsers(
            @RequestHeader("X-User-Role") String role,
            @PageableDefault(size = 20) Pageable pageable) {
        validateAdminAccess(role);
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить пользователя по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Данные пользователя"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<AdminDto.UserResponse> getUser(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID userId) {
        validateAdminAccess(role);
        return ResponseEntity.ok(adminService.getUser(userId));
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Сменить роль пользователя",
            description = "Допустимые значения: USER, MODERATOR, ADMIN. Нельзя изменить свою роль.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль изменена"),
            @ApiResponse(responseCode = "400", description = "Недопустимая роль или попытка изменить свою роль"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<AdminDto.UserResponse> changeRole(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID userId,
            @RequestBody AdminDto.ChangeRoleRequest request) {
        validateAdminAccess(role);
        return ResponseEntity.ok(adminService.changeRole(userId, adminId, request));
    }

    @PutMapping("/{userId}/active")
    @Operation(summary = "Заблокировать / разблокировать пользователя",
            description = "active=false — блокирует вход. Нельзя заблокировать себя.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус изменён"),
            @ApiResponse(responseCode = "400", description = "Попытка заблокировать себя"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<AdminDto.UserResponse> setActive(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID userId,
            @RequestBody AdminDto.SetActiveRequest request) {
        validateAdminAccess(role);
        return ResponseEntity.ok(adminService.setActive(userId, adminId, request));
    }

    private void validateAdminAccess(String role) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("Access denied. Admin role required.");
        }
    }
}