package org.university.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.university.field.TelegramField;
import org.university.game.Game;
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
    Map<String, Game> games = new HashMap<>();
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
        MyUser currentUser = allUsers.get(update.getCallbackQuery().getFrom().getUserName());

        switch (currentUser.getState())
        {
            case "in_lobby" -> lobbyCallbackQueryHandler(currentUser, update);
            case "is_setting_linCore" -> linCoreSettingCallbackQueryHandler(currentUser, update);
            case "is_setting_cruiser" -> cruiserSettingCallbackQueryHandler(currentUser, update);
            case "is_setting_esminez1" -> esminez1SettingCallbackQueryHandler(currentUser, update);
            case "is_setting_esminez2" -> esminez2SettingCallbackQueryHandler(currentUser, update);
            case "is_setting_boat1" -> boat1SettingCallbackQueryHandler(currentUser, update);
            case "is_setting_boat2" -> boat2SettingCallbackQueryHandler(currentUser, update);
            case "is_setting_boat3" -> boat3SettingCallbackQueryHandler(currentUser, update);
            case "is_moving" -> movingHandler(currentUser, update);
            case "has_finished_the_game" -> revengeHandler(currentUser, update);
            case "wanna_to_replay" -> revengeHandler(currentUser, update);
        }
    }

    /**
     * метод для обработки реванша
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void revengeHandler(MyUser currentUser, Update update)
    {
        String callBackData = update.getCallbackQuery().getData();
        switch (callBackData)
        {
            case "wanna_to_replay" -> replayHandler(currentUser);
            case "wanna_exit" -> leaveAfterGame(currentUser);
        }
    }

    /**
     * метод для обработки разрыва сеанса между двумя игроками после игры
     * @param currentUser - текущий пользователь 
     */
    private void leaveAfterGame(MyUser currentUser)
    {
        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));

        currentUser.setState("in_lobby");
        pairUser.setState("in_lobby");
        deleteLastMessage(currentUser.getChatId());
        deleteLastMessage(currentUser.getChatId());
        deleteLastMessage(currentUser.getChatId());
        deleteLastMessage(currentUser.getChatId());

        deleteLastMessage(pairUser.getChatId());
        deleteLastMessage(pairUser.getChatId());
        deleteLastMessage(pairUser.getChatId());
        deleteLastMessage(pairUser.getChatId());

        sendMessage(currentUser.getChatId(), "Ты снова оказался в лобби и " +
                "через @ можешь пригласить играть другого пользователя");
        sendMessage(pairUser.getChatId(), "К сожалению, " + currentUser.getFirstName()
                + " не хочет играть.\nТы снова оказался в лобби и "
                + "через @ можешь пригласить играть другого пользователя");
    }

    /**
     * метод для обработки начала новой игры с сохранием исходной пары игроков
     * @param currentUser - текущий пользователь
     */
    private void replayHandler(MyUser currentUser)
    {
        if (currentUser.getState().equals("wanna_to_replay"))
            return;

        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));

        currentUser.setState("wanna_to_replay");

        if (pairUser.getState().equals("wanna_to_replay"))
        {
            currentUser.setState("is_setting_lin-core");
            pairUser.setState("is_setting_lin-core");

            Game newGame = new Game(pairUser, currentUser);
            games.put(currentUser.getUsername(), newGame);
            games.put(pairUser.getUsername(), newGame);

            deleteLastMessage(currentUser.getChatId());
            deleteLastMessage(currentUser.getChatId());
            deleteLastMessage(currentUser.getChatId());
            deleteLastMessage(currentUser.getChatId());

            deleteLastMessage(pairUser.getChatId());
            deleteLastMessage(pairUser.getChatId());
            deleteLastMessage(pairUser.getChatId());
            deleteLastMessage(pairUser.getChatId());

            sendField(pairUser.getChatId(),
                    newGame.getOwnFields().get(pairUser.getUsername()),
                    "Расставь линкор (корабль с 4-мя жизнями)");
            sendField(currentUser.getChatId(),
                    newGame.getOwnFields().get(currentUser.getUsername()),
                    "Расставь линкор (корабль с 4-мя жизнями)");
        }
    }

    /**
     * метод для обработки любых ходов во время игры
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void movingHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());
        String pairUsername = userPairs.get(currentUser.getUsername());
        String callBackData = update.getCallbackQuery().getData();
        MyUser pairUser = allUsers.get(pairUsername);

        if (currentGame.getOwnFields().get(pairUsername).getUsedCages().contains(callBackData.substring(1)))
            return;

        String info = currentGame.attack(currentUser, callBackData.substring(1));
        currentGame.getOwnFields().get(pairUsername).getUsedCages().add(callBackData.substring(1));

        String pairInfo;
        switch (info)
        {
            case "Поздравляю, ты победил!\uD83C\uDFC6" -> pairInfo = "К сожалению, ты проиграл :(";
            case "Ты промахнулся :(" -> pairInfo = "Противник промахнулся";
            case "Ты нанес ранение!" -> pairInfo = "Противник ранил твой корабль";
            case "Ты потопил вражеский корабль!" -> pairInfo = "Противник потопил твой корабль!";
            default -> pairInfo = "undefined";
        }

        if (!info.equals("Поздравляю, ты победил!\uD83C\uDFC6"))
            treatNotWinMovement(currentUser, pairUser, currentGame, info, pairInfo);
        else
            treatWinMovement(currentUser, pairUser, currentGame, info, pairInfo);
    }

    /**
     * /**
     * метод для обработки ситуации, когда currentUser победил
     * @param currentUser - текущий пользователь-победитель
     * @param pairUser - проигравший пользователь, играющий в паре с currentUser
     * @param currentGame - текущая играя, в которой оба user'а играют
     * @param info - информация об игровом процессе относительно текущего пользователя
     * @param pairInfo - информация об игровом процессе относительно второго пользователя
     */
    private void treatWinMovement(MyUser currentUser, MyUser pairUser,
                                  Game currentGame, String info, String pairInfo)
    {
        deleteLastMessage(currentUser.getChatId());
        deleteLastMessage(currentUser.getChatId());

        deleteLastMessage(pairUser.getChatId());
        deleteLastMessage(pairUser.getChatId());

        Message currentPeek = latestMessages.get(pairUser.getChatId()).pop();

        editField(currentUser.getChatId(),
                latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                currentGame.getEnemyFields().get(currentUser.getUsername()));
        editField(pairUser.getChatId(),
                latestMessages.get(pairUser.getChatId()).peek().getMessageId(),
                currentGame.getOwnFields().get(pairUser.getUsername()));

        latestMessages.get(pairUser.getChatId()).add(currentPeek);

        currentUser.setState("has_finished_the_game");
        pairUser.setState("has_finished_the_game");

        sendMessage(currentUser.getChatId(), info);
        sendMessage(pairUser.getChatId(), pairInfo);

        games.remove(currentUser.getUsername());
        games.remove(pairUser.getUsername());

        sendRepeatGame(currentUser.getChatId());
        sendRepeatGame(pairUser.getChatId());
    }

    /**
     * метод для обработки ситуации, когда при очередном шаге победитель не выявился
     * @param currentUser - текущий пользователь
     * @param pairUser - пользователь, состоящий в паре с текущим
     * @param currentGame - игра, в которой текущий и парный ему пользователи играют
     * @param info - информация об игровом процессе относительно текущего пользователя
     * @param pairInfo - информация об игровом процессе относительно второго пользователя
     */
    private void treatNotWinMovement(MyUser currentUser, MyUser pairUser,
                                     Game currentGame, String info, String pairInfo)
    {

        Message currentUserLastMsg = latestMessages.get(currentUser.getChatId()).peek();
        Message pairUserLastMsg = latestMessages.get(pairUser.getChatId()).peek();

        if (!currentGame.isFirstMove())
            treatNotFirstMovement(currentUser, pairUser,
                    info, pairInfo,
                    currentUserLastMsg, pairUserLastMsg);
        else
            treatFirstMovement(currentUser, pairUser,
                    info, pairInfo,
                    currentGame, currentUserLastMsg, pairUserLastMsg);

        Message currentTurn = latestMessages.get(currentUser.getChatId()).pop();
        Message currentEvent = latestMessages.get(currentUser.getChatId()).pop();

        Message pairTurn = latestMessages.get(pairUser.getChatId()).pop();
        Message pairEvent = latestMessages.get(pairUser.getChatId()).pop();

        Message currentPeek = latestMessages.get(pairUser.getChatId()).pop();

        editField(currentUser.getChatId(),
                latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                currentGame.getEnemyFields().get(currentUser.getUsername()));
        editField(pairUser.getChatId(),
                latestMessages.get(pairUser.getChatId()).peek().getMessageId(),
                currentGame.getOwnFields().get(pairUser.getUsername()));

        latestMessages.get(currentUser.getChatId()).add(currentEvent);
        latestMessages.get(currentUser.getChatId()).add(currentTurn);

        latestMessages.get(pairUser.getChatId()).add(currentPeek);
        latestMessages.get(pairUser.getChatId()).add(pairEvent);
        latestMessages.get(pairUser.getChatId()).add(pairTurn);
    }

    /**
     * метод для обработки самого перва шага игры
     * @param currentUser - текущий пользователь
     * @param pairUser - пользователь, играющий в паре с текущим
     * @param info - информация об игровом процессе относительно текущего пользователя
     * @param pairInfo - информация об игровом процессе относительно второго пользователя
     * @param currentGame - игра, в которой текущий и парный ему пользователи играют
     * @param currentUserLastMsg - последнее сообщение, пришедшее текущему пользователю
     * @param pairUserLastMsg - последнее сообщение, пришедшее второму пользователю, находящемуся в паре с текущим
     */
    private void treatFirstMovement(MyUser currentUser, MyUser pairUser,
                                    String info, String pairInfo,
                                    Game currentGame, Message currentUserLastMsg, Message pairUserLastMsg)
    {
        editMessage(currentUser.getChatId(), currentUserLastMsg, info);
        currentUserLastMsg.setText(info);

        editMessage(pairUser.getChatId(), pairUserLastMsg, pairInfo);

        pairUserLastMsg.setText(pairInfo);

        String currentWhoseTurn;
        String pairWhoseTurn;

        if (info.equals("Ты промахнулся :("))
        {
            currentWhoseTurn = "Сейчас ходит противник";
            pairWhoseTurn = "Сейчас ходишь ты";
            pairUser.setState("is_moving");
            currentUser.setState("is_waiting");
        }
        else
        {
            currentWhoseTurn = "Сейчас ходишь ты";
            pairWhoseTurn = "Сейчас ходит противник";
        }
        sendMessage(currentUser.getChatId(), currentWhoseTurn);
        sendMessage(pairUser.getChatId(), pairWhoseTurn);
        currentGame.setFirstMove(false);
    }

    /**
     * обработка всех остальных непервых ходов в игре
     * @param currentUser - текущий пользователь
     * @param pairUser - пользователь, находящийся в паре с текущим
     * @param info - информация об игровом процессе относительно текущего пользователя
     * @param pairInfo - информация об игровом процессе относительно второго пользователя
     * @param currentUserLastMsg - последнее сообщение, пришедшее текущему пользователю
     * @param pairUserLastMsg - последнее сообщение, пришедшее второму пользователю, находящемуся в паре с текущим
     */
    private void treatNotFirstMovement(MyUser currentUser, MyUser pairUser,
                                       String info, String pairInfo,
                                       Message currentUserLastMsg, Message pairUserLastMsg)
    {
        latestMessages.get(currentUser.getChatId()).pop();
        Message currentInfoMsg = latestMessages.get(currentUser.getChatId()).peek();

        latestMessages.get(pairUser.getChatId()).pop();
        Message pairInfoMsg = latestMessages.get(pairUser.getChatId()).peek();

        if (!currentInfoMsg.getText().equals(info))
        {
            editMessage(currentUser.getChatId(), currentInfoMsg, info);
            currentInfoMsg.setText(info);

            editMessage(pairUser.getChatId(), pairInfoMsg, pairInfo);
            pairInfoMsg.setText(pairInfo);
        }

        if (info.equals("Ты промахнулся :("))
        {
            pairUser.setState("is_moving");
            currentUser.setState("is_waiting");

            editMessage(currentUser.getChatId(), currentUserLastMsg, "Сейчас ходит противник");
            currentUserLastMsg.setText("Сейчас ходит противник");

            editMessage(pairUser.getChatId(), pairUserLastMsg, "Сейчас ходишь ты");
            pairUserLastMsg.setText("Сейчас ходишь ты");
        }
        latestMessages.get(currentUser.getChatId()).add(currentUserLastMsg);
        latestMessages.get(pairUser.getChatId()).add(pairUserLastMsg);
    }

    /**
     * метод для отправки сообщения на повторную игру
     * @param chatId - куда отправить
     */
    public void sendRepeatGame(Long chatId)
    {
        SendMessage message = new SendMessage();
        message.setText("Хочешь сыграть с этим игроком еще раз?");
        message.setChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton button_yes = new InlineKeyboardButton();
        button_yes.setText("Сыграть еще раз✅");
        button_yes.setCallbackData("wanna_to_replay");

        InlineKeyboardButton button_no = new InlineKeyboardButton();
        button_no.setText("Выйти в лобби ожидания❌");
        button_no.setCallbackData("wanna_exit");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(button_yes);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(button_no);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        inlineKeyboardMarkup.setKeyboard(rows);

        message.setReplyMarkup(inlineKeyboardMarkup);

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
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    /**
     * метод для редактирования сообщений
     * @param chatId - где отредактировать
     * @param message - какое сообщение отредактировать
     * @param editedText - на что отредактировать
     */
    public void editMessage(Long chatId, Message message, String editedText)
    {
        if (message.getText().equals(editedText))
            return;
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText(editedText);
        try
        {
            execute(editMessageText);
        }
        catch (TelegramApiException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * метод для установки третьего одножизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - имзенения
     */
    private void boat3SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());

        if (currentGame.setCage(
                update.getCallbackQuery().getData(),
                currentUser,
                currentGame.getShips().get(currentUser.getUsername()).get(6)))
        {
            if (currentGame.getShips().get(currentUser.getUsername()).get(6).getCoordinatesSet().size() ==
                    currentGame.getShips().get(currentUser.getUsername()).get(6).getLives())
            {
                currentUser.setState("is_ready_to_play");
                deleteLastMessage(currentUser.getChatId());
                MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));
                if (!pairUser.getState().equals("is_ready_to_play"))
                    sendField(currentUser.getChatId(), currentGame.getOwnFields().get(currentUser.getUsername()),
                            "Подожди, твой противник еще расставляет корабли");
                else
                {
                    deleteLastMessage(pairUser.getChatId());

                    deleteLastMessage(pairUser.getChatId());
                    deleteLastMessage(currentUser.getChatId());
                    sendField(currentUser.getChatId(),
                            currentGame.getOwnFields().get(currentUser.getUsername()),
                            "Твое поле:");
                    sendField(pairUser.getChatId(),
                            currentGame.getOwnFields().get(pairUser.getUsername()),
                            "Твое поле:");
                    sendField(currentUser.getChatId(),
                            currentGame.getEnemyFields().get(currentUser.getUsername()),
                            "Поле твоего противника:");
                    sendField(pairUser.getChatId(),
                            currentGame.getEnemyFields().get(pairUser.getUsername()),
                            "Поле твоего противника:");

                    if (currentUser.getUsername().equals(currentGame.getCreator().getUsername()))
                    {
                        currentUser.setState("is_moving");
                        pairUser.setState("is_waiting");
                        sendMessage(currentUser.getChatId(), "Cейчас ходишь ты");
                        sendMessage(pairUser.getChatId(), "Cейчас ходит противник");
                    }
                    else
                    {
                        pairUser.setState("is_moving");
                        currentUser.setState("is_waiting");
                        sendMessage(pairUser.getChatId(), "Cейчас ходишь ты");
                        sendMessage(currentUser.getChatId(), "Cейчас ходит противник");
                    }
                }
            }
        }
    }

    /**
     * метод для установки второго одножизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void boat2SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());

        if (currentGame.setCage(
                update.getCallbackQuery().getData(),
                currentUser,
                currentGame.getShips().get(currentUser.getUsername()).get(5))
        )
        {
            if (currentGame.getShips().get(currentUser.getUsername()).get(5).getCoordinatesSet().size() ==
                    currentGame.getShips().get(currentUser.getUsername()).get(5).getLives())
                currentUser.setState("is_setting_boat3");
            editField(
                    currentUser.getChatId(),
                    latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUsername()));
        }
    }

    /**
     * метод для установки первого одножизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void boat1SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());

        if (currentGame.setCage(
                update.getCallbackQuery().getData(),
                currentUser,
                currentGame.getShips().get(currentUser.getUsername()).get(4))
            )
        {
            if (currentGame.getShips().get(currentUser.getUsername()).get(4).getCoordinatesSet().size() ==
                    currentGame.getShips().get(currentUser.getUsername()).get(4).getLives())
                currentUser.setState("is_setting_boat2");
            editField(
                    currentUser.getChatId(),
                    latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUsername()));
        }
    }

    /**
     * метод для установки второго двухжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void esminez2SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());

        if (currentGame.setCage(
                update.getCallbackQuery().getData(),
                currentUser,
                currentGame.getShips().get(currentUser.getUsername()).get(3))
            )
        {
            if (currentGame.getShips().get(currentUser.getUsername()).get(3).getCoordinatesSet().size() ==
                    currentGame.getShips().get(currentUser.getUsername()).get(3).getLives())
            {
                currentUser.setState("is_setting_boat1");
                deleteLastMessage(currentUser.getChatId());
                sendField(currentUser.getChatId(), currentGame.getOwnFields().get(currentUser.getUsername()),
                        "Расставь три катера (корабль с одной жизнью)");
                return;
            }
            editField(
                    currentUser.getChatId(),
                    latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUsername()));
        }
    }

    /**
     * метод для установки первого двухжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void esminez1SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());

        if (currentGame.setCage(
                update.getCallbackQuery().getData(),
                currentUser,
                currentGame.getShips().get(currentUser.getUsername()).get(2))
            )
        {
            if (currentGame.getShips().get(currentUser.getUsername()).get(2).getCoordinatesSet().size() ==
                    currentGame.getShips().get(currentUser.getUsername()).get(2).getLives())
                currentUser.setState("is_setting_esminez2");
            editField(
                    currentUser.getChatId(),
                    latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUsername()));
        }
    }

    /**
     * метод для установки единственного трехжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void cruiserSettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());
        if (currentGame.setCage(
                update.getCallbackQuery().getData(),
                currentUser,
                currentGame.getShips().get(currentUser.getUsername()).get(1))
            )
        {
            if (currentGame.getShips().get(currentUser.getUsername()).get(1).getCoordinatesSet().size() ==
                    currentGame.getShips().get(currentUser.getUsername()).get(1).getLives())
            {
                currentUser.setState("is_setting_esminez1");
                deleteLastMessage(currentUser.getChatId());
                sendField(currentUser.getChatId(), currentGame.getOwnFields().get(currentUser.getUsername()),
                        "Расставь двух эсминцов (корабль с 2-мя жизнями)");
                return;
            }
            editField(
                    currentUser.getChatId(),
                    latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUsername()));
        }
    }

    /**
     * метод для установки единственного четырезжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void linCoreSettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());
        if (currentGame.setCage(update.getCallbackQuery().getData(),
                currentUser,
                currentGame.getShips().get(currentUser.getUsername()).get(0)))
        {
            if (currentGame.getShips().get(currentUser.getUsername()).get(0).getCoordinatesSet().size() ==
                    currentGame.getShips().get(currentUser.getUsername()).get(0).getLives())
            {
                currentUser.setState("is_setting_cruiser");
                deleteLastMessage(currentUser.getChatId());
                sendField(currentUser.getChatId(), currentGame.getOwnFields().get(currentUser.getUsername()),
                        "Расставь один крейсер (корабль с 3-мя жизнями)");
                return;
            }
            editField(
                    currentUser.getChatId(),
                    latestMessages.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUsername()));
        }
    }

    /**
     * метод для редактирования поля
     * @param chatId - в каком диалоге редактировать
     * @param messageId - в каком сообщении
     * @param field - на какое поле заменить исходное
     */
    public void editField(Long chatId, Integer messageId, TelegramField field)
    {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(chatId);
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup(field.getKeyboardMarkup());
        try
        {
            execute(editMessageReplyMarkup);
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * метод для обработки callback-действий, когда currentUser находится в лобби
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void lobbyCallbackQueryHandler(MyUser currentUser, Update update)
    {
        String callBackData = update.getCallbackQuery().getData();
        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));

        switch (callBackData)
        {
            case "accept_Invite" -> treatAcceptInvite(currentUser, pairUser);
            case "refuse_Invite" -> treatRefuseInvite(currentUser, pairUser);
        }
        currentUser.setInvitedFlag(false);
        pairUser.setInvitedFlag(false);
    }

    /**
     * метод для обработки принятия приглашения
     * @param whoAccepts - кто принимает
     * @param whoInvites - кто приглашает
     */
    private void treatAcceptInvite(MyUser whoAccepts, MyUser whoInvites)
    {
        deleteLastMessage(whoAccepts.getChatId());
        deleteLastMessage(whoInvites.getChatId());
        whoAccepts.setState("is_setting_linCore");
        whoInvites.setState("is_setting_linCore");
        Game newGame = new Game(whoInvites, whoAccepts);
        games.put(whoAccepts.getUsername(), newGame);
        games.put(whoInvites.getUsername(), newGame);
        sendField(whoAccepts.getChatId(), newGame.getOwnFields().get(whoAccepts.getUsername()),
                "Расставь линкор (корабль с 4-мя жизнями)");
        sendField(whoInvites.getChatId(), newGame.getOwnFields().get(whoInvites.getUsername()),
                "Расставь линкор (корабль с 4-мя жизнями)");
    }
    /**
     * метод для отправки игрового поля
     * @param chatId куда отправить
     * @param field какое поле отправить
     * @param text надпись над полем
     */
    public void sendField(Long chatId, TelegramField field, String text)
    {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setReplyMarkup(field.getKeyboardMarkup());
        if (!text.isEmpty())
        {
            message.setText(text);
        }
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
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * метод для обработки отклонения приглашения
     * @param whoRefuses - кто отклоняет
     * @param whoInvites - кто приглашает
     */
    private void treatRefuseInvite(MyUser whoRefuses, MyUser whoInvites)
    {
        deleteLastMessage(whoRefuses.getChatId());
        deleteLastMessage(whoInvites.getChatId());
        userPairs.remove(whoRefuses.getUsername(), whoInvites.getUsername());
        userPairs.remove(whoInvites.getUsername(), whoInvites.getUsername());
        sendMessage(whoInvites.getChatId(), whoRefuses.getFirstName() +
                " отклонил твое приглашение.");
        sendMessage(whoRefuses.getChatId(), "Ты отклонил приглашение");
    }
    /**
     * метод для обработки всех тектовых сообщений, вводимых пользователем
     * @param update - входящие изменения
     */
    private void handleMessage(Update update)
    {
        if (allUsers.containsKey(update.getMessage().getFrom().getUserName()))
        {
            MyUser currentUser = allUsers.get(update.getMessage().getFrom().getUserName());

            switch (currentUser.getState())
            {
                case "in_lobby" -> lobbyMessageHandler(currentUser, update);
            }
        }
        else if (update.getMessage().getText().equals("/start"))
        {
            Long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getFrom().getUserName();
            String firstName = update.getMessage().getFrom().getFirstName();
            registerUserAndGreet(chatId, userName, firstName);
        }
    }

    /**
     * метод для обработки текстовых сообщений пользователя, находящегося в лобби
     * @param currentUser - текущий ползователь
     * @param update - изменения
     */
    private void lobbyMessageHandler(MyUser currentUser, Update update)
    {
        Message message = update.getMessage();
        String text = message.getText();

        switch (text)
        {
            case "Отменить приглашение❌" -> cancelInvite(currentUser);
            //TODO: /permute, /exit
            default -> treatPairUserPresenceAndCreatePair(text, currentUser);
        }
    }

    /**
     * метод для проверки наличия в базе введенного текущим пользователем тега другого пользователя и
     * в случае успеха создание из них игровой пары
     * @param text - текстовое сообщение
     * @param currentUser - текущий пользователь
     */
    private void treatPairUserPresenceAndCreatePair(String text, MyUser currentUser)
    {
        if (text.charAt(0) == '@')
        {
            String invitedUserName = text.substring(1);
            if (!allUsers.containsKey(invitedUserName))
            {
                sendMessage(currentUser.getChatId(),
                        "Извини, данного пользователя нет в системе.");
                return;
            }
            MyUser invitedUser = allUsers.get(invitedUserName);
            if (invitedUser.getState().equals("in_lobby") && !invitedUser.isInvited())
            {
                userPairs.put(currentUser.getUsername(), invitedUserName);
                userPairs.put(invitedUserName, currentUser.getUsername());
                sendInvite(invitedUser.getChatId(), currentUser.getFirstName());
                sendWaitingMessage(currentUser.getChatId());
                //TODO: пока что заглушкой от множественного приглашения является поле invtiedFlag в классе MyUser
                //TODO вскоре планируется убрать эту заглушку и сделать очередь из приглашений
                invitedUser.setInvitedFlag(true);
                currentUser.setInvitedFlag(true);
                return;
            }
            if (!currentUser.getState().equals("in_lobby"))
            {
                sendMessage(currentUser.getChatId(),
                        "Извини, данный пользователь уже с кем-то играет");
                return;
            }
            if (invitedUser.isInvited())
            {
             sendMessage(currentUser.getChatId(),
                     "Извини, но данный пользователь состоит с кем-то в приглашении");
            }
        }
    }

    /**
     * метод для отмены приглашения у приглашающего пользователя
     * @param invitingUser - приглашающий пользователь
     */
    private void cancelInvite(MyUser invitingUser)
    {
        MyUser pairUser = allUsers.get(userPairs.get(invitingUser.getUsername()));
        deleteLastMessage(pairUser.getChatId());
        deleteLastMessage(invitingUser.getChatId());
        userPairs.remove(invitingUser.getUsername());
        userPairs.remove(pairUser.getUsername());
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
            sendGreetings(userChatId, userFirstName);
        }
    }

    /**
     * Метод, высылающий пользователю сообщение-приветствие
     * @param chatId - куда отправить
     * @param userFirstName - имя для обращения к пользователю
     */
    public void sendGreetings(Long chatId, String userFirstName)
    {
        InputFile picture = new InputFile("https://www.blast.hk/data/avatars/o/345/345147.jpg?1636698629");
        sendPhoto(chatId, picture, "Привет, " + userFirstName + "! Добро пожаловать в морской ♂boy♂\n"+
                "Сейчас ты находишься в лобби ожидания. Для того, чтобы начать игру, введи " +
                "мне @userName своего друга (обязательно с @), с которым хочешь сыграть, либо же дождись," +
                " когда это сделает он.");
    }

    /**
     * метод для отправки приветственной открытки
     * @param chatId - куда отправить
     * @param photo - открытка
     * @param caption - надпись для открытки
     */
    public void sendPhoto(Long chatId, InputFile photo, String caption)
    {
        SendPhoto message = new SendPhoto();
        message.setPhoto(photo);
        message.setChatId(chatId);

        if (!caption.isEmpty())
            message.setCaption(caption);

        try
        {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
        }
        catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param chatId - в каком диалоге удалить
     */
    private void deleteLastMessage(Long chatId)
    {
        if (latestMessages.containsKey(chatId) && !latestMessages.get(chatId).empty())
        {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                            .chatId(chatId)
                            .messageId(latestMessages.get(chatId).pop().getMessageId()).build();
            try
            {
                execute(deleteMessage);
            }
            catch (TelegramApiException e)
            {
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
        }
        catch (TelegramApiException e)
        {
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
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    /**
     * метод для формирования поля
     * @return поле для приглашаемого игрока
     */
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
