package org.University.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.University.user.myUser;

import java.util.*;
/**
 * Класс, в котором изложена логика обработки взаимодействия с пользователями телеграмма
 */
public class TelegramBot extends TelegramLongPollingBot {
    private Map<String, myUser> allUsers = new HashMap<String, myUser>();
    private Map<String, String> userPairs = new HashMap<String, String>();
    private Map<Long, Stack<Message>> latestMessages = new HashMap<Long, Stack<Message>>();
    private final String botUserName;
    private final String botToken;

    public TelegramBot(String botUserName, String token)
    {
        this.botUserName = botUserName;
        this.botToken = token;
    }
    /**
     * Основной метод, разделяющий входные сообщения на текст и кнопки (callBackQuery)
     * @param update
     */
    @Override
    public void onUpdateReceived(Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        }
        else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }
    /**
     * метод для обработки всех нажатий на кнопки
     * @param update
     */
    private void handleCallbackQuery(Update update) {
        String callBackData = update.getCallbackQuery().getData();
        myUser currentUser = allUsers.get(update.getCallbackQuery().getFrom().getUserName());
        String pairUsername = userPairs.get(currentUser.getUsername());
        myUser pairUser = allUsers.get(pairUsername);

        switch (callBackData)
        {
            case "accept_Invite":
                deleteLastMessage(currentUser.getChatId());
                deleteLastMessage(pairUser.getChatId());
                sendMessage(pairUser.getChatId(), currentUser.getFirstName() +
                        " принял твое приглашение!");
                sendMessage(currentUser.getChatId(), "Ты принял приглашение");
                break;
            case "refuse_Invite":
                deleteLastMessage(currentUser.getChatId());
                deleteLastMessage(pairUser.getChatId());
                userPairs.remove(currentUser.getUsername(), pairUsername);
                userPairs.remove(pairUsername, currentUser.getUsername());
                sendMessage(pairUser.getChatId(), "К сожалению, " + currentUser.getFirstName() +
                        " отклонил твое приглашение.");
                sendMessage(currentUser.getChatId(), "Ты отклонил приглашение");
                break;
        }
    }
    /**
     * метод для обработки сообщений
     */
    /**
     * метод для обработки всех тектовых сообщений, вводимых пользователем
     * @param update
     */
    private void handleMessage(Update update) {
        Message message = update.getMessage();
        User currentUser = message.getFrom();
        String text = message.getText();

        String currentUserName = currentUser.getUserName();
        Long currentUserChatId = message.getChatId();
        String currentUserFirstName = currentUser.getFirstName();

        switch (text)
        {
            case "/start":
                if (!allUsers.containsKey(currentUser.getUserName()))
                {
                    allUsers.put(currentUserName, new myUser(currentUserChatId, currentUserName, currentUserFirstName, "in_lobby"));
                    sendGreetings(currentUserChatId);
                }
                break;
            case "Отменить приглашение❌":
                myUser pairUser = allUsers.get(userPairs.get(currentUserName));
                deleteLastMessage(pairUser.getChatId());
                deleteLastMessage(currentUserChatId);
                break;
            default:
                if (text.charAt(0) == '@')
                {
                    String invitedUserName = text.substring(1);
                    if (allUsers.containsKey(invitedUserName) && allUsers.get(currentUserName).getState().equals("in_lobby"))
                    {
                        userPairs.put(currentUserName, invitedUserName);
                        userPairs.put(invitedUserName, currentUserName);
                        myUser invitedUser = allUsers.get(invitedUserName);
                        sendInvite(invitedUser.getChatId(), currentUserFirstName);
                        sendWaitingMessage(currentUserChatId, "Приглашение отправлено." +
                                " Ожидай ответа!");
                    }
                    else if (!allUsers.containsKey(invitedUserName))
                    {
                      sendMessage(currentUserChatId, "Извини, но данного пользователя нет в системе.");
                    }
                }
        }
    }
    /**
     * Метод, высылающий пользователю сообщение-приветствие
     * @param chatId - куда отправить
     */
    private void sendGreetings(Long chatId)
    {
        sendMessage(chatId, "Добро пожаловать в морской бой!\n" +
                "В данный момент ты находишься в лобби. Для того чтобы начать" +
                " играть, пригласи друга.\n" +
                "Для этого введи мне его @username");
    }
    /**
     * метод отправки сообщений
     * @param chatId - куда отправить
     * @param whatToSend - что отправить
     */
    private void sendMessage(Long chatId, String whatToSend)
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

    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param chatId
     */
    private void deleteLastMessage(Long chatId) {
        if (latestMessages.containsKey(chatId) && !latestMessages.get(chatId).empty())
        {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(latestMessages.get(chatId).pop().getMessageId());
            try {
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Присылает сообщение для приглашающего и ожидающего пользователя, предоставляющее ему возможность отменить приглашение
     * @param chatId куда отправить
     * @param whatToSend что отправить
     */
    private void sendWaitingMessage(Long chatId, String whatToSend)
    {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(whatToSend);

        // Создание клавиатуры
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        // Создание кнопок
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Отменить приглашение❌"));
        keyboard.add(row1);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try
        {
            Message sendedMessage = execute(message);
            if (!latestMessages.containsKey(chatId))
            {
                Stack<Message> newStack = new Stack<Message>();
                newStack.add(sendedMessage);
                latestMessages.put(chatId, newStack);
            }
            else
            {
                latestMessages.get(chatId).add(sendedMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * метод для отправки приглашения на поединок
     * @param chatId - куда отправить
     * @param whoInvites - имя приглашающего пользователя
     */
    private void sendInvite(Long chatId, String whoInvites)
    {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(whoInvites + " приглашает тебя поиграть в морской бой!");

        InlineKeyboardMarkup inviteKeyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton acceptButton = new InlineKeyboardButton();
        acceptButton.setText("Принять приглашение✅");
        acceptButton.setCallbackData("accept_Invite");

        InlineKeyboardButton refuseButton = new InlineKeyboardButton();
        refuseButton.setText("Отклонить❌");
        refuseButton.setCallbackData("refuse_Invite");

        List<InlineKeyboardButton> row1 = new ArrayList<InlineKeyboardButton>();
        row1.add(acceptButton);

        List<InlineKeyboardButton> row2 = new ArrayList<InlineKeyboardButton>();
        row2.add(refuseButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<List<InlineKeyboardButton>>();
        rows.add(row1);
        rows.add(row2);

        inviteKeyboard.setKeyboard(rows);
        message.setReplyMarkup(inviteKeyboard);

        try
        {
            Message sendedMessage = execute(message);
            if (!latestMessages.containsKey(chatId))
            {
                Stack<Message> newStack = new Stack<Message>();
                newStack.add(sendedMessage);
                latestMessages.put(chatId, newStack);
            }
            else
            {
                latestMessages.get(chatId).add(sendedMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
    /**
     * геттер для имени бота
     * @return имя бота
     */
    @Override
    public String getBotUsername() {return this.botUserName;}
    /**
     * геттер для токена бота
     * @return токен бота
     */
    public String getBotToken(){return this.botToken;}
}
