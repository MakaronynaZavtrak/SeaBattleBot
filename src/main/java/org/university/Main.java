package org.university;

import org.university.bot.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static Properties getBotProperties(File propertyFile) throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(propertyFile);
        properties.load(input);
        return properties;
    }
    public static void main(String[] args) throws TelegramApiException {
        String token;
        String botUserName;
                try {
            Properties properties = getBotProperties(new File(args[0]));
            token = properties.getProperty("bot.token");
            botUserName = properties.getProperty("bot.name");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TelegramBot(botUserName, token));
    }
}
