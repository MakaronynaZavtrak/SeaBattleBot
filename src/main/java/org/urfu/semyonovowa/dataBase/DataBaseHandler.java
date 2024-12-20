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
                String firstName = resultSet.getString(Column.FIRST_NAME);
                int rankIdx = resultSet.getInt(Column.RANK_INDEX);
                int iWins = resultSet.getInt(Column.WINS);
                int iLoses = resultSet.getInt(Column.LOSES);
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

    public void freezeUser(MyUser user, Integer messageId) throws ClassNotFoundException
    {
        Class.forName(forName);
        try (Connection connection = DriverManager.getConnection(url, properties);
             PreparedStatement preparedStatement = connection.prepareStatement(Query.FREEZE_USER))
        {
            preparedStatement.setInt(1, messageId);
            preparedStatement.setLong(2, user.getChatId());
            preparedStatement.executeUpdate();
        }
        catch (SQLException e)
        { System.err.println("Ошибка при вставке данных: " + e.getMessage()); }
    }

    public MyUser pullUserFromDB(String userName)
    {
        String sql = Query.PULL_USER_FROM_DB + userName + "';";
        MyUser pulledUser = null;
        try (Connection connection = DriverManager.getConnection(url, properties);
             Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            pulledUser = MyUser.builder()
                    .chatId(resultSet.getLong(Column.CHAT_ID))
                    .userName(resultSet.getString(Column.USER_NAME))
                    .firstName(resultSet.getString(Column.FIRST_NAME))
                    .currentRankIdx(resultSet.getInt(Column.RANK_INDEX))
                    .experience(resultSet.getInt(Column.EXPERIENCE))
                    .wins(resultSet.getInt(Column.WINS))
                    .loses(resultSet.getInt(Column.LOSES))
                    .lastMessageId(resultSet.getInt(Column.LAST_MESSAGE_ID)).build();
        }
        catch (SQLException e)
        {
            System.err.println("Ошибка при чтении данных: " + e.getMessage());
        }
        return pulledUser;
    }
}
