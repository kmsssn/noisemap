package kz.noisemap.userservice.service;

import kz.noisemap.userservice.dto.AdminDto;
import kz.noisemap.userservice.model.Role;
import kz.noisemap.userservice.model.User;
import kz.noisemap.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public Page<AdminDto.UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminResponse);
    }

    public AdminDto.UserResponse getUser(UUID userId) {
        return toAdminResponse(findById(userId));
    }

    @Transactional
    public AdminDto.UserResponse changeRole(UUID targetUserId, UUID adminId, AdminDto.ChangeRoleRequest request) {
        if (targetUserId.equals(adminId)) {
            throw new IllegalArgumentException("Admin cannot change their own role");
        }
        Role newRole;
        try {
            newRole = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Allowed values: USER, MODERATOR, ADMIN");
        }
        User user = findById(targetUserId);
        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        log.info("Role changed: userId={}, {} -> {}, by adminId={}", targetUserId, oldRole, newRole, adminId);
        return toAdminResponse(user);
    }

    @Transactional
    public AdminDto.UserResponse setActive(UUID targetUserId, UUID adminId, AdminDto.SetActiveRequest request) {
        if (targetUserId.equals(adminId)) {
            throw new IllegalArgumentException("Admin cannot ban themselves");
        }
        User user = findById(targetUserId);

        if (Boolean.TRUE.equals(request.getActive())) {
            // Разблокировка — очищаем все поля бана
            user.setActive(true);
            user.setBlockReason(null);
            user.setBlockedAt(null);
            user.setBlockedUntil(null);
            userRepository.save(user);
            log.info("User unbanned: userId={}, by adminId={}", targetUserId, adminId);
        } else {
            // Блокировка — сохраняем причину, время и срок
            user.setActive(false);
            user.setBlockReason(request.getReason());
            user.setBlockedAt(Instant.now());

            Integer hours = request.getDurationHours();
            if (hours != null && hours > 0) {
                Instant until = Instant.now().plus(hours, ChronoUnit.HOURS);
                user.setBlockedUntil(until);
                log.info("User banned until {}: userId={}, reason='{}', by adminId={}",
                        until, targetUserId, request.getReason(), adminId);
            } else {
                user.setBlockedUntil(null); // навсегда
                log.info("User banned permanently: userId={}, reason='{}', by adminId={}",
                        targetUserId, request.getReason(), adminId);
            }
            userRepository.save(user);
        }
        return toAdminResponse(user);
    }

    private User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private AdminDto.UserResponse toAdminResponse(User user) {
        return AdminDto.UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .language(user.getLanguage())
                .active(user.getActive())
                .blockReason(user.getBlockReason())
                .blockedAt(user.getBlockedAt())
                .blockedUntil(user.getBlockedUntil())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
