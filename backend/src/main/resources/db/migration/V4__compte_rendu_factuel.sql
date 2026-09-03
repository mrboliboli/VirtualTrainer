ALTER TABLE activity ADD COLUMN perceived_exertion_rpe DOUBLE PRECISION;
ALTER TABLE activity ADD COLUMN garmin_feeling_score DOUBLE PRECISION;
ALTER TABLE activity ADD COLUMN subjective_feedback_source VARCHAR(30);

ALTER TABLE activity ADD CONSTRAINT chk_activity_rpe
    CHECK (perceived_exertion_rpe IS NULL OR perceived_exertion_rpe BETWEEN 0 AND 10);
ALTER TABLE activity ADD CONSTRAINT chk_activity_feeling
    CHECK (garmin_feeling_score IS NULL OR garmin_feeling_score BETWEEN 0 AND 100);
