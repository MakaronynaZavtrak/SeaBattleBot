package org.urfu.semyonovowa.dataBase;

import org.urfu.semyonovowa.user.MyUser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public final class DataBaseHandler
{
    private final String forName;
    private final String url;
    private final Properties properties;
    public DataBaseHandler(String forName, String url, Properties properties)
    {
        this.forName = forName;
        this.url = url;
        this.properties = properties;
    }
    public static DataBaseHandlerBuilder builder(){ return new DataBaseHandlerBuilder(); }
    public void insertUserIntoDB(MyUser user) throws ClassNotFoundException, SQLException
    {
        Class.forName(forName);
        try (Connection connection = DriverManager.getConnection(url, properties);
             PreparedStatement preparedStatement = connection.prepareStatement(Query.INSERT_USER_INTO_DB_SQL))
        {
            preparedStatement.setLong(1, user.getChatId());
            preparedStatement.setString(2, user.getUserName());
            preparedStatement.setString(3, user.getFirstName());
            preparedStatement.executeUpdate();
        }
        catch (SQLException e)
        { System.err.println("Ошибка при вставке данных: " + e.getMessage()); }
    }
}
