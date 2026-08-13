package org.urfu.semyonovowa.bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Тонкий шлюз к Telegram API — единственное место, где вызывается {@code execute(...)}.
 * Централизует обработку {@link TelegramApiException} и рапорт об ошибке создателю бота.
 * Состояния не хранит: только ввод-вывод. В v10 отправка идёт через {@link TelegramClient}.
 */
public class TelegramGateway
{
    private final TelegramClient telegramClient;
    private final Long creatorChatId;

    public TelegramGateway(TelegramClient telegramClient, Long creatorChatId)
    {
        this.telegramClient = telegramClient;
        this.creatorChatId = creatorChatId;
    }

    /**
     * Отправляет текстовое сообщение.
     * @return отправленное сообщение, либо null при ошибке
     */
    public Message send(SendMessage message)
    {
        try
        {
            return telegramClient.execute(message);
        }
        catch (TelegramApiException e)
        {
            report(message.getChatId(), e);
            return null;
        }
    }

    /**
     * Отправляет фото.
     * @return отправленное сообщение, либо null при ошибке
     */
    public Message send(SendPhoto photo)
    {
        try
        {
            return telegramClient.execute(photo);
        }
        catch (TelegramApiException e)
        {
            report(photo.getChatId(), e);
            return null;
        }
    }

    public void edit(EditMessageText message)
    {
        try
        {
            telegramClient.execute(message);
        }
        catch (TelegramApiException e)
        {
            report(message.getChatId(), e);
        }
    }

    public void edit(EditMessageReplyMarkup message)
    {
        try
        {
            telegramClient.execute(message);
        }
        catch (TelegramApiException e)
        {
            report(message.getChatId(), e);
        }
    }

    public void delete(DeleteMessage message)
    {
        try
        {
            telegramClient.execute(message);
        }
        catch (TelegramApiException e)
        {
            report(message.getChatId(), e);
        }
    }

    private void report(String chatId, TelegramApiException e)
    {
        SendMessage error = SendMessage.builder()
                .chatId(creatorChatId)
                .text("Ошибка Telegram API (chatId=" + chatId + "):\n" + e.getMessage())
                .build();
        try
        {
            telegramClient.execute(error);
        }
        catch (TelegramApiException ignored)
        {
            // не зацикливаемся, если сообщение не доходит даже до создателя
        }
    }
}
