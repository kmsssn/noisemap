package kz.noisemap.userservice.service;

import kz.noisemap.userservice.dto.PublicUserDto;
import kz.noisemap.userservice.dto.UserDto;
import kz.noisemap.userservice.model.User;
import kz.noisemap.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto.Response getProfile(UUID userId) {
        return toResponse(findById(userId));
    }

    @Transactional
    public UserDto.Response updateProfile(UUID userId, UserDto.UpdateRequest request) {
        User user = findById(userId);
        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());
        return toResponse(userRepository.save(user));
    }

    /**
     * Публичная информация — только displayName.
     * Используется другими сервисами (gamification лидерборд).
     */
    public PublicUserDto getPublicInfo(UUID userId) {
        User user = findById(userId);
        return PublicUserDto.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .build();
    }

    private User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private UserDto.Response toResponse(User user) {
        return UserDto.Response.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .language(user.getLanguage())
                .createdAt(user.getCreatedAt())
                .build();
    }
}