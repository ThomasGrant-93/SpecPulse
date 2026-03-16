-- V6__Create_application_settings.sql

-- Таблица настроек приложения
CREATE TABLE application_settings
(
    id            BIGSERIAL PRIMARY KEY,
    category      VARCHAR(100) NOT NULL,
    setting_key   VARCHAR(255) NOT NULL,
    setting_value JSONB,
    value_type    VARCHAR(50)  NOT NULL DEFAULT 'string',
    description   TEXT,
    is_public     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_editable   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_category_key UNIQUE (category, setting_key)
);

CREATE INDEX idx_settings_category ON application_settings (category);
CREATE INDEX idx_settings_key ON application_settings (setting_key);

-- Триггер для updated_at
CREATE TRIGGER update_application_settings_updated_at
    BEFORE UPDATE
    ON application_settings
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Предзаполнение настройками по умолчанию
INSERT INTO application_settings (category, setting_key, setting_value, value_type, description, is_public, is_editable)
VALUES
-- Общие настройки
('general', 'app.name', '"SpecPulse"', 'string', 'Название приложения', TRUE, TRUE),
('general', 'app.description', '"OpenAPI Specification Management System"', 'string', 'Описание приложения', TRUE,
 TRUE),
('general', 'app.logo_url', 'null', 'string', 'URL логотипа', TRUE, TRUE),
('general', 'app.theme', '"light"', 'string', 'Тема интерфейса (light/dark)', TRUE, TRUE),
('general', 'app.items_per_page', '10', 'integer', 'Элементов на страницу', TRUE, TRUE),

-- Scheduler настройки
('scheduler', 'pull.enabled', 'true', 'boolean', 'Включить автоматический pull', FALSE, TRUE),
('scheduler', 'pull.cron_expression', '"0 0 */6 * * ?"', 'string', 'Cron выражение для pull (каждые 6 часов)', FALSE,
 TRUE),
('pull', 'pull.timeout_seconds', '30', 'integer', 'Таймаут pull запроса (секунды)', FALSE, TRUE),
('pull', 'pull.max_retries', '3', 'integer', 'Максимум попыток pull', FALSE, TRUE),

-- Уведомления
('notifications', 'webhook.enabled', 'false', 'boolean', 'Включить webhook уведомления', FALSE, TRUE),
('notifications', 'webhook.url', 'null', 'string', 'URL webhook для уведомлений', FALSE, TRUE),
('notifications', 'webhook.secret', 'null', 'string', 'Секрет для подписи webhook', FALSE, TRUE),
('notifications', 'email.enabled', 'false', 'boolean', 'Включить email уведомления', FALSE, TRUE),
('notifications', 'email.recipients', '[]', 'array', 'Список email получателей', FALSE, TRUE),

-- Безопасность (будущая интеграция)
('security', 'auth.enabled', 'false', 'boolean', 'Включить аутентификацию', FALSE, FALSE),
('security', 'auth.provider', '"internal"', 'string', 'Провайдер аутентификации (internal/ldap/oauth2)', FALSE, FALSE),
('security', 'auth.session_timeout_minutes', '60', 'integer', 'Таймаут сессии (минуты)', FALSE, TRUE),
('security', 'rbac.enabled', 'false', 'boolean', 'Включить RBAC', FALSE, FALSE),
('security', 'abac.enabled', 'false', 'boolean', 'Включить ABAC', FALSE, FALSE),

-- Аудит
('audit', 'audit.enabled', 'true', 'boolean', 'Включить аудит событий', FALSE, TRUE),
('audit', 'audit.retention_days', '90', 'integer', 'Срок хранения аудит логов (дней)', FALSE, TRUE);
