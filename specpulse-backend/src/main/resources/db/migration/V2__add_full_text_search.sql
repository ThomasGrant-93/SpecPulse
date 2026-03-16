-- V2__add_full_text_search.sql
-- Add full-text search capability to services table

-- Add search vector column
ALTER TABLE services
    ADD COLUMN search_vector tsvector;

-- Populate search vector for existing records
UPDATE services
SET search_vector =
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(open_api_url, '')), 'C');

-- Create index for fast search
CREATE INDEX idx_services_search_vector ON services USING GIN (search_vector);

-- Create trigger to update search vector on changes
CREATE OR REPLACE FUNCTION services_search_vector_update() RETURNS trigger AS
$$
BEGIN
    NEW.search_vector :=
            setweight(to_tsvector('english', coalesce(NEW.name, '')), 'A') ||
            setweight(to_tsvector('english', coalesce(NEW.description, '')), 'B') ||
            setweight(to_tsvector('simple', coalesce(NEW.open_api_url, '')), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER services_search_vector_trigger
    BEFORE INSERT OR UPDATE
    ON services
    FOR EACH ROW
EXECUTE FUNCTION services_search_vector_update();

-- Add search rank column for ordering
ALTER TABLE services
    ADD COLUMN search_rank REAL DEFAULT 0;
CREATE INDEX idx_services_search_rank ON services (search_rank DESC);
