package org.urfu.semyonovowa.dataBase;

public final class Query
{
    private Query(){}
    public static final String INSERT_USER_INTO_DB_SQL =
            """
            INSERT INTO users (chat_id, user_name, first_name, rank_index, experience, wins, loses)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;
    public static final String GET_TOP_10_USERS_SQL =
            """
            SELECT first_name, rank_index, wins, loses FROM users
            ORDER BY wins DESC
            LIMIT 10;
            """;
    public static final String GET_POSITION_SQL =
            """
            SELECT position FROM
            (
                SELECT chat_id, ROW_NUMBER() OVER (ORDER BY wins DESC) AS position
                FROM users
            ) ranked
            WHERE chat_id = ?;
            """;
    public static final String UPDATE_WINS_SQL = """
            UPDATE users
            SET wins = ?
            WHERE chat_id = ?;
            """;
    public static final String UPDATE_LOSES_SQL = """
            UPDATE users
            SET loses = ?
            WHERE chat_id = ?;
            """;
    public static final String UPDATE_EXPERIENCE_SQL = """
            UPDATE users
            SET experience = ?
            WHERE chat_id = ?;
            """;
    public static final String UPDATE_RANK_INDEX_SQL = """
            UPDATE users
            SET rank_index = ?
            WHERE chat_id = ?;
            """;
    public static final String FREEZE_USER_SQL = """
            UPDATE users
            SET last_message_id = ?
            WHERE chat_id = ?;
            """;
    public static final String UPDATE_USER_NAME_SQL = """
            UPDATE users
            SET user_name = ?
            WHERE chat_id = ?;
            """;
    public static final String PULL_USER_BY_CHAT_ID_SQL = "SELECT * FROM users WHERE chat_id = ?;";
    public static final String PULL_USER_BY_USER_NAME_SQL = "SELECT * FROM users WHERE user_name = ?;";
}
