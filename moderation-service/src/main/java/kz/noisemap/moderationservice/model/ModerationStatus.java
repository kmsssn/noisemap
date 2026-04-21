package kz.noisemap.moderationservice.model;

public enum ModerationStatus {
    PENDING,     // ожидает проверки модератором
    APPROVED,    // одобрена модератором
    REJECTED     // отклонена модератором
}
