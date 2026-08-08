package org.urfu.semyonovowa.bot;

import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Тонкий шлюз к Telegram API — единственное место, где вызывается {@code execute(...)}.
 * Централизует обработку {@link TelegramApiException} и рапорт об ошибке создателю бота.
 * Состояния не хранит: только ввод-вывод.
 */
public class TelegramGateway
{
    private final DefaultAbsSender sender;
    private final Long creatorChatId;

    public TelegramGateway(DefaultAbsSender sender, Long creatorChatId)
    {
        this.sender = sender;
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
            return sender.execute(message);
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
            return sender.execute(photo);
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
            sender.execute(message);
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
            sender.execute(message);
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
            sender.execute(message);
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
            sender.execute(error);
        }
        catch (TelegramApiException ignored)
        {
            // не зацикливаемся, если сообщение не доходит даже до создателя
        }
    }
}
