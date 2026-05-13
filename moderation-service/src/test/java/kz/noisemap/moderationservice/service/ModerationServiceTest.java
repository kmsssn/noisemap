package kz.noisemap.moderationservice.service;

import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.common.event.RecordingFlaggedEvent;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    }

    // === Координаты ===

    @ParameterizedTest(name = "lat={0}, lng={1} → VALID (не флагируется)")
    @CsvSource({
            "43.238, 76.945",
            "43.0,   76.0",
            "43.9,   77.9",
            "40.0,   76.0",
            "44.0,   78.0"
    })
    @DisplayName("Валидные координаты Алматы — не флагируются")
    void checkRecording_validAlmatyCoordinates_noFlag(double lat, double lng) {
        // Координаты валидные → доходим до spam check → нужен stub
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(0L);

        service.checkRecording(buildEvent(lat, lng));

        verify(moderationRepository, never()).save(any());
        verifyNoInteractions(rabbitTemplate);
    }

    @ParameterizedTest(name = "lat={0}, lng={1} → out_of_bounds")
    @CsvSource({
            "55.751, 37.617",
            "51.507, -0.127",
            "0.0,    0.0",
            "39.9,   76.0",
            "44.1,   76.0",
            "43.0,   75.9",
            "43.0,   78.1"
    })
    @DisplayName("Координаты за пределами Алматы → out_of_bounds")
    void checkRecording_outsideAlmaty_flaggedAsOutOfBounds(double lat, double lng) {
        service.checkRecording(buildEvent(lat, lng));

        verify(moderationRepository).save(argThat(r ->
                "out_of_bounds".equals(r.getReason())
        ));
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(RecordingFlaggedEvent.class)
        );
    }

    @Test
    @DisplayName("Spam: менее 5 флагов за час — не флагируется")
    void checkRecording_belowSpamThreshold_noFlag() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(4L);

        service.checkRecording(buildEvent(43.238, 76.945));

        verify(moderationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Spam: ровно 5 флагов за час → spam_pattern")
    void checkRecording_atSpamThreshold_flagged() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(5L);

        service.checkRecording(buildEvent(43.238, 76.945));

        verify(moderationRepository).save(argThat(r ->
                "spam_pattern".equals(r.getReason())
        ));
    }

    @Test
    @DisplayName("Spam: 10 флагов за час → spam_pattern")
    void checkRecording_exceedsSpamThreshold_flagged() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(10L);

        service.checkRecording(buildEvent(43.238, 76.945));

        verify(moderationRepository).save(argThat(r ->
                "spam_pattern".equals(r.getReason())
        ));
    }

    @Test
    @DisplayName("out_of_bounds имеет приоритет над spam_pattern (ранний возврат)")
    void checkRecording_outOfBoundsAndSpam_flagsOnlyOutOfBounds() {
        service.checkRecording(buildEvent(55.751, 37.617)); // Москва

        verify(moderationRepository, times(1)).save(argThat(r ->
                "out_of_bounds".equals(r.getReason())
        ));
    }

    @Test
    @DisplayName("Spam detection: используется countByUserIdAndFlaggedAtAfter (с временным окном)")
    void checkRecording_usesTimeWindowedCount_notAllTimeCount() {
        when(moderationRepository.countByUserIdAndFlaggedAtAfter(eq(userId), any(Instant.class)))
                .thenReturn(0L);

        service.checkRecording(buildEvent(43.238, 76.945));

        verify(moderationRepository).countByUserIdAndFlaggedAtAfter(eq(userId), any(Instant.class));
        verify(moderationRepository, never()).countByUserId(any());
    }


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