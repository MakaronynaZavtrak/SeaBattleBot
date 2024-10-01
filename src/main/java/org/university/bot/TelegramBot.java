package org.university.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.university.user.MyUser;

import java.util.*;
/**
 * Класс, в котором изложена логика обработки взаимодействия с пользователями телеграмма
 */
public class TelegramBot extends TelegramLongPollingBot
{
    private final Map<String, MyUser> allUsers = new HashMap<>();
    private final Map<String, String> userPairs = new HashMap<>();
    private final Map<Long, Stack<Message>> latestMessages = new HashMap<>();
    private final String botUserName;
    private final String botToken;

    public TelegramBot(String botUserName, String token)
    {
        this.botUserName = botUserName;
        this.botToken = token;
    }
    /**
     * Основной метод, разделяющий входные сообщения на текст и кнопки (callBackQuery)
     * @param update - полученные обновления
     */
    @Override
    public void onUpdateReceived(Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText())
        {
            handleMessage(update);
        }
        else if (update.hasCallbackQuery())
        {
            handleCallbackQuery(update);
        }
    }
    /**
     * метод для обработки всех нажатий на кнопки
     * @param update - входящие изменения
     */
    private void handleCallbackQuery(Update update)
    {
        CallbackQuery currentCallback = update.getCallbackQuery();
        String callBackData = currentCallback.getData();

        MyUser currentUser = allUsers.get(currentCallback.getFrom().getUserName());
        String pairUsername = userPairs.get(currentUser.getUsername());
        MyUser pairUser = allUsers.get(pairUsername);

        switch (callBackData)
        {
            case "accept_Invite" -> treatAcceptInvite(currentUser, pairUser);
            case "refuse_Invite" -> treatRefuseInvite(currentUser, pairUser);
        }
    }

    /**
     *
     */
    private void treatAcceptInvite(MyUser whoAccepts, MyUser whoInvites)
    {
        deleteLastMessage(whoAccepts.getChatId());
        deleteLastMessage(whoInvites.getChatId());
        sendMessage(whoInvites.getChatId(), whoAccepts.getFirstName() +
                " принял твое приглашение!");
        sendMessage(whoAccepts.getChatId(), "Ты принял приглашение");
    }

    /**
     *
     */
    private void treatRefuseInvite(MyUser whoAccepts, MyUser whoInvites)
    {
        deleteLastMessage(whoAccepts.getChatId());
        deleteLastMessage(whoInvites.getChatId());
        userPairs.remove(whoAccepts.getUsername(), whoInvites.getUsername());
        userPairs.remove(whoInvites.getUsername(), whoInvites.getUsername());
        sendMessage(whoInvites.getChatId(), "К сожалению, "
                + whoAccepts.getFirstName() +
                " отклонил твое приглашение.");
        sendMessage(whoAccepts.getChatId(), "Ты отклонил приглашение");
    }
    /**
     * метод для обработки всех тектовых сообщений, вводимых пользователем
     * @param update - входящие изменения
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
            case "/start" -> registerUserAndGreet(currentUserChatId, currentUserName, currentUserFirstName);
            case "Отменить приглашение❌" -> cancelInvite(currentUserName, currentUserChatId);
            default -> treatPairUserPresenceAndCreatePair(text, currentUserName);
        }
    }

    private void treatPairUserPresenceAndCreatePair(String text, String currentUserName)
    {
        if (text.charAt(0) == '@') {
            MyUser currentUser = allUsers.get(currentUserName);
            String invitedUserName = text.substring(1);
            if (allUsers.containsKey(invitedUserName) &&
                    currentUser.getState().equals("in_lobby"))
            {
                userPairs.put(currentUserName, invitedUserName);
                userPairs.put(invitedUserName, currentUserName);
                MyUser invitedUser = allUsers.get(invitedUserName);
                sendInvite(invitedUser.getChatId(), currentUser.getFirstName());
                sendWaitingMessage(currentUser.getChatId());
            }
            else if (!allUsers.containsKey(invitedUserName))
            {
                sendMessage(currentUser.getChatId(),
                        "Извини, но данного пользователя нет в системе.");
            }
        }
    }

    private void cancelInvite(String userName, Long userChatId)
    {
        MyUser pairUser = allUsers.get(userPairs.get(userName));
        deleteLastMessage(pairUser.getChatId());
        deleteLastMessage(userChatId);
    }

    private void registerUserAndGreet(Long userChatId, String userName, String userFirstName)
    {
        if (!allUsers.containsKey(userName))
        {
            allUsers.put(userName, new MyUser(
                    userChatId,
                    userName,
                    userFirstName,
                    "in_lobby"));
            sendGreetings(userChatId);
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
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(whatToSend).build();
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
     * @param chatId - в каком диалоге удалить
     */
    private void deleteLastMessage(Long chatId) {
        if (latestMessages.containsKey(chatId) && !latestMessages.get(chatId).empty())
        {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                            .chatId(chatId)
                            .messageId(latestMessages.get(chatId).pop().getMessageId()).build();
            try {
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Присылает сообщение для приглашающего и ожидающего пользователя,
     * предоставляющее ему возможность отменить приглашение
     *
     * @param chatId куда отправить
     */
    private void sendWaitingMessage(Long chatId)
    {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Приглашение отправлено. Ожидай ответа!").build();

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

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
                Stack<Message> newStack = new Stack<>();
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
        SendMessage message = SendMessage.builder()
                        .chatId(chatId)
                        .text(whoInvites + " приглашает тебя поиграть в морской бой!").build();

        InlineKeyboardMarkup inviteKeyboard = getInviteKeyboard();
        message.setReplyMarkup(inviteKeyboard);

        try
        {
            Message sendedMessage = execute(message);
            if (!latestMessages.containsKey(chatId))
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                latestMessages.put(chatId, newStack);
            }
            else
            {
                latestMessages.get(chatId).add(sendedMessage);
            }
        } catch (TelegramApiException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private InlineKeyboardMarkup getInviteKeyboard() {
        InlineKeyboardMarkup inviteKeyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton acceptButton = new InlineKeyboardButton();
        acceptButton.setText("Принять приглашение✅");
        acceptButton.setCallbackData("accept_Invite");

        InlineKeyboardButton refuseButton = new InlineKeyboardButton();
        refuseButton.setText("Отклонить❌");
        refuseButton.setCallbackData("refuse_Invite");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(acceptButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(refuseButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        inviteKeyboard.setKeyboard(rows);
        return inviteKeyboard;
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
    @Override
    public String getBotToken(){return this.botToken;}
}
