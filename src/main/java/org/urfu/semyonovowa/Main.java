package org.urfu.semyonovowa;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.urfu.semyonovowa.bot.TelegramBotBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main
{
    public static Properties getBotProperties(File propertyFile) throws IOException
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
        try
        {
            Properties properties = getBotProperties(new File(args[0]));
            token = properties.getProperty("bot.token");
            botUserName = properties.getProperty("bot.name");
            creatorChatId = Long.parseLong(properties.getProperty("bot.creatorChatId"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TelegramBotBuilder()
                .botUserName(botUserName)
                .token(token)
                .creatorChatId(creatorChatId).build());
    }
}
