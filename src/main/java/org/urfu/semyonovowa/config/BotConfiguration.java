package org.urfu.semyonovowa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.urfu.semyonovowa.bot.TelegramBot;
import org.urfu.semyonovowa.dataBase.DataBaseHandler;

import javax.sql.DataSource;

/**
 * Собирает и регистрирует бины приложения средствами Spring вместо самодельных билдеров.
 * Регистрация в Telegram пока ручная (через {@link TelegramBotsApi}); на этапе 7
 * она уедет в официальный Spring Boot стартер telegrambots.
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

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException
    {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(telegramBot);
        return telegramBotsApi;
    }
}
