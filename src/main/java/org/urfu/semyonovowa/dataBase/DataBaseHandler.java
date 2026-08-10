package org.urfu.semyonovowa.dataBase;

import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.RankList;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализует задачи обращения к базе данных
 * @author Oleg Semenov
 */
public final class DataBaseHandler
{
    /**
     * Пул соединений с базой данных
     */
    private final DataSource dataSource;
    /**
     * Конейнер для хранения транзакций
     */
    private final List<BatchContainer> batchHolder;

    /**
     * Конструктор
     * @param dataSource пул соединений с базой данных (HikariCP, конфигурируется Spring Boot)
     */
    public DataBaseHandler(DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.batchHolder = new ArrayList<>();
    }

    /**
     * Вставляет данные пользователя в базу данных
     * @param user пользователь, чьи данные заносятся в базу данных
     * @throws ClassNotFoundException если не найден класс forName
     */
    public void insertUserIntoDB(MyUser user) throws ClassNotFoundException
    {
        try (Connection connection = dataSource.getConnection();
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
        catch (SQLException e) { System.err.println("Ошибка при вставке данных: " + e.getMessage()); }
    }

    /**
     * Вычисляет долю побед на основании количества побед и поражений
     * @param iWins количество побед
     * @param iLoses количество поражений
     * @return доля побед
     */
    public BigDecimal getUserWinRate(int iWins, int iLoses)
    {
        BigDecimal wins = BigDecimal.valueOf(iWins);
        BigDecimal loses = BigDecimal.valueOf(iLoses);
        BigDecimal winRate = (iLoses != 0)
                ? wins.divide(wins.add(loses), 4, RoundingMode.HALF_DOWN)
                : BigDecimal.valueOf(1);
        winRate = winRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_DOWN);
        return winRate;
    }

    /**
     * Находит десять лучших пользователей
     * @return сообщение с данными о статистике 10 лучших игроков
     * @throws ClassNotFoundException если не найден класс forName
     * @throws SQLException если не удалось выполить запрос
     */
    public String getTop10Users() throws ClassNotFoundException, SQLException {
        StringBuilder result = new StringBuilder();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery(Query.GET_TOP_10_USERS_SQL);
            for (int number = 1; resultSet.next() && number < 11; number++)
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
        catch (SQLException e) { System.err.println("Ошибка при чтении данных: " + e.getMessage()); }
        return result.toString();
    }

    /**
     * Находит позицию текущего пользователя в топе
     * @param user пользователь, позиция которого ищется
     * @return позиция пользователя в топе
     * @throws ClassNotFoundException если не найден класс forName
     */
    public int getSingleUserPosition(MyUser user) throws ClassNotFoundException
    {
        int index = 1;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement())
        {
            ResultSet resultSet = statement.executeQuery(Query.GET_POSITION_SQL);
            for (; resultSet.next(); index++)
                if (resultSet.getLong(Column.CHAT_ID) == user.getChatId())
                    return index;
        }
        catch (SQLException e) { System.err.println("Ошибка при чтении данных: " + e.getMessage()); }
        return index;
    }

    /**
     * добавляет новую транзакцию в хранение
     * @param sql запрос, который нужно осуществить к базе данных
     * @param chatId идентификатор диалога с пользователем
     * @param updatedValue значение, на которое нужно менять исходное
     */
    public void addBatch(String sql, Long chatId, int updatedValue)
    { batchHolder.add(new BatchContainer(sql, chatId, updatedValue)); }

    /**
     * Исполняет все накопленные транзакции
     * @throws ClassNotFoundException если не найден класс forName
     */
    public void executeAddedQueries() throws ClassNotFoundException
    {
        try (Connection connection = dataSource.getConnection())
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
        catch (SQLException e) { System.err.println("Ошибка при обновлении данных: " + e.getMessage()); }
    }

    /**
     * Заносит состояние пользователя перед его удалением из кэша
     * @param user пользователь, чьи данные заносить
     * @param messageId идентификатор последнего, отправленного ботом сообщения пользователю user
     * @throws ClassNotFoundException если не найден класс forName
     */
    public void freezeUser(MyUser user, Integer messageId) throws ClassNotFoundException
    {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.FREEZE_USER_SQL))
        {
            preparedStatement.setInt(1, messageId);
            preparedStatement.setLong(2, user.getChatId());
            preparedStatement.executeUpdate();
        }
        catch (SQLException e) { System.err.println("Ошибка при вставке данных: " + e.getMessage()); }
    }

    /**
     * Извлекает пользователя из базы данных по наводящим данным.
     * Наводящими данными явлются user_name или chat_id пользователя
     * @param data наводящие данные на пользователя
     * @return пользователь, чьи данные совпадают наводящими
     */
    public MyUser pullUserFromDB(Object data)
    {
        boolean byUserName = data instanceof String;
        String sql = byUserName
                ? Query.PULL_USER_BY_USER_NAME_SQL
                : Query.PULL_USER_BY_CHAT_ID_SQL;

        MyUser pulledUser = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql))
        {
            if (byUserName)
                preparedStatement.setString(1, (String) data);
            else
                preparedStatement.setLong(1, (Long) data);

            try (ResultSet resultSet = preparedStatement.executeQuery())
            {
                if (resultSet.next())
                {
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
            }
        }
        catch (SQLException e) { System.err.println("Ошибка при чтении данных: " + e.getMessage()); }
        return pulledUser;
    }

    /**
     * Обновляет столбец user_name пользователя в базе данных
     * @param user пользователь, изменивишй свой user_name
     * @param newUserName новый user_name
     * @throws ClassNotFoundException если не найден класс forName
     */
    public void updateUserName(MyUser user, String newUserName) throws ClassNotFoundException
    {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(Query.UPDATE_USER_NAME_SQL))
        {
            preparedStatement.setString(1, newUserName);
            preparedStatement.setLong(2, user.getChatId());
            preparedStatement.executeUpdate();
        }
        catch (SQLException e) { System.err.println("Ошибка при вставке данных: " + e.getMessage()); }
    }
}
