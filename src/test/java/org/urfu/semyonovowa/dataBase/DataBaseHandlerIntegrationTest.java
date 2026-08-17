package org.urfu.semyonovowa.dataBase;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.State;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты {@link DataBaseHandler} против НАСТОЯЩЕГО PostgreSQL,
 * поднятого в Docker через Testcontainers. Flyway накатывает на контейнер
 * миграцию V1, поэтому проверяется и схема, и SQL запросов.
 *
 * Контекст Spring намеренно не поднимается (иначе стартер telegrambots попытался
 * бы зарегистрировать бота) — DataBaseHandler собирается вручную из DataSource.
 *
 * Без Docker тест пропускается ({@code disabledWithoutDocker = true}): локальный
 * {@code mvn test} без Docker останется зелёным, а в CI (там Docker есть) отработает.
 */
@Testcontainers(disabledWithoutDocker = true)
class DataBaseHandlerIntegrationTest
{
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static DataBaseHandler handler;
    static JdbcClient jdbcClient;

    @BeforeAll
    static void setUp()
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");

        Flyway.configure().dataSource(dataSource).load().migrate();

        jdbcClient = JdbcClient.create(dataSource);
        handler = new DataBaseHandler(dataSource, jdbcClient);
    }

    @BeforeEach
    void cleanTable()
    {
        jdbcClient.sql("DELETE FROM users").update();
    }

    /** Создаёт пользователя с заданной статистикой через билдер (для сценариев с wins). */
    private MyUser userWithWins(long chatId, String userName, String firstName, int wins)
    {
        return MyUser.builder()
                .chatId(chatId).userName(userName).firstName(firstName)
                .wins(wins).loses(0).experience(0).currentRankIdx(0).lastMessageId(0).build();
    }

    @Test
    @DisplayName("вставка и чтение по chat_id возвращают того же пользователя")
    void insertAndPullByChatId() throws Exception
    {
        handler.insertUserIntoDB(new MyUser(1L, "alice", "Alice", State.IN_LOBBY));

        MyUser pulled = handler.pullUserFromDB(1L);

        assertThat(pulled).isNotNull();
        assertThat(pulled.getChatId()).isEqualTo(1L);
        assertThat(pulled.getUserName()).isEqualTo("alice");
        assertThat(pulled.getFirstName()).isEqualTo("Alice");
        assertThat(pulled.getWins()).isZero();
        assertThat(pulled.getLoses()).isZero();
    }

    @Test
    @DisplayName("чтение по user_name находит пользователя")
    void pullByUserName() throws Exception
    {
        handler.insertUserIntoDB(new MyUser(2L, "bob", "Bob", State.IN_LOBBY));

        MyUser pulled = handler.pullUserFromDB("bob");

        assertThat(pulled).isNotNull();
        assertThat(pulled.getChatId()).isEqualTo(2L);
        assertThat(pulled.getFirstName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("чтение отсутствующего пользователя возвращает null")
    void pullAbsentReturnsNull()
    {
        assertThat(handler.pullUserFromDB(999L)).isNull();
        assertThat(handler.pullUserFromDB("nobody")).isNull();
    }

    @Test
    @DisplayName("обновление user_name сохраняется")
    void updateUserNamePersists() throws Exception
    {
        MyUser user = new MyUser(3L, "old_name", "Carol", State.IN_LOBBY);
        handler.insertUserIntoDB(user);

        handler.updateUserName(user, "new_name");

        assertThat(handler.pullUserFromDB(3L).getUserName()).isEqualTo("new_name");
    }

    @Test
    @DisplayName("freezeUser сохраняет last_message_id")
    void freezeUserPersistsLastMessageId() throws Exception
    {
        MyUser user = new MyUser(4L, "dave", "Dave", State.IN_LOBBY);
        handler.insertUserIntoDB(user);

        handler.freezeUser(user, 42);

        assertThat(handler.pullUserFromDB(4L).getLastMessageId()).isEqualTo(42);
    }

    @Test
    @DisplayName("позиция считается по убыванию побед")
    void positionByWinsDesc() throws Exception
    {
        handler.insertUserIntoDB(userWithWins(10L, "a", "A", 10));
        handler.insertUserIntoDB(userWithWins(20L, "b", "B", 5));
        handler.insertUserIntoDB(userWithWins(30L, "c", "C", 20));

        assertThat(handler.getSingleUserPosition(userWithWins(30L, "c", "C", 20))).isEqualTo(1);
        assertThat(handler.getSingleUserPosition(userWithWins(10L, "a", "A", 10))).isEqualTo(2);
        assertThat(handler.getSingleUserPosition(userWithWins(20L, "b", "B", 5))).isEqualTo(3);
    }

    @Test
    @DisplayName("топ-10 упорядочен по убыванию побед")
    void topTenOrderedByWins() throws Exception
    {
        handler.insertUserIntoDB(userWithWins(10L, "a", "Anna", 3));
        handler.insertUserIntoDB(userWithWins(20L, "b", "Boris", 15));

        String top = handler.getTop10Users();

        assertThat(top).contains("Anna").contains("Boris");
        assertThat(top.indexOf("Boris")).isLessThan(top.indexOf("Anna"));
    }

    @Test
    @DisplayName("накопленные транзакции применяются в executeAddedQueries")
    void batchedUpdatesApply() throws Exception
    {
        handler.insertUserIntoDB(new MyUser(5L, "erin", "Erin", State.IN_LOBBY));

        handler.addBatch(Query.UPDATE_WINS_SQL, 5L, 7);
        handler.executeAddedQueries();

        assertThat(handler.pullUserFromDB(5L).getWins()).isEqualTo(7);
    }
}
