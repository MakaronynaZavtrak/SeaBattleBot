package org.urfu.semyonovowa.dataBase;

public final class Query
{
    private Query(){}
    public static final String INSERT_USER_INTO_DB_SQL =
            """
            INSERT INTO users (chat_id, user_name, first_name) VALUES (?, ?, ?);
            """;
}
