ALTER TABLE planned_workout
    ADD COLUMN version INTEGER,
    ADD COLUMN previous_workout_id UUID REFERENCES planned_workout(id) ON DELETE SET NULL,
    ADD COLUMN decided_at TIMESTAMP WITH TIME ZONE;

WITH versions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS workout_version
    FROM planned_workout
)
UPDATE planned_workout workout
SET version = versions.workout_version
FROM versions
WHERE workout.id = versions.id;

ALTER TABLE planned_workout ALTER COLUMN version SET NOT NULL;
ALTER TABLE planned_workout ADD CONSTRAINT uk_planned_workout_version UNIQUE (version);

CREATE INDEX idx_planned_workout_history ON planned_workout (version DESC);
