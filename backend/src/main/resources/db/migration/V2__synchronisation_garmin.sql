CREATE TABLE activity_synchronization (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL,
    error_message VARCHAR(1000),
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_activity_sync_retry ON activity_synchronization (status, next_attempt_at);

CREATE TABLE synchronization_candidate (
    id UUID PRIMARY KEY,
    synchronization_id UUID NOT NULL REFERENCES activity_synchronization(id) ON DELETE CASCADE,
    external_id VARCHAR(128) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sport VARCHAR(100),
    distance_meters BIGINT,
    duration_seconds BIGINT,
    confidence VARCHAR(10) NOT NULL,
    UNIQUE (synchronization_id, external_id)
);

CREATE TABLE activity (
    id UUID PRIMARY KEY,
    source VARCHAR(30) NOT NULL,
    source_external_id VARCHAR(128) NOT NULL,
    discovered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_synchronized_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processing_status VARCHAR(30) NOT NULL,
    details_json TEXT NOT NULL,
    original_fit BYTEA,
    fit_fingerprint VARCHAR(64),
    UNIQUE (source, source_external_id)
);
