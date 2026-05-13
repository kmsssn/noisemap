package kz.noisemap.userservice.service;

import kz.noisemap.userservice.dto.DeviceCalibrationDto;
import kz.noisemap.userservice.model.DeviceCalibration;
import kz.noisemap.userservice.repository.DeviceCalibrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceCalibrationService — unit tests")
class DeviceCalibrationServiceTest {

    @Mock DeviceCalibrationRepository repository;

    @InjectMocks DeviceCalibrationService service;

    @ParameterizedTest(name = "model={0} → manufacturer={1}")
    @CsvSource({
            "iPhone 13 Pro,Apple",
            "IPHONE 15,Apple",
            "iPad Pro,Apple",
            "Galaxy S24 Ultra,Samsung",
            "samsung galaxy s23,Samsung",
            "Pixel 8 Pro,Google",
            "Redmi Note 12,Xiaomi",
            "POCO X5 Pro,Xiaomi",
            "Xiaomi Mi 13,Xiaomi",
            "Huawei P60,Huawei",
            "Honor 90,Huawei",
            "OnePlus 11,OnePlus",
            "Realme GT Neo 5,Realme",
            "Oppo Reno 10,Oppo",
            "Vivo V29,Vivo",
            "Some Random Device,Unknown",
            "Mi 13,Unknown"  // одиночное 'Mi' не распознаётся — намеренно
    })
    @DisplayName("guessManufacturer: правильно определяет производителя")
    void guessManufacturer(String model, String expectedManufacturer) {
        when(repository.findByModelIgnoreCase(model)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            DeviceCalibration saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        service.getCalibrationOrCreate(model);

        verify(repository).save(argThat(d ->
                expectedManufacturer.equals(d.getManufacturer())
        ));
    }


    @Test
    @DisplayName("Известное устройство: возвращает из справочника, инкрементирует счётчик")
    void getCalibrationOrCreate_knownDevice_returnsFromRegistry() {
        DeviceCalibration existing = DeviceCalibration.builder()
                .id(UUID.randomUUID())
                .manufacturer("Apple")
                .model("iPhone 13 Pro")
                .calibrationOffsetDb(-2.5)
                .verified(true)
                .source("research")
                .sampleCount(100)
                .build();

        when(repository.findByModelIgnoreCase("iPhone 13 Pro")).thenReturn(Optional.of(existing));

        DeviceCalibrationDto.CalibrationResult result =
                service.getCalibrationOrCreate("iPhone 13 Pro");

        assertThat(result.getModel()).isEqualTo("iPhone 13 Pro");
        assertThat(result.getCalibrationOffsetDb()).isEqualTo(-2.5);
        assertThat(result.getVerified()).isTrue();

        // НЕ должен создавать новую запись
        verify(repository, never()).save(any());
        // Должен инкрементировать счётчик
        verify(repository).incrementSampleCount(existing.getId());
    }

    @Test
    @DisplayName("Неизвестное устройство: создаёт новую запись с offset=0, verified=false")
    void getCalibrationOrCreate_unknownDevice_createsAutoEntry() {
        when(repository.findByModelIgnoreCase("MyCustomPhone XYZ")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            DeviceCalibration d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DeviceCalibrationDto.CalibrationResult result =
                service.getCalibrationOrCreate("MyCustomPhone XYZ");

        assertThat(result.getCalibrationOffsetDb()).isEqualTo(0.0);
        assertThat(result.getVerified()).isFalse();

        verify(repository).save(argThat(d ->
                "MyCustomPhone XYZ".equals(d.getModel()) &&
                        d.getCalibrationOffsetDb() == 0.0 &&
                        !d.getVerified() &&
                        "auto".equals(d.getSource())
        ));
    }

    @Test
    @DisplayName("Null deviceModel: возвращает offset=0.0 без обращения к БД")
    void getCalibrationOrCreate_nullModel_returnsZero() {
        DeviceCalibrationDto.CalibrationResult result = service.getCalibrationOrCreate(null);

        assertThat(result.getCalibrationOffsetDb()).isEqualTo(0.0);
        assertThat(result.getVerified()).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Пустой deviceModel: возвращает offset=0.0 без обращения к БД")
    void getCalibrationOrCreate_blankModel_returnsZero() {
        DeviceCalibrationDto.CalibrationResult result = service.getCalibrationOrCreate("   ");

        assertThat(result.getCalibrationOffsetDb()).isEqualTo(0.0);
        verifyNoInteractions(repository);
    }

    // === create ===

    @Test
    @DisplayName("create: дублирующая модель бросает исключение")
    void create_duplicateModel_throwsException() {
        when(repository.existsByModelIgnoreCase("iPhone 13 Pro")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                DeviceCalibrationDto.CreateRequest.builder()
                        .manufacturer("Apple")
                        .model("iPhone 13 Pro")
                        .calibrationOffsetDb(-2.5)
                        .build()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("update: несуществующий id бросает исключение")
    void update_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id,
                DeviceCalibrationDto.UpdateRequest.builder()
                        .calibrationOffsetDb(-3.0)
                        .build()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}