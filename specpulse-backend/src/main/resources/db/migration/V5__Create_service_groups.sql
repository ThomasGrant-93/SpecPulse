-- V5__Create_service_groups.sql

-- Таблица групп сервисов
CREATE TABLE service_groups
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    parent_group_id BIGINT REFERENCES service_groups (id) ON DELETE CASCADE,
    color           VARCHAR(7),
    icon            VARCHAR(50),
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_name_per_parent UNIQUE (name, parent_group_id)
);

CREATE INDEX idx_service_groups_parent ON service_groups (parent_group_id);
CREATE INDEX idx_service_groups_sort ON service_groups (sort_order);

-- Таблица связей групп и сервисов
CREATE TABLE group_members
(
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT    NOT NULL REFERENCES service_groups (id) ON DELETE CASCADE,
    service_id BIGINT    NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    added_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_group_service UNIQUE (group_id, service_id)
);

CREATE INDEX idx_group_members_group ON group_members (group_id);
CREATE INDEX idx_group_members_service ON group_members (service_id);

-- Добавим колонку group_id в services для быстрого доступа к основной группе
ALTER TABLE services
    ADD COLUMN group_id BIGINT REFERENCES service_groups (id) ON DELETE SET NULL;

CREATE INDEX idx_services_group ON services (group_id);

-- Триггер для updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_service_groups_updated_at
    BEFORE UPDATE
    ON service_groups
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
