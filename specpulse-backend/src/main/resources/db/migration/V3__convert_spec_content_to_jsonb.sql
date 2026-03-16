-- V3__convert_spec_content_to_jsonb.sql
-- Convert spec_content from TEXT to JSONB for better JSON handling

-- Convert existing TEXT data to JSONB
ALTER TABLE spec_versions
    ALTER COLUMN spec_content TYPE JSONB USING spec_content::jsonb;

-- Add validation constraint to ensure valid JSON
ALTER TABLE spec_versions
    ADD CONSTRAINT spec_content_is_json CHECK (jsonb_typeof(spec_content) = 'object');

-- Add GIN index for efficient JSON queries
CREATE INDEX idx_spec_versions_content_gin ON spec_versions USING GIN (spec_content);

-- Add index on spec version path for faster lookups
CREATE INDEX idx_spec_versions_openapi_version ON spec_versions ((spec_content ->> 'openapi'));
CREATE INDEX idx_spec_versions_swagger_version ON spec_versions ((spec_content ->> 'swagger'));
