CREATE TABLE ai_configuration (
    id UUID PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    active_provider VARCHAR(50) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    analysis_model VARCHAR(100) NOT NULL,
    planning_model VARCHAR(100) NOT NULL,
    temperature DOUBLE PRECISION NOT NULL CHECK (temperature >= 0 AND temperature <= 2),
    max_output_tokens INTEGER NOT NULL CHECK (max_output_tokens BETWEEN 128 AND 16384),
    custom_instructions VARCHAR(4000),
    last_test_status VARCHAR(30),
    last_tested_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ai_call (
    id UUID PRIMARY KEY,
    activity_id UUID REFERENCES activity(id) ON DELETE CASCADE,
    operation_type VARCHAR(30) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(30) NOT NULL,
    schema_version VARCHAR(30) NOT NULL,
    analysis_version INTEGER NOT NULL CHECK (analysis_version > 0),
    input_json TEXT NOT NULL,
    response_json TEXT,
    provider_response_id VARCHAR(200),
    duration_millis BIGINT CHECK (duration_millis IS NULL OR duration_millis >= 0),
    input_tokens INTEGER CHECK (input_tokens IS NULL OR input_tokens >= 0),
    output_tokens INTEGER CHECK (output_tokens IS NULL OR output_tokens >= 0),
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    controlled_error VARCHAR(1000),
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (activity_id, operation_type, analysis_version)
);

CREATE UNIQUE INDEX uq_ai_call_active_prompt
    ON ai_call (activity_id, operation_type, prompt_version)
    WHERE status IN ('EN_ATTENTE', 'EN_COURS', 'ERREUR_TEMPORAIRE');
CREATE INDEX idx_ai_call_retry ON ai_call (status, next_attempt_at);
CREATE INDEX idx_ai_call_activity_version ON ai_call (activity_id, operation_type, analysis_version DESC);
