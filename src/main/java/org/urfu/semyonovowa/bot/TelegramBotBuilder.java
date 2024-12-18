package org.urfu.semyonovowa.bot;

import org.urfu.semyonovowa.dataBase.DataBaseHandler;

public class TelegramBotBuilder {
    private String botUserName;
    private String token;
    private Long creatorChatId;
    private DataBaseHandler dataBaseHandler;
    public TelegramBotBuilder botUserName(String botUserName)
    {
        this.botUserName = botUserName;
        return this;
    }

    public TelegramBotBuilder token(String token)
    {
        this.token = token;
        return this;
    }
    public TelegramBotBuilder creatorChatId(Long creatorChatId)
    {
        this.creatorChatId = creatorChatId;
        return this;
    }
    public TelegramBotBuilder dataBaseHandler(DataBaseHandler dataBaseHandler)
    {
        this.dataBaseHandler = dataBaseHandler;
        return this;
    }
    public TelegramBot build() { return new TelegramBot(botUserName, token, creatorChatId, dataBaseHandler); }
}