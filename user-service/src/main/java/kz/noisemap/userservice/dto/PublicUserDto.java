package kz.noisemap.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Публичные данные пользователя — только то что можно показать другим.
 * Используется в лидерборде геймификации.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserDto {
    private UUID id;
    private String displayName;
    private Integer level; // если нужно
}
