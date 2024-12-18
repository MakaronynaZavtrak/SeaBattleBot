package org.urfu.semyonovowa;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.urfu.semyonovowa.bot.TelegramBot;
import org.urfu.semyonovowa.dataBase.DataBaseHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main
{
    public static Properties getProperties(File propertyFile) throws IOException
    {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(propertyFile);
        properties.load(input);
        return properties;
    }
    public static void main(String[] args) throws TelegramApiException
    {
        String token;
        String botUserName;
        long creatorChatId;

        String forName;
        String url;
        String user;
        String password;
        try
        {
            Properties botProperties = getProperties(new File(args[0]));
            token = botProperties.getProperty("bot.token");
            botUserName = botProperties.getProperty("bot.name");
            creatorChatId = Long.parseLong(botProperties.getProperty("bot.creatorChatId"));

            Properties dataBaseProperties = getProperties(new File(args[1]));
            forName = dataBaseProperties.getProperty("db.forname");
            url = dataBaseProperties.getProperty("db.url");
            user = dataBaseProperties.getProperty("db.user");
            password = dataBaseProperties.getProperty("db.password");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);

        DataBaseHandler dataBaseHandler = DataBaseHandler.builder()
                .forName(forName)
                .url(url)
                .properties(properties).build();

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(TelegramBot.builder()
                .botUserName(botUserName)
                .token(token)
                .creatorChatId(creatorChatId)
                .dataBaseHandler(dataBaseHandler).build());
    }
}
