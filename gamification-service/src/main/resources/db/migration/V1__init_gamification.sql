-- V1__init_gamification.sql
-- Таблица определений ачивок
CREATE TABLE IF NOT EXISTS achievement_definitions (
    code          VARCHAR(50)  PRIMARY KEY,
    title         VARCHAR(100) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    points        INTEGER      NOT NULL,
    trigger_type  VARCHAR(50)  NOT NULL,
    trigger_value VARCHAR(50),
    icon_url      VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    DEFAULT NOW()
);

-- Таблица очков пользователей
CREATE TABLE IF NOT EXISTS user_scores (
    user_id              UUID    PRIMARY KEY,
    total_points         INTEGER NOT NULL DEFAULT 0,
    total_recordings     INTEGER NOT NULL DEFAULT 0,
    level                INTEGER NOT NULL DEFAULT 1,
    current_streak       INTEGER NOT NULL DEFAULT 0,
    last_recording_date  TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT NOW()
);

-- Таблица полученных ачивок
CREATE TABLE IF NOT EXISTS user_achievements (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL,
    achievement_code VARCHAR(50) NOT NULL REFERENCES achievement_definitions(code),
    unlocked_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_code)
);

-- Начальные данные ачивок
-- trigger_type: recording_count, noise_level_below, noise_level_above, time_of_day, streak_days
INSERT INTO achievement_definitions (code, title, description, points, trigger_type, trigger_value, icon_url) VALUES

-- By recording count
('first_recording',   'First Step',        'Make your first recording',     10,   'recording_count', '1',   '/icons/achievements/first-step.svg'),
('recordings_10',     'Activist',          'Make 10 recordings',        50,   'recording_count', '10',  '/icons/achievements/activist.svg'),
('recordings_50',     'Explorer',          'Make 50 recordings',        200,  'recording_count', '50',  '/icons/achievements/explorer.svg'),
('recordings_100',    'Expert',            'Make 100 recordings',       500,  'recording_count', '100', '/icons/achievements/expert.svg'),
('recordings_500',    'Legend',            'Make 500 recordings',       2000, 'recording_count', '500', '/icons/achievements/legend.svg'),

-- By noise level
('quiet_finder',      'Peace and Quiet',   'Find a spot quieter than 40 dBA',  100,  'noise_level_below', '40', '/icons/achievements/quiet.svg'),
('loud_discoverer',   'Hotspot',           'Find a spot louder than 85 dBA', 50,  'noise_level_above', '85', '/icons/achievements/loud.svg'),

-- By time
('night_owl',         'Night Watch',       'Recording between 23:00 and 5:00',  75,   'time_of_day', 'night',   '/icons/achievements/night-owl.svg'),
('early_bird',        'Early Bird',        'Recording between 5:00 and 7:00',   75,   'time_of_day', 'morning', '/icons/achievements/early-bird.svg'),

-- By streak
('streak_7',          'One Week Streak',   'Record for 7 days in a row',  150,  'streak_days', '7',  '/icons/achievements/streak-7.svg'),
('streak_30',         'One Month Streak',  'Record for 30 days in a row', 500,  'streak_days', '30', '/icons/achievements/streak-30.svg')

ON CONFLICT (code) DO NOTHING;
