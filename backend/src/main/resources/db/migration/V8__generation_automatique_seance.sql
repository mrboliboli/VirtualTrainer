ALTER TABLE ai_configuration
    ADD COLUMN auto_generate_workout_after_import BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE planned_workout (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goal(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    planned_date DATE NOT NULL,
    objective VARCHAR(1000) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 1 AND 360),
    intensity VARCHAR(30) NOT NULL,
    steps_json TEXT NOT NULL,
    rationale VARCHAR(1500) NOT NULL,
    confidence VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_planned_workout_next ON planned_workout (status, planned_date, created_at DESC);
