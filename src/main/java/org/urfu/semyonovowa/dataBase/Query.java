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
            SELECT first_name, rank_index, wins, loses from users
            ORDER BY wins DESC;
            """;
    public static final String GET_POSITION_SQL =
            """
            SELECT chat_id from users
            ORDER BY wins DESC;
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
}
