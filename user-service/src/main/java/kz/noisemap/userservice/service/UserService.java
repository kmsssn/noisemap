package kz.noisemap.userservice.service;

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
        User user = findById(userId);
        return toResponse(user);
    }

    @Transactional
    public UserDto.Response updateProfile(UUID userId, UserDto.UpdateRequest request) {
        User user = findById(userId);

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getLanguage() != null) {
            user.setLanguage(request.getLanguage());
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserDto.Response updateDevice(UUID userId, UserDto.DeviceUpdateRequest request) {
        User user = findById(userId);

        if (request.getDeviceModel() != null) {
            user.setDeviceModel(request.getDeviceModel());
        }
        if (request.getCalibrationOffset() != null) {
            user.setCalibrationOffset(request.getCalibrationOffset());
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Вызывается другими сервисами через REST для получения калибровки устройства.
     */
    public Double getCalibrationOffset(UUID userId) {
        User user = findById(userId);
        return user.getCalibrationOffset();
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
                .deviceModel(user.getDeviceModel())
                .calibrationOffset(user.getCalibrationOffset())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
