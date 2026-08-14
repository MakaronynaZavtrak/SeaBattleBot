package org.urfu.semyonovowa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.urfu.semyonovowa.bot.TelegramBot;
import org.urfu.semyonovowa.dataBase.DataBaseHandler;

import javax.sql.DataSource;

/**
 * Собирает бины приложения средствами Spring. Регистрацию бота в Telegram
 * берёт на себя telegrambots-springboot-longpolling-starter: он сам находит
 * бин типа SpringLongPollingBot и запускает long polling — ручной TelegramBotsApi больше не нужен.
 */
@Configuration
public class BotConfiguration
{
    @Bean
    public DataBaseHandler dataBaseHandler(DataSource dataSource)
    {
        return new DataBaseHandler(dataSource);
    }

    @Bean
    public TelegramBot telegramBot(BotProperties botProperties, DataBaseHandler dataBaseHandler)
    {
        return new TelegramBot(
                botProperties.name(),
                botProperties.token(),
                botProperties.creatorChatId(),
                dataBaseHandler);
    }
}
