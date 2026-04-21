package kz.noisemap.recordingservice.config;

import kz.noisemap.common.event.RabbitConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /**
     * Topic exchange — одно событие может уходить в несколько очередей.
     */
    @Bean
    public TopicExchange noiseMapExchange() {
        return new TopicExchange(RabbitConstants.EXCHANGE, true, false);
    }

    // === Очереди, которые слушают recording.created ===

    @Bean
    public Queue mlClassificationQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_ML_CLASSIFICATION).build();
    }

    @Bean
    public Queue moderationCheckQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_MODERATION_CHECK).build();
    }

    @Bean
    public Queue gamificationRecordingQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_GAMIFICATION_RECORDING).build();
    }

    // === Bindings: recording.created → 3 очереди ===

    @Bean
    public Binding mlClassificationBinding() {
        return BindingBuilder.bind(mlClassificationQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_RECORDING_CREATED);
    }

    @Bean
    public Binding moderationCheckBinding() {
        return BindingBuilder.bind(moderationCheckQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_RECORDING_CREATED);
    }

    @Bean
    public Binding gamificationRecordingBinding() {
        return BindingBuilder.bind(gamificationRecordingQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_RECORDING_CREATED);
    }

    // === Очереди, которые слушают classification.completed ===

    @Bean
    public Queue mappingUpdateQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_MAPPING_UPDATE).build();
    }

    @Bean
    public Queue statisticsUpdateQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_STATISTICS_UPDATE).build();
    }

    @Bean
    public Queue gamificationAchievementQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_GAMIFICATION_ACHIEVEMENT).build();
    }

    @Bean
    public Queue notificationNoiseAlertQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_NOTIFICATION_NOISE_ALERT).build();
    }

    // === Bindings: classification.completed → 4 очереди ===

    @Bean
    public Binding mappingUpdateBinding() {
        return BindingBuilder.bind(mappingUpdateQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_CLASSIFICATION_COMPLETED);
    }

    @Bean
    public Binding statisticsUpdateBinding() {
        return BindingBuilder.bind(statisticsUpdateQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_CLASSIFICATION_COMPLETED);
    }

    @Bean
    public Binding gamificationAchievementBinding() {
        return BindingBuilder.bind(gamificationAchievementQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_CLASSIFICATION_COMPLETED);
    }

    @Bean
    public Binding notificationNoiseAlertBinding() {
        return BindingBuilder.bind(notificationNoiseAlertQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_CLASSIFICATION_COMPLETED);
    }

    // === Очереди для achievement.unlocked и recording.flagged ===

    @Bean
    public Queue notificationAchievementQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_NOTIFICATION_ACHIEVEMENT).build();
    }

    @Bean
    public Queue notificationModeratorQueue() {
        return QueueBuilder.durable(RabbitConstants.Q_NOTIFICATION_MODERATOR).build();
    }

    @Bean
    public Binding notificationAchievementBinding() {
        return BindingBuilder.bind(notificationAchievementQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_ACHIEVEMENT_UNLOCKED);
    }

    @Bean
    public Binding notificationModeratorBinding() {
        return BindingBuilder.bind(notificationModeratorQueue())
                .to(noiseMapExchange())
                .with(RabbitConstants.RK_RECORDING_FLAGGED);
    }

    /**
     * JSON сериализация для RabbitMQ сообщений.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
