package org.urfu.semyonovowa.dataBase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.RankList;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализует задачи обращения к базе данных.
 *
 * Большинство методов используют {@link JdbcClient} (Spring Framework 6.1+,
 * автоконфигурируется Boot из spring-boot-starter-jdbc) вместо ручного
 * Connection/PreparedStatement/ResultSet. Он делегирует в JdbcTemplate и
 * бросает непроверяемый {@link DataAccessException} вместо SQLException —
 * поэтому вокруг каждого вызова try/catch, сохраняющий прежнее поведение
 * (залогировать и не пробрасывать дальше).
 *
 * {@code executeAddedQueries} — исключение: это ручная транзакция с несколькими
 * операторами и явным commit/rollback, для которой JdbcClient не подходит
 * (он не занимается управлением транзакциями), поэтому там остаётся «сырой» JDBC
 * через {@link DataSource}.
 *
 * throws ClassNotFoundException/SQLException в сигнатурах — вестигиальные
 * @author Oleg Semenov
 */
@Slf4j
public final class DataBaseHandler
{
    private static final RowMapper<MyUser> USER_ROW_MAPPER = (resultSet, rowNum) -> MyUser.builder()
            .chatId(resultSet.getLong(Column.CHAT_ID))
            .userName(resultSet.getString(Column.USER_NAME))
            .firstName(resultSet.getString(Column.FIRST_NAME))
            .currentRankIdx(resultSet.getInt(Column.RANK_INDEX))
            .experience(resultSet.getInt(Column.EXPERIENCE))
            .wins(resultSet.getInt(Column.WINS))
            .loses(resultSet.getInt(Column.LOSES))
            .lastMessageId(resultSet.getInt(Column.LAST_MESSAGE_ID)).build();

    /**
     * Пул соединений с базой данных. Используется напрямую только для
     * многооператорной транзакции в executeAddedQueries.
     */
    private final DataSource dataSource;
    /**
     * Флюентный клиент JDBC (Spring), используется для всех остальных запросов
     */
    private final JdbcClient jdbcClient;
    /**
     * Конейнер для хранения транзакций
     */
    private final List<BatchContainer> batchHolder;

    /**
     * Конструктор
     * @param dataSource пул соединений с базой данных (HikariCP, конфигурируется Spring Boot)
     * @param jdbcClient флюентный JDBC-клиент (автоконфигурируется Spring Boot)
     */
    public DataBaseHandler(DataSource dataSource, JdbcClient jdbcClient)
    {
        this.dataSource = dataSource;
        this.jdbcClient = jdbcClient;
        this.batchHolder = new ArrayList<>();
    }

    /**
     * Вставляет данные пользователя в базу данных
     * @param user пользователь, чьи данные заносятся в базу данных
     * @throws ClassNotFoundException вестигиальный, см. javadoc класса
     */
    public void insertUserIntoDB(MyUser user) throws ClassNotFoundException
    {
        try
        {
            jdbcClient.sql(Query.INSERT_USER_INTO_DB_SQL)
                    .param(1, user.getChatId())
                    .param(2, user.getUserName())
                    .param(3, user.getFirstName())
                    .param(4, user.getCurrentRankIdx())
                    .param(5, user.getExperience())
                    .param(6, user.getWins())
                    .param(7, user.getLoses())
                    .update();
        }
        catch (DataAccessException e) { log.error("Ошибка при вставке данных", e); }
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
     * @throws ClassNotFoundException вестигиальный, см. javadoc класса
     * @throws SQLException вестигиальный, см. javadoc класса
     */
    public String getTop10Users() throws ClassNotFoundException, SQLException
    {
        try
        {
            List<String> rows = jdbcClient.sql(Query.GET_TOP_10_USERS_SQL)
                    .query(this::formatTopUserRow)
                    .list();
            return String.join("", rows);
        }
        catch (DataAccessException e)
        {
            log.error("Ошибка при чтении данных", e);
            return "";
        }
    }

    /**
     * Мапит одну строку топ-10 в готовую для вывода строку текста.
     */
    private String formatTopUserRow(ResultSet resultSet, int rowNum) throws SQLException
    {
        String firstName = resultSet.getString(Column.FIRST_NAME);
        int rankIdx = resultSet.getInt(Column.RANK_INDEX);
        int iWins = resultSet.getInt(Column.WINS);
        int iLoses = resultSet.getInt(Column.LOSES);
        BigDecimal winRate = getUserWinRate(iWins, iLoses);
        return (rowNum + 1) + ". " + firstName + " (" + RankList.ranks.get(rankIdx).rank + "), " +
                "количество побед: " + iWins + ", доля побед: " + winRate + "%\n";
    }

    /**
     * Находит позицию текущего пользователя в топе
     * @param user пользователь, позиция которого ищется
     * @return позиция пользователя в топе
     * @throws ClassNotFoundException вестигиальный, см. javadoc класса
     */
    public int getSingleUserPosition(MyUser user) throws ClassNotFoundException
    {
        try
        {
            return jdbcClient.sql(Query.GET_POSITION_SQL)
                    .param(1, user.getChatId())
                    .query(Integer.class)
                    .optional()
                    .orElse(1);
        }
        catch (DataAccessException e)
        {
            log.error("Ошибка при чтении данных", e);
            return 1;
        }
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
     * Исполняет все накопленные транзакции. Многооператорная транзакция с явным
     * commit — намеренно на «сыром» JDBC, а не JdbcClient (см. javadoc класса).
     * @throws ClassNotFoundException вестигиальный, см. javadoc класса
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
        catch (SQLException e) { log.error("Ошибка при обновлении данных", e); }
    }

    /**
     * Заносит состояние пользователя перед его удалением из кэша
     * @param user пользователь, чьи данные заносить
     * @param messageId идентификатор последнего, отправленного ботом сообщения пользователю user
     * @throws ClassNotFoundException вестигиальный, см. javadoc класса
     */
    public void freezeUser(MyUser user, Integer messageId) throws ClassNotFoundException
    {
        try
        {
            jdbcClient.sql(Query.FREEZE_USER_SQL)
                    .param(1, messageId)
                    .param(2, user.getChatId())
                    .update();
        }
        catch (DataAccessException e) { log.error("Ошибка при вставке данных", e); }
    }

    /**
     * Извлекает пользователя из базы данных по наводящим данным.
     * Наводящими данными явлются user_name или chat_id пользователя
     * @param data наводящие данные на пользователя
     * @return пользователь, чьи данные совпадают наводящими, либо null
     */
    public MyUser pullUserFromDB(Object data)
    {
        boolean byUserName = data instanceof String;
        String sql = byUserName
                ? Query.PULL_USER_BY_USER_NAME_SQL
                : Query.PULL_USER_BY_CHAT_ID_SQL;

        try
        {
            return jdbcClient.sql(sql)
                    .param(1, data)
                    .query(USER_ROW_MAPPER)
                    .optional()
                    .orElse(null);
        }
        catch (DataAccessException e)
        {
            log.error("Ошибка при чтении данных", e);
            return null;
        }
    }

    /**
     * Обновляет столбец user_name пользователя в базе данных
     * @param user пользователь, изменивишй свой user_name
     * @param newUserName новый user_name
     * @throws ClassNotFoundException вестигиальный, см. javadoc класса
     */
    public void updateUserName(MyUser user, String newUserName) throws ClassNotFoundException
    {
        try
        {
            jdbcClient.sql(Query.UPDATE_USER_NAME_SQL)
                    .param(1, newUserName)
                    .param(2, user.getChatId())
                    .update();
        }
        catch (DataAccessException e) { log.error("Ошибка при вставке данных", e); }
    }
}
