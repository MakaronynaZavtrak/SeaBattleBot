package org.urfu.semyonovowa.bot;

public class TelegramBotBuilder {
    private String botUserName;
    private String token;
    private Long creatorChatId;
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
    public TelegramBot build() { return new TelegramBot(botUserName, token, creatorChatId); }
}