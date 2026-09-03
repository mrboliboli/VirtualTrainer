ALTER TABLE activity ADD COLUMN fit_decode_status VARCHAR(30) NOT NULL DEFAULT 'FIT_INDISPONIBLE';
UPDATE activity SET fit_decode_status='A_DECODER' WHERE original_fit IS NOT NULL;
ALTER TABLE activity ALTER COLUMN fit_decode_status SET DEFAULT 'A_DECODER';
ALTER TABLE activity ADD COLUMN fit_decode_error VARCHAR(500);
ALTER TABLE activity ADD COLUMN fit_decoder_version VARCHAR(30);
ALTER TABLE activity ADD COLUMN fit_decoded_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE activity_fit_summary (
    activity_id UUID PRIMARY KEY REFERENCES activity(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE,
    sport VARCHAR(50), sub_sport VARCHAR(50),
    distance_meters DOUBLE PRECISION, elapsed_seconds DOUBLE PRECISION, active_seconds DOUBLE PRECISION,
    average_speed_mps DOUBLE PRECISION, maximum_speed_mps DOUBLE PRECISION,
    average_heart_rate SMALLINT, maximum_heart_rate SMALLINT,
    average_cadence SMALLINT, maximum_cadence SMALLINT,
    average_power INTEGER, maximum_power INTEGER, normalized_power INTEGER,
    calories INTEGER, ascent_meters INTEGER, descent_meters INTEGER,
    aerobic_training_effect DOUBLE PRECISION, anaerobic_training_effect DOUBLE PRECISION,
    training_load DOUBLE PRECISION,
    average_vertical_oscillation_mm DOUBLE PRECISION,
    average_vertical_ratio_percent DOUBLE PRECISION,
    average_ground_contact_time_ms DOUBLE PRECISION,
    average_step_length_mm DOUBLE PRECISION,
    average_temperature_celsius DOUBLE PRECISION,
    average_respiration_rate DOUBLE PRECISION
);

CREATE TABLE activity_fit_lap (
    id UUID PRIMARY KEY, activity_id UUID NOT NULL REFERENCES activity(id) ON DELETE CASCADE,
    lap_index INTEGER NOT NULL, started_at TIMESTAMP WITH TIME ZONE,
    distance_meters DOUBLE PRECISION, elapsed_seconds DOUBLE PRECISION, active_seconds DOUBLE PRECISION,
    average_speed_mps DOUBLE PRECISION, maximum_speed_mps DOUBLE PRECISION,
    average_heart_rate SMALLINT, maximum_heart_rate SMALLINT,
    average_cadence SMALLINT, maximum_cadence SMALLINT,
    average_power INTEGER, maximum_power INTEGER, normalized_power INTEGER,
    ascent_meters INTEGER, descent_meters INTEGER,
    UNIQUE(activity_id, lap_index)
);

CREATE TABLE activity_fit_sample (
    id UUID PRIMARY KEY, activity_id UUID NOT NULL REFERENCES activity(id) ON DELETE CASCADE,
    sample_index INTEGER NOT NULL, recorded_at TIMESTAMP WITH TIME ZONE,
    heart_rate SMALLINT, power INTEGER, cadence SMALLINT,
    altitude_meters DOUBLE PRECISION, speed_mps DOUBLE PRECISION,
    temperature_celsius SMALLINT, respiration_rate DOUBLE PRECISION,
    UNIQUE(activity_id, sample_index)
);
CREATE INDEX idx_fit_sample_activity_time ON activity_fit_sample(activity_id, recorded_at);

CREATE TABLE activity_fit_zone (
    id UUID PRIMARY KEY, activity_id UUID NOT NULL REFERENCES activity(id) ON DELETE CASCADE,
    zone_type VARCHAR(20) NOT NULL, zone_index INTEGER NOT NULL,
    lower_bound DOUBLE PRECISION, upper_bound DOUBLE PRECISION, duration_seconds DOUBLE PRECISION,
    UNIQUE(activity_id, zone_type, zone_index)
);
