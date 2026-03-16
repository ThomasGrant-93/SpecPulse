-- V1__init_schema.sql
-- Initial schema for SpecPulse

-- Services registry table
CREATE TABLE services
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL UNIQUE,
    open_api_url VARCHAR(1024) NOT NULL,
    description  TEXT,
    enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_services_enabled ON services (enabled);
CREATE INDEX idx_services_name ON services (name);

-- Specification versions table
CREATE TABLE spec_versions
(
    id              BIGSERIAL PRIMARY KEY,
    service_id      BIGINT      NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    version_hash    VARCHAR(64) NOT NULL,
    spec_content    TEXT        NOT NULL,
    spec_version    VARCHAR(50),
    spec_title      VARCHAR(500),
    file_size_bytes BIGINT,
    pulled_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (service_id, version_hash)
);

CREATE INDEX idx_spec_versions_service ON spec_versions (service_id);
CREATE INDEX idx_spec_versions_hash ON spec_versions (version_hash);
CREATE INDEX idx_spec_versions_pulled_at ON spec_versions (pulled_at);

-- Spec diff results table
CREATE TABLE spec_diffs
(
    id                     BIGSERIAL PRIMARY KEY,
    service_id             BIGINT    NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    from_version_id        BIGINT    NOT NULL REFERENCES spec_versions (id) ON DELETE CASCADE,
    to_version_id          BIGINT    NOT NULL REFERENCES spec_versions (id) ON DELETE CASCADE,
    diff_content           TEXT      NOT NULL,
    has_breaking_changes   BOOLEAN   NOT NULL DEFAULT FALSE,
    breaking_changes_count INT       NOT NULL DEFAULT 0,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_spec_diffs_service ON spec_diffs (service_id);
CREATE INDEX idx_spec_diffs_versions ON spec_diffs (from_version_id, to_version_id);

-- Audit log table
CREATE TABLE audit_log
(
    id              BIGSERIAL PRIMARY KEY,
    service_id      BIGINT      REFERENCES services (id) ON DELETE SET NULL,
    spec_version_id BIGINT      REFERENCES spec_versions (id) ON DELETE SET NULL,
    event_type      VARCHAR(50) NOT NULL,
    event_details   TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_service ON audit_log (service_id);
CREATE INDEX idx_audit_log_event_type ON audit_log (event_type);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

-- Pull execution history table
CREATE TABLE pull_executions
(
    id                  BIGSERIAL PRIMARY KEY,
    service_id          BIGINT      NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL,
    http_status_code    INT,
    error_message       TEXT,
    duration_ms         BIGINT,
    new_version_created BOOLEAN              DEFAULT FALSE,
    executed_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pull_executions_service ON pull_executions (service_id);
CREATE INDEX idx_pull_executions_status ON pull_executions (status);
CREATE INDEX idx_pull_executions_executed_at ON pull_executions (executed_at);

-- Test executions table
CREATE TABLE test_executions
(
    id               BIGSERIAL PRIMARY KEY,
    service_id       BIGINT        NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    spec_version_id  BIGINT        REFERENCES spec_versions (id) ON DELETE SET NULL,
    test_name        VARCHAR(255)  NOT NULL,
    endpoint         VARCHAR(1024) NOT NULL,
    method           VARCHAR(10)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    response_code    INT,
    response_time_ms BIGINT,
    error_message    TEXT,
    executed_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_executions_service ON test_executions (service_id);
CREATE INDEX idx_test_executions_version ON test_executions (spec_version_id);
CREATE INDEX idx_test_executions_status ON test_executions (status);
