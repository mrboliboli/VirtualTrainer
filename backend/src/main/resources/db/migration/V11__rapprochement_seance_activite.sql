ALTER TABLE planned_workout
    ADD COLUMN completed_activity_id UUID REFERENCES activity(id) ON DELETE SET NULL,
    ADD COLUMN completion_matched_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN completion_match_method VARCHAR(40);

CREATE UNIQUE INDEX uk_planned_workout_completed_activity
    ON planned_workout (completed_activity_id)
    WHERE completed_activity_id IS NOT NULL;

CREATE INDEX idx_planned_workout_matching
    ON planned_workout (planned_date, status)
    WHERE completed_activity_id IS NULL;
