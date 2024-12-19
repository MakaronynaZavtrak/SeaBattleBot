package org.urfu.semyonovowa.dataBase;

import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.RankList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class DataBaseHandler
{
    private final String forName;
    private final String url;
    private final Properties properties;
    private final List<BatchContainer> batchHolder;
    public DataBaseHandler(String forName, String url, Properties properties)
    {
        this.forName = forName;
        this.url = url;
        this.properties = properties;
        this.batchHolder = new ArrayList<>();
    }
    public static DataBaseHandlerBuilder builder(){ return new DataBaseHandlerBuilder(); }
    public void insertUserIntoDB(MyUser user) throws ClassNotFoundException
    {
        Class.forName(forName);
        try (Connection connection = DriverManager.getConnection(url, properties);
             PreparedStatement preparedStatement = connection.prepareStatement(Query.INSERT_USER_INTO_DB_SQL))
        {
            preparedStatement.setLong(1, user.getChatId());
            preparedStatement.setString(2, user.getUserName());
            preparedStatement.setString(3, user.getFirstName());
            preparedStatement.setInt(4, user.getCurrentRankIdx());
            preparedStatement.setInt(5, user.getExperience());
            preparedStatement.setInt(6, user.getWins());
            preparedStatement.setInt(7, user.getLoses());
            preparedStatement.executeUpdate();
        }
        catch (SQLException e)
        { System.err.println("Ошибка при вставке данных: " + e.getMessage()); }
    }
    public BigDecimal getUserWinRate(int iWins, int iLoses)
    {
        BigDecimal wins = BigDecimal.valueOf(iWins);
        BigDecimal loses = BigDecimal.valueOf(iLoses);
        BigDecimal winRate = (iLoses != 0)
                ? wins.divide(wins.add(loses), 4, RoundingMode.HALF_DOWN)
                :BigDecimal.valueOf(1);
        winRate = winRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_DOWN);
        return winRate;
    }
    public String getTop10Users() throws ClassNotFoundException, SQLException {
        StringBuilder result = new StringBuilder();
        int number = 1;
        Class.forName(forName);
        try (Connection connection = DriverManager.getConnection(url, properties);
             Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery(Query.GET_TOP_10_USERS_SQL);
            for (; resultSet.next() && number < 11; number++)
            {
                String firstName = resultSet.getString("first_name");
                int rankIdx = resultSet.getInt("rank_index");
                int iWins = resultSet.getInt("wins");
                int iLoses = resultSet.getInt("loses");
                BigDecimal winRate = getUserWinRate(iWins, iLoses);
                result
                    .append(number)
                    .append(". ")
                    .append(firstName)
                    .append(" (")
                    .append(RankList.ranks.get(rankIdx).rank)
                    .append("), ")
                    .append("количество побед: ")
                    .append(iWins)
                    .append(", доля побед: ")
                    .append(winRate)
                    .append("%\n");
            }
        }
        catch (SQLException e)
        {
            System.err.println("Ошибка при чтении данных: " + e.getMessage());
        }
        return result.toString();
    }
    public int getSingleUserPosition(MyUser user) throws ClassNotFoundException
    {
        int index = 1;
        Class.forName(forName);
        try (Connection connection = DriverManager.getConnection(url, properties);
             Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery(Query.GET_POSITION_SQL);
            for (; resultSet.next(); index++)
                if (resultSet.getLong("chat_id") == user.getChatId())
                    return index;
        }
        catch (SQLException e)
        {
            System.err.println("Ошибка при чтении данных: " + e.getMessage());
        }
        return index;
    }
    public void addBatch(String sql, Long chatId, int updatedValue)
    {
        batchHolder.add(new BatchContainer(sql, chatId, updatedValue));
    }
    public void executeAddedQueries() throws ClassNotFoundException {
        Class.forName(forName);

        try (Connection connection = DriverManager.getConnection(url, properties))
        {
            connection.setAutoCommit(false);
            for (BatchContainer container : batchHolder)
            {
                try(PreparedStatement preparedStatement = connection.prepareStatement(container.sql()))
                {
                    preparedStatement.setInt(1, container.updatedValue());
                    preparedStatement.setLong(2, container.chatId());
                    preparedStatement.executeUpdate();
                }
            }
            connection.commit();
            batchHolder.clear();
        }
        catch (SQLException e)
        {
            System.err.println("Ошибка при обновлении данных: " + e.getMessage());
        }
    }
}
