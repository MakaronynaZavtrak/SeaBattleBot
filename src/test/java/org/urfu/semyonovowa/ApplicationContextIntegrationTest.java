package org.urfu.semyonovowa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.urfu.semyonovowa.dataBase.DataBaseHandler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Поднимает ПОЛНЫЙ Spring-контекст против настоящего PostgreSQL (Testcontainers)
 * и проверяет то, что не ловят остальные тесты: проводку автоконфигураций на старте.
 * Именно здесь молча ломался Flyway (flyway-core без spring-boot-starter-flyway
 * в модульном Spring Boot 4) — контекст вставал, но миграция не шла. Этот тест
 * такой баг краснит в CI, а не в проде.
 *
 * Автоконфиг telegrambots-стартера исключён: бин бота создаётся (конструктор без
 * сети), но long polling не запускается — в Telegram тест не ходит. Данные БД
 * подставляются в spring.datasource.* из контейнера через @DynamicPropertySource.
 * Без Docker тест пропускается.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "bot.name=test-bot",
        "bot.token=test-token",
        "bot.creator-chat-id=1"
})
@Testcontainers(disabledWithoutDocker = true)
class ApplicationContextIntegrationTest
{
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private DataBaseHandler dataBaseHandler;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsAndFlywayMigratesSchema() throws Exception
    {
        assertThat(dataBaseHandler)
                .as("бин DataBaseHandler должен создаться (значит и JdbcClient автоконфигурировался)")
                .isNotNull();

        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, "users", null))
        {
            assertThat(tables.next())
                    .as("таблица users должна быть создана миграцией Flyway на старте контекста")
                    .isTrue();
        }
    }
}
