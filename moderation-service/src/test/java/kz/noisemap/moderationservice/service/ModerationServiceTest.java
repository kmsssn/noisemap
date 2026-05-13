package kz.noisemap.moderationservice.service;

import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.moderationservice.repository.ModerationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModerationService — unit tests")
class ModerationServiceTest {

    @Mock ModerationRepository moderationRepository;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks ModerationService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        // По умолчанию нет флагов за последний час
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(any(), any())).thenReturn(0L);
    }

    // === Координаты ===

    @ParameterizedTest(name = "lat={0}, lng={1} → VALID (не флагируется)")
    @CsvSource({
            "43.238, 76.945",    // центр Алматы
            "43.0,   76.0",      // минимум граница
            "43.9,   77.9",      // максимум граница
            "40.0,   76.0",      // ровно на границе
            "44.0,   78.0"       // ровно на границе
    })
    @DisplayName("Валидные координаты Алматы — не флагируются")
    void checkRecording_validAlmatyCoordinates_noFlag(double lat, double lng) {
        RecordingCreatedEvent event = buildEvent(lat, lng);

        service.checkRecording(event);

        // Не должен сохранять и публиковать событие
        verify(moderationRepository, never()).save(any());
        verifyNoInteractions(rabbitTemplate);
    }

    @ParameterizedTest(name = "lat={0}, lng={1} → out_of_bounds")
    @CsvSource({
            "55.751, 37.617",    // Москва
            "51.507, -0.127",    // Лондон
            "0.0,    0.0",       // ноль
            "39.9,   76.0",      // чуть за южной границей
            "44.1,   76.0",      // чуть за северной границей
            "43.0,   75.9",      // чуть за западной границей
            "43.0,   78.1"       // чуть за восточной границей
    })
    @DisplayName("Координаты за пределами Алматы → out_of_bounds")
    void checkRecording_outsideAlmaty_flaggedAsOutOfBounds(double lat, double lng) {
        RecordingCreatedEvent event = buildEvent(lat, lng);

        service.checkRecording(event);

        verify(moderationRepository).save(argThat(r ->
                "out_of_bounds".equals(r.getReason())
        ));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any());
    }

    // === Spam detection ===

    @Test
    @DisplayName("Spam: менее 5 флагов за час — не флагируется")
    void checkRecording_belowSpamThreshold_noFlag() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any()))
                .thenReturn(4L);

        service.checkRecording(buildEvent(43.238, 76.945));

        verify(moderationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Spam: ровно 5 флагов за час → spam_pattern")
    void checkRecording_atSpamThreshold_flagged() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any()))
                .thenReturn(5L);

        service.checkRecording(buildEvent(43.238, 76.945));

        verify(moderationRepository).save(argThat(r ->
                "spam_pattern".equals(r.getReason())
        ));
    }

    @Test
    @DisplayName("Spam: 10 флагов за час → spam_pattern")
    void checkRecording_exceedsSpamThreshold_flagged() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any()))
                .thenReturn(10L);

        service.checkRecording(buildEvent(43.238, 76.945));

        verify(moderationRepository).save(argThat(r ->
                "spam_pattern".equals(r.getReason())
        ));
    }

    @Test
    @DisplayName("out_of_bounds имеет приоритет над spam_pattern (ранний возврат)")
    void checkRecording_outOfBoundsAndSpam_flagsOnlyOutOfBounds() {
        // Юзер за пределами И спам
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any()))
                .thenReturn(10L);

        service.checkRecording(buildEvent(55.751, 37.617)); // Москва

        // save вызывается только один раз (out_of_bounds, не spam)
        verify(moderationRepository, times(1)).save(argThat(r ->
                "out_of_bounds".equals(r.getReason())
        ));
    }

    @Test
    @DisplayName("Spam detection: используется countByUserIdAndFlaggedAtAfter (с временным окном), не countByUserId")
    void checkRecording_usesTimeWindowedCount_notAllTimeCount() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any()))
                .thenReturn(0L);

        service.checkRecording(buildEvent(43.238, 76.945));

        // Должен использовать метод с временным окном
        verify(moderationRepository).countByUserIdAndFlaggedAtAfter(eq(userId), any(Instant.class));
        // Не должен использовать метод без временного окна
        verify(moderationRepository, never()).countByUserId(any());
    }

    // === Helpers ===

    private RecordingCreatedEvent buildEvent(double lat, double lng) {
        return RecordingCreatedEvent.builder()
                .recordingId("rec-" + UUID.randomUUID())
                .userId(userId)
                .latitude(lat)
                .longitude(lng)
                .deviceModel("Test Device")
                .calibrationOffset(0.0)
                .audioFileUrl("/data/audio/test.wav")
                .recordedAt(Instant.now())
                .publishedAt(Instant.now())
                .build();
    }
}
