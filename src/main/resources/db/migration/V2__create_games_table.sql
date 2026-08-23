-- Активные партии для персистентности: одна строка на игру, ключ — chat_id
-- создателя (у игрока одновременно одна активная партия). Логическое состояние
-- партии хранится JSON-снапшотом в колонке snapshot; поле рендерится заново при
-- восстановлении, поэтому телеграм-объекты в БД не тащим.

CREATE TABLE games
(
    game_id          BIGINT      PRIMARY KEY,
    creator_chat_id  BIGINT      NOT NULL,
    opponent_chat_id BIGINT      NOT NULL,
    snapshot         TEXT        NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
