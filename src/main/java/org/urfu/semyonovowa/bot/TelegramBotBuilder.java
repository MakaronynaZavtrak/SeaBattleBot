package org.urfu.semyonovowa.bot;

public class TelegramBotBuilder {
    private String botUserName;
    private String token;
    public TelegramBotBuilder setBotUserName(String botUserName)
    {
        this.botUserName = botUserName;
        return this;
    }

    public TelegramBotBuilder setToken(String token)
    {
        this.token = token;
        return this;
    }
    public TelegramBot createTelegramBot() {
        return new TelegramBot(botUserName, token);
    }
}