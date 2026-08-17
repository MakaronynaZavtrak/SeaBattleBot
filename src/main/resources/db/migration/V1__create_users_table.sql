-- Базовая схема бота «Морской бой».
-- Восстановлена из запросов (Query.java) и колонок (Column.java): именно её
-- ожидает DataBaseHandler. Для уже существующей БД Flyway не запускает эту
-- миграцию (spring.flyway.baseline-on-migrate=true помечает её как базовую),
-- а на чистой БД (новый деплой, Testcontainers) — создаёт таблицу с нуля.

CREATE TABLE users
(
    chat_id         BIGINT       PRIMARY KEY,
    user_name       VARCHAR(255),
    first_name      VARCHAR(255) NOT NULL,
    rank_index      INTEGER      NOT NULL DEFAULT 0,
    experience      INTEGER      NOT NULL DEFAULT 0,
    wins            INTEGER      NOT NULL DEFAULT 0,
    loses           INTEGER      NOT NULL DEFAULT 0,
    last_message_id INTEGER
);

-- Индекс под таблицу лидеров и позицию игрока (ORDER BY wins DESC).
CREATE INDEX idx_users_wins ON users (wins DESC);

-- Индекс под поиск пользователя по @username (приглашения).
CREATE INDEX idx_users_user_name ON users (user_name);
