package org.University.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {
    private String botUserName;
    private String botToken;

    public TelegramBot(String botUserName, String token)
    {
        this.botUserName = botUserName;
        this.botToken = token;
    }
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        User user = message.getFrom();
        String text = message.getText();
        Long chatId = user.getId();
        switch (text)
        {
            case "/start":
                sendMessage(chatId, "Привет! Я эхо-бот, который пишет " +
                    "сообщения в обратном порядке.");
                break;
            default:
                sendMessage(chatId, new StringBuilder(text).reverse().toString());
                break;
        }

    }

    public void sendMessage(Long chatId, String whatToSend)
    {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(whatToSend);
        try
        {
            execute(message);
        }
        catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return this.botUserName;
    }
    public String getBotToken(){return this.botToken;}
}
