CREATE TABLE athlete_profile (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    birth_year INTEGER,
    height_cm INTEGER,
    weight_kg NUMERIC(5, 2),
    maximum_heart_rate INTEGER,
    resting_heart_rate INTEGER,
    heart_rate_threshold INTEGER,
    power_threshold INTEGER,
    usual_weekly_volume_km NUMERIC(6, 2),
    available_days VARCHAR(1000),
    maximum_session_duration_minutes INTEGER,
    available_terrains VARCHAR(1000),
    constraints_and_injuries VARCHAR(2000),
    training_preferences VARCHAR(2000),
    notes VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE goal (
    id UUID PRIMARY KEY,
    athlete_profile_id UUID NOT NULL REFERENCES athlete_profile(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    event_date DATE NOT NULL,
    distance NUMERIC(8, 3),
    distance_unit VARCHAR(20),
    goal_type VARCHAR(30) NOT NULL,
    official_url VARCHAR(2048),
    priority INTEGER NOT NULL,
    target_time_seconds INTEGER,
    target_elevation_gain_meters INTEGER,
    notes VARCHAR(4000),
    status VARCHAR(20) NOT NULL,
    primary_goal BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_goal_profile_date ON goal (athlete_profile_id, event_date);
CREATE UNIQUE INDEX uk_goal_primary_active ON goal (athlete_profile_id)
    WHERE primary_goal = TRUE AND archived = FALSE;
