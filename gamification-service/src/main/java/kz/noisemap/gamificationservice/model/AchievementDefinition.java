package kz.noisemap.gamificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Registry of all achievements. Add new ones here.
 */
@Getter
@AllArgsConstructor
public enum AchievementDefinition {

    FIRST_RECORDING("first_recording", "First Step", "Make your first recording", 10),
    RECORDINGS_10("recordings_10", "Activist", "Make 10 recordings", 50),
    RECORDINGS_50("recordings_50", "Explorer", "Make 50 recordings", 200),
    RECORDINGS_100("recordings_100", "Expert", "Make 100 recordings", 500),
    RECORDINGS_500("recordings_500", "Legend", "Make 500 recordings", 2000),

    QUIET_FINDER("quiet_finder", "Peace & Quiet", "Find a place quieter than 40 dBA", 100),
    LOUD_DISCOVERER("loud_discoverer", "Hotspot Hunter", "Find a place louder than 85 dBA", 50),

    NIGHT_OWL("night_owl", "Night Owl", "Make a recording between 23:00 and 5:00", 75),
    EARLY_BIRD("early_bird", "Early Bird", "Make a recording between 5:00 and 7:00", 75),

    STREAK_7("streak_7", "Week Streak", "Record sound for 7 days in a row", 150),
    STREAK_30("streak_30", "Month Streak", "Record sound for 30 days in a row", 500);

    private final String code;
    private final String title;
    private final String description;
    private final int points;
}
