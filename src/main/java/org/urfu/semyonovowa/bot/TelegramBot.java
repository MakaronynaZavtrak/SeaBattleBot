package org.urfu.semyonovowa.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.urfu.semyonovowa.dataBase.DataBaseHandler;
import org.urfu.semyonovowa.dataBase.Query;
import org.urfu.semyonovowa.field.TelegramField;
import org.urfu.semyonovowa.game.Game;
import org.urfu.semyonovowa.game.MovingInformation;
import org.urfu.semyonovowa.game.MovingInformationForBothPlayers;
import org.urfu.semyonovowa.ship.Ship;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.Rank;
import org.urfu.semyonovowa.user.RankList;
import org.urfu.semyonovowa.user.State;

import java.sql.SQLException;
import java.util.*;
/**
 * Класс, в котором изложена логика обработки взаимодействия с пользователями телеграмма
 */
public class TelegramBot extends TelegramLongPollingBot
{
    private final Map<String, MyUser> allUsers;
    private final Map<String, String> userPairs;
    private final Map<Long, Stack<Message>> messageStacks;
    private final Map<Long, Map<String, Integer>> invitationMessages;
    private final Map<String, MyUser> invitedUsers;
    private final Map<String, Game> games;
    private final String botUserName;
    private final String botToken;
    private final Long creatorChatId;
    private final DataBaseHandler dataBaseHandler;
    public TelegramBot(String botUserName, String token, Long creatorChatId, DataBaseHandler dataBaseHandler)
    {
        super(token);
        this.botUserName = botUserName;
        this.botToken = token;
        this.creatorChatId = creatorChatId;
        this.allUsers = new HashMap<>();
        this.userPairs = new HashMap<>();
        this.messageStacks = new HashMap<>();
        this.invitationMessages = new HashMap<>();
        this.invitedUsers = new HashMap<>();
        this.games = new HashMap<>();
        this.dataBaseHandler = dataBaseHandler;
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
            try
            {
                handleMessage(update);
            }
            catch (SQLException | ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
        else if (update.hasCallbackQuery())
        {
            try
            {
                handleCallbackQuery(update);
            }
            catch (SQLException | ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    /**
     * метод для обработки всех нажатий на кнопки
     * @param update - входящие изменения
     */
    private void handleCallbackQuery(Update update) throws SQLException, ClassNotFoundException {
        MyUser currentUser = allUsers.get(update.getCallbackQuery().getFrom().getUserName());
        switch (currentUser.getState())
        {
            case State.IN_LOBBY -> lobbyCallbackQueryHandler(currentUser, update);
            case State.LINCORE_SETTING -> linCoreSettingCallbackQueryHandler(currentUser, update);
            case State.CRUISER_SETTING -> cruiserSettingCallbackQueryHandler(currentUser, update);
            case State.ESMINEZ_1_SETTTING -> esminez1SettingCallbackQueryHandler(currentUser, update);
            case State.ESMINEZ_2_SETTTING -> esminez2SettingCallbackQueryHandler(currentUser, update);
            case State.BOAT_1_SETTING -> boat1SettingCallbackQueryHandler(currentUser, update);
            case State.BOAT_2_SETTING -> boat2SettingCallbackQueryHandler(currentUser, update);
            case State.BOAT_3_SETTING -> boat3SettingCallbackQueryHandler(currentUser, update);
            case State.MOVING -> movingHandler(currentUser, update);
            case State.FINISHED_GAME, State.WANT_TO_REPLAY -> revengeHandler(currentUser, update);
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
            case "want_to_replay" -> replayHandler(currentUser);
            case "want_to_exit" -> leaveAfterGame(currentUser);
        }
    }

    private void leaveFromCurrentGame(MyUser currentUser, int times)
    {
        commonLeaveGame(currentUser, times, "@", " сбежал с поля битвы!");
    }

    private void commonLeaveGame(MyUser currentUser, int times, String textBefore, String textAfter)
    {
        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUserName()));
        commonCleanTrailsEndGame(currentUser, times, "");
        commonCleanTrailsEndGame(pairUser, times, textBefore + currentUser.getUserName() + textAfter);
    }
    private void commonCleanTrailsEndGame(MyUser user, int times, String message)
    {
        user.setState(State.IN_LOBBY);
        deleteLastMessage(user, times);

        if (!message.isEmpty())
            sendMessageWithNoSave(user.getChatId(), message);
        sendMainLobbyMenu(user);
    }

    /**
     * метод для обработки разрыва сеанса между двумя игроками после игры
     * @param currentUser - текущий пользователь 
     */
    private void leaveAfterGame(MyUser currentUser)
    {
        commonLeaveGame(currentUser, 4, "К сожалению, @", " больше не хочет играть.");
    }

    /**
     * метод для обработки начала новой игры с сохранием исходной пары игроков
     * @param currentUser - текущий пользователь
     */
    private void replayHandler(MyUser currentUser)
    {
        if (currentUser.getState().equals(State.WANT_TO_REPLAY))
            return;

        currentUser.setState(State.WANT_TO_REPLAY);

        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUserName()));
        if (pairUser.getState().equals(State.WANT_TO_REPLAY))
        {
            Game newGame = new Game(pairUser, currentUser);
            prepareForReplay(currentUser, newGame);
            prepareForReplay(pairUser, newGame);
        }
    }
    private void prepareForReplay(MyUser user, Game game)
    {
        user.setState(State.LINCORE_SETTING);
        games.put(user.getUserName(), game);
        deleteLastMessage(user, 4);
        sendField(user, game.getOwnFields().get(user.getUserName()), TIP.LINCORE);
    }
    /**
     * метод для обработки любых ходов во время игры
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void movingHandler(MyUser currentUser, Update update) throws ClassNotFoundException {
        Game currentGame = games.get(currentUser.getUserName());
        String coordinates = update.getCallbackQuery().getData().substring(1);
        String pairUsername = userPairs.get(currentUser.getUserName());
        Set<String> usedCages = currentGame.getOwnFields().get(pairUsername).getUsedCages();

        if (usedCages.contains(coordinates))
            return;
        MovingInformationForBothPlayers information = currentGame.attack(currentUser, coordinates);
        usedCages.add(coordinates);

        MyUser pairUser = allUsers.get(pairUsername);
        if (!information.currentUserInformation.equals(MovingInformation.CURRENT_USER_WIN))
        {
            treatNotWinMovement(currentUser, currentGame, information.currentUserInformation);
            treatNotWinMovement(pairUser, currentGame, information.pairUserInformation);
        }
        else
        {
            treatWinMovement(currentUser, currentGame, information.currentUserInformation);
            treatWinMovement(pairUser, currentGame, information.pairUserInformation);
            dataBaseHandler.executeAddedQueries();
        }
    }
    /**
     * метод для обработки ситуации, когда кто-то победил
     * @param user - текущий пользователь
     * @param game - текущая играя
     * @param information - информация о текущем ходе
     */
    private void treatWinMovement(MyUser user, Game game, String information)
    {
        deleteLastMessage(user, 2);
        Stack<Message> userStack = messageStacks.get(user.getChatId());

        if (user.getState().equals(State.WAITING))
        {
            user.increaseExperience(5);
            user.incrementLoses();
            dataBaseHandler.addBatch(Query.UPDATE_LOSES_SQL, user.getChatId(), user.getLoses());
            Message userMessagePeek = userStack.pop();
            editField(user, userStack.peek().getMessageId(),
                    game.getOwnFields().get(user.getUserName()));
            userStack.add(userMessagePeek);
        }
        else
        {
            user.increaseExperience(10);
            user.incrementWins();
            dataBaseHandler.addBatch(Query.UPDATE_WINS_SQL, user.getChatId(), user.getWins());
            editField(user, userStack.peek().getMessageId(),
                    game.getEnemyFields().get(user.getUserName()));
        }

        user.setState(State.FINISHED_GAME);
        dataBaseHandler.addBatch(Query.UPDATE_EXPERIENCE_SQL, user.getChatId(), user.getExperience());

        if (user.getExperience() >= RankList.ranks.get(user.getCurrentRankIdx()).experience)
        {
            user.incrementCurrentRankIdx();
            dataBaseHandler.addBatch(Query.UPDATE_RANK_INDEX_SQL, user.getChatId(), user.getCurrentRankIdx());
            String[] splittedRank = RankList.ranks.get(user.getCurrentRankIdx()).rank.split(" ");
            information += "\nПоздравляю! Твое звание повышено до " + splittedRank[0] + "a";
            information += (splittedRank.length > 1) ? " " + splittedRank[1] + "!" : "!";
        }

        sendMessage(user, information);
        games.remove(user.getUserName());
        sendRepeatGame(user);
    }
    /**
     * метод для обработки ситуации, когда при очередном шаге победитель не выявился
     * @param user - текущий пользователь
     * @param game - текущая играя
     * @param information - информация о текущем ходе
     */
    private void treatNotWinMovement(MyUser user, Game game, String information)
    {
        Stack<Message> userStack = messageStacks.get(user.getChatId());
        Message lastMessage = userStack.peek();
        String userState = user.getState();

        Boolean flag = game.getFirstMovement().get(user.getUserName());
        if (flag != null)
            treatNotFirstMovement(user, information, lastMessage);
        else
        {
            treatFirstMovement(user, information, lastMessage);
            game.getFirstMovement().put(user.getUserName(), true);
        }

        Message turn = userStack.pop();
        Message event = userStack.pop();

        if (userState.equals(State.WAITING))
        {
            Message userMessagePeek = userStack.pop();
            editField(user, userStack.peek().getMessageId(), game.getOwnFields().get(user.getUserName()));
            userStack.add(userMessagePeek);
        }
        else
            editField(user, userStack.peek().getMessageId(), game.getEnemyFields().get(user.getUserName()));

        userStack.add(event);
        userStack.add(turn);
    }
    /**
     * метод для обработки самого перва шага игры
     * @param user - текущий пользователь
     * @param information - информация о текущем ходе
     * @param lastMessage - последнее сообщение, пришедшее текущему пользователю
     */
    private void treatFirstMovement(MyUser user, String information, Message lastMessage)
    {
        editMessage(user, lastMessage, information);
        lastMessage.setText(information);
        String turn;
        switch (information)
        {
            case MovingInformation.CURRENT_USER_MISS -> {turn = "Сейчас ходит противник"; user.setState(State.WAITING);}
            case MovingInformation.PAIR_USER_MISS -> {turn = "Сейчас ходишь ты"; user.setState(State.MOVING);}
            case MovingInformation.CURRENT_USER_HURT, MovingInformation.CURRENT_USER_KILL -> turn = "Сейчас ходишь ты";
            default -> turn = "Сейчас ходит противник";
        }
        sendMessage(user, turn);
    }
    /**
     * обработка всех остальных непервых ходов в игре
     * @param user - текущий пользователь
     * @param userInformation - информация о текущем ходе
     * @param lastMessage - последнее сообщение, пришедшее текущему пользователю
     */
    private void treatNotFirstMovement(MyUser user, String userInformation, Message lastMessage)
    {
        Stack<Message> userMessageStack = messageStacks.get(user.getChatId());
        userMessageStack.pop();
        Message infoMsg = userMessageStack.peek();

        if (!infoMsg.getText().equals(userInformation))
        {
            editMessage(user, infoMsg, userInformation);
            infoMsg.setText(userInformation);
        }
        if (userInformation.equals(MovingInformation.CURRENT_USER_MISS))
        {
            user.setState(State.WAITING);
            editMessage(user, lastMessage, "Сейчас ходит противник");
            lastMessage.setText("Сейчас ходит противник");

        }
        else if (userInformation.equals(MovingInformation.PAIR_USER_MISS))
        {
            user.setState(State.MOVING);
            editMessage(user, lastMessage, "Сейчас ходишь ты");
            lastMessage.setText("Сейчас ходишь ты");
        }
        userMessageStack.add(lastMessage);
    }
    /**
     * метод для отправки сообщения на повторную игру
     * @param user - кому отправить
     */
    public void sendRepeatGame(MyUser user)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text("Хочешь сыграть с этим игроком еще раз?")
                .replyMarkup(getKeyboardForSendingRepeatGame()).build();
        try
        {
            messageStacks.get(user.getChatId()).add(execute(message));
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                            " произошла ошибка в методе sendRepeatGame(MyUser user).\n" + e.getMessage());
        }
    }
    private InlineKeyboardMarkup getKeyboardForSendingRepeatGame()
    {
        InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                .text("Сыграть еще раз✅")
                .callbackData("want_to_replay").build();
        InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                .text("Выйти в лобби ожидания❌")
                .callbackData("want_to_exit").build();
        List<InlineKeyboardButton> row1 = Collections.singletonList(yesButton);
        List<InlineKeyboardButton> row2 = Collections.singletonList(noButton);
        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2)).build();
    }
    /**
     * метод для редактирования сообщений
     * @param user - в диалоге с кем отредактировать
     * @param message - какое сообщение отредактировать
     * @param editedText - на что отредактировать
     */
    public void editMessage(MyUser user, Message message, String editedText)
    {
        EditMessageText editedMessage = EditMessageText.builder()
                .chatId(user.getChatId())
                .messageId(message.getMessageId())
                .text(editedText).build();
        try
        {
            execute(editedMessage);
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе editMessage(MyUser user, Message message, String editedText).\n" +
                    e.getMessage());
        }
    }
    /**
     * метод для установки третьего одножизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - имзенения
     */
    private void boat3SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUserName());
        final int LAST_SHIP = 6;
        Ship boat3 = currentGame.getShips().get(currentUser.getUserName()).get(LAST_SHIP);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, boat3))
        {
            if (boat3.getCoordinatesSet().size() == boat3.getLives())
            {
                currentUser.setState(State.READY_TO_PLAY);
                MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUserName()));
                if (!pairUser.getState().equals(State.READY_TO_PLAY))
                {
                    editMessage(currentUser, messageStacks.get(currentUser.getChatId()).peek(),
                            "Подожди, твой противник еще расставляет корабли");
                    editField(currentUser, messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                            currentGame.getOwnFields().get(currentUser.getUserName()));
                }
                else
                {
                    sendFieldsAndDefineTurn(currentUser, currentGame);
                    sendFieldsAndDefineTurn(pairUser, currentGame);
                }
            }
        }
    }
    private void sendFieldsAndDefineTurn(MyUser user, Game game)
    {
        deleteLastMessage(user);
        sendField(user, game.getOwnFields().get(user.getUserName()), "Твое поле:");
        sendField(user, game.getEnemyFields().get(user.getUserName()), "Поле твоего противника:");
        if (user.getUserName().equals((game.getCreator().getUserName())))
        {
            user.setState(State.MOVING);
            sendMessage(user, "Cейчас ходишь ты");
        }
        else
        {
            user.setState(State.WAITING);
            sendMessage(user, "Cейчас ходит противник");
        }
    }
    /**
     * метод для установки второго одножизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void boat2SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUserName());
        Ship boat2 = currentGame.getShips().get(currentUser.getUserName()).get(5);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, boat2))
        {
            if (boat2.getCoordinatesSet().size() == boat2.getLives())
                currentUser.setState(State.BOAT_3_SETTING);
            editField(currentUser, messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUserName()));
        }
    }
    /**
     * метод для установки первого одножизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void boat1SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUserName());
        Ship boat1 = currentGame.getShips().get(currentUser.getUserName()).get(4);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, boat1))
        {
            if (boat1.getCoordinatesSet().size() == boat1.getLives())
                currentUser.setState(State.BOAT_2_SETTING);
            editField(currentUser, messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUserName()));
        }
    }
    /**
     * метод для установки второго двухжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void esminez2SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUserName());
        Ship esminez2 = currentGame.getShips().get(currentUser.getUserName()).get(3);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, esminez2))
        {
            if (esminez2.getCoordinatesSet().size() == esminez2.getLives())
            {
                currentUser.setState(State.BOAT_1_SETTING);
                editMessage(currentUser, messageStacks.get(currentUser.getChatId()).peek(), TIP.BOATS);
            }
            editField(currentUser, messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUserName()));
        }
    }
    /**
     * метод для установки первого двухжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void esminez1SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUserName());
        Ship esminez1 = currentGame.getShips().get(currentUser.getUserName()).get(2);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, esminez1))
        {
            if (esminez1.getCoordinatesSet().size() == esminez1.getLives())
                currentUser.setState(State.ESMINEZ_2_SETTTING);
            editField(currentUser, messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUserName()));
        }
    }
    /**
     * метод для установки единственного трехжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void cruiserSettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUserName());
        Ship cruiser = currentGame.getShips().get(currentUser.getUserName()).get(1);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, cruiser))
        {
            if (cruiser.getCoordinatesSet().size() == cruiser.getLives())
            {
                currentUser.setState(State.ESMINEZ_1_SETTTING);
                editMessage(currentUser, messageStacks.get(currentUser.getChatId()).peek(), TIP.ESMINEZS);
            }
            editField(currentUser, messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUserName()));
        }
    }
    /**
     * метод для установки единственного четырезжизненного корабля
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void linCoreSettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUserName());
        Ship linCore = currentGame.getShips().get(currentUser.getUserName()).get(0);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, linCore))
        {
            if (linCore.getCoordinatesSet().size() == linCore.getLives())
            {
                currentUser.setState(State.CRUISER_SETTING);
                editMessage(currentUser, messageStacks.get(currentUser.getChatId()).peek(), TIP.CRUISER);
            }
            editField(currentUser, messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getUserName()));
        }
    }
    /**
     * метод для редактирования поля
     * @param user - кому отправить
     * @param messageId - в каком сообщении
     * @param field - на какое поле заменить исходное
     */
    public void editField(MyUser user, Integer messageId, TelegramField field)
    {
        EditMessageReplyMarkup editedField = EditMessageReplyMarkup.builder()
                        .chatId(user.getChatId())
                        .messageId(messageId)
                        .replyMarkup(field.getKeyboardMarkup()).build();
        try
        {
            execute(editedField);
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе editField(MyUser user, Integer messageId, TelegramField field).\n" +
                    e.getMessage());
        }
    }
    /**
     * метод для обработки callback-действий, когда currentUser находится в лобби
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void lobbyCallbackQueryHandler(MyUser currentUser, Update update) throws ClassNotFoundException {
        String rawInformation = update.getCallbackQuery().getData();
        if (rawInformation.length() > 13)
        {
            String information = rawInformation.substring(0, 13);
            String pairUserName = rawInformation.substring(13);
            MyUser pairUser = allUsers.get(pairUserName);
            switch (information)
            {
                case "accept_Invite" -> treatAcceptInvite(currentUser, pairUser);
                case "refuse_Invite" -> treatRefuseInvite(currentUser, pairUser);
            }
        }
        else
        {
            deleteLastMessage(currentUser);
            switch (rawInformation)
            {
                case "my_stats" -> sendUserStatistics(currentUser);
                case "top_10" -> sendTop10Users(currentUser);
                case "rules" -> sendRules(currentUser);
                case "prject_info" -> sendProjectInfo(currentUser);
                case "back_to_main" -> sendMainLobbyMenu(currentUser);
            }
        }
    }

    private void sendProjectInfo(MyUser user)
    {
        sendWindow(user, "Находится в стадии разработки...");
    }

    private void sendRules(MyUser user)
    {
        sendWindow(user, "Находится в стадии разработки...");
    }

    private void sendTop10Users(MyUser user) throws ClassNotFoundException {
        String queryResult;
        try
        {
            queryResult = dataBaseHandler.getTop10Users();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        sendWindow(user, queryResult);
    }

    private void sendUserStatistics(MyUser user) throws ClassNotFoundException {
        int position = dataBaseHandler.getSingleUserPosition(user);
        Rank currentRank = RankList.ranks.get(user.getCurrentRankIdx());
        String content =  "Твоя статистика:\n" +
                "Общее количество игр: " + (user.getWins() + user.getLoses()) + "\n" +
                "Из них: " + user.getWins() + " побед, " + user.getLoses() + " поражений\n" +
                "Доля побед: " + dataBaseHandler.getUserWinRate(user.getWins(), user.getLoses()) + "%\n" +
                "Твое звание: " + currentRank.rank + "\n" +
                "До следующего звания осталось: " +
                (currentRank.experience - user.getExperience()) + " опыта\n" +
                "Всего опыта: " + user.getExperience() + "\n" +
                "Твое место среди всех пользователей: " + position;
        sendWindow(user, content);
    }

    private InlineKeyboardMarkup getBackToMainMenuButton()
    {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("⬅️Вернуться в главное меню")
                        .callbackData("back_to_main").build()))).build();
    }

    private void sendWindow(MyUser user, String text)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text(text)
                .replyMarkup(getBackToMainMenuButton()).build();
        try
        {
            Message sendedMessage = execute(message);
            Stack<Message> currentMessageStack = messageStacks.get(user.getChatId());
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(user.getChatId(), newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе sendWindow(MyUser user, String text).\n" +
                    e.getMessage());
        }
    }

    /**
     * метод для обработки принятия приглашения
     * @param whoAccepts - кто принимает
     * @param whoInvites - кто приглашает
     */
    private void treatAcceptInvite(MyUser whoAccepts, MyUser whoInvites)
    {
        Game newGame = new Game(whoInvites, whoAccepts);
        deleteLastMessage(whoInvites, 2);
        deleteInvitationMessage(whoAccepts, whoInvites.getUserName());
        deleteLastMessage(whoAccepts);
        prepareForShipSetting(whoInvites, whoAccepts, newGame);
        prepareForShipSetting(whoAccepts, whoInvites, newGame);
    }
    private void prepareForShipSetting(MyUser user1, MyUser user2, Game game)
    {
        user1.setState(State.LINCORE_SETTING);
        games.put(user1.getUserName(), game);
        userPairs.put(user1.getUserName(), user2.getUserName());
        sendField(user1, game.getOwnFields().get(user1.getUserName()), TIP.LINCORE);
    }
    /**
     * метод для отправки игрового поля
     * @param user - кому отправить
     * @param field - какое поле отправить
     * @param caption - надпись над полем
     */
    public void sendField(MyUser user, TelegramField field, String caption)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text(caption)
                .replyMarkup(field.getKeyboardMarkup()).build();
        try
        {
            Message sendedMessage = execute(message);
            Stack<Message> currentMessageStack = messageStacks.get(user.getChatId());
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(user.getChatId(), newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе sendField(MyUser user, TelegramField field, String caption).\n" +
                    e.getMessage());
        }
    }
    /**
     * метод для обработки отклонения приглашения
     * @param whoRefuses - кто отклоняет
     * @param whoInvites - кто приглашает
     */
    private void treatRefuseInvite(MyUser whoRefuses, MyUser whoInvites)
    {
        deleteLastMessage(whoInvites);
        sendMessageWithNoSave(whoInvites.getChatId(),
                whoRefuses.getFirstName() + " отклонил твое приглашение.");
        deleteInvitationMessage(whoRefuses, whoInvites.getUserName());
    }
    /**
     * метод для обработки всех тектовых сообщений, вводимых пользователем
     * @param update - входящие изменения
     */
    private void handleMessage(Update update) throws SQLException, ClassNotFoundException
    {
        User currentUser =  update.getMessage().getFrom();
        MyUser currentMyUser = allUsers.get(currentUser.getUserName());
        Long chatId = update.getMessage().getChatId();
        if (currentMyUser != null)
        {
            switch (currentMyUser.getState())
            {
                case State.IN_LOBBY -> lobbyMessageHandler(currentMyUser, update);
                case State.WANT_TO_REPLAY, State.FINISHED_GAME -> endGameMessageHandler(currentMyUser, update);
                default -> gameMessageHandler(currentMyUser, update);
            }
        }
        else if (update.getMessage().getText().equals(MessageCommand.START))
            registerUserAndGreet(chatId, currentUser);
        else
            sendMessageWithNoSave(chatId, "Для авторизации напиши команду /start");
    }
    private void gameMessageHandler(MyUser currentUser, Update update)
    {
        String text = update.getMessage().getText();
        switch (text)
        {
            case MessageCommand.PERMUTE -> { if (currentUser.getState().contains("setting")) permuteField(currentUser);}
            case MessageCommand.EXIT -> exitFromGameHandler(currentUser);
        }
    }
    private void exitFromGameHandler(MyUser currentUser)
    {
        int deleteMessageCounter;
        if (currentUser.getState().contains("setting") || currentUser.getState().equals(State.READY_TO_PLAY))
            deleteMessageCounter = 1;
        else if (games.get(currentUser.getUserName()).getFirstMovement().get(currentUser.getUserName()) == null)
            deleteMessageCounter = 3;
        else
            deleteMessageCounter = 4;
        leaveFromCurrentGame(currentUser, deleteMessageCounter);
    }
    private void permuteField(MyUser currentUser)
    {
        deleteLastMessage(currentUser);
        Game currentGame = games.get(currentUser.getUserName());
        currentGame.resetOwnField(currentUser);
        currentUser.setState(State.LINCORE_SETTING);
        sendField(currentUser, currentGame.getOwnFields().get(currentUser.getUserName()), TIP.LINCORE);
    }
    private void endGameMessageHandler(MyUser currentUser, Update update)
    {
        if (update.getMessage().getText().equals(MessageCommand.EXIT))
            leaveAfterGame(currentUser);
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

        if (text.equals("Отменить приглашение❌"))
            cancelInvitation(currentUser);
        else
            treatPairUserPresence(text, currentUser);
    }
    /**
     * метод для проверки наличия в базе введенного текущим пользователем тега другого пользователя и
     * в случае успеха создание из них игровой пары
     * @param text - текстовое сообщение
     * @param currentUser - текущий пользователь
     */
    private void treatPairUserPresence(String text, MyUser currentUser)
    {
        if (text.charAt(0) == '@')
        {
            MyUser invitedUser = allUsers.get(text.substring(1));
            if (invitedUser == null)
            {
                sendMessageWithNoSave(currentUser.getChatId(),
                        "Извини, данного пользователя нет в системе.");
            }
            else if (invitedUser.getState().equals(State.IN_LOBBY) &&
                    !invitedUser.getUserName().equals(currentUser.getUserName()))
            {
                sendInvite(invitedUser, currentUser);
                sendWaitingMessage(currentUser);
            }
            else if (!currentUser.getState().equals(State.IN_LOBBY))
            {
                sendMessageWithNoSave(currentUser.getChatId(),
                        "Извини, данный пользователь уже с кем-то играет");
            }
            else if (invitedUser.getUserName().equals(currentUser.getUserName()))
                sendMessageWithNoSave(currentUser.getChatId(), "Опции игры с самим собой пока-что нет :(");
        }
    }
    /**
     * метод для отмены приглашения у приглашающего пользователя
     * @param invitingUser - приглашающий пользователь
     */
    private void cancelInvitation(MyUser invitingUser)
    {
        deleteLastMessage(invitingUser);
        String invitingUserName = invitingUser.getUserName();
        deleteInvitationMessage(invitedUsers.get(invitingUserName), invitingUserName);
    }

    private void deleteInvitationMessage(MyUser user, String invitingUserName)
    {
        Integer messageId = invitationMessages.get(user.getChatId()).get(invitingUserName);
        invitedUsers.remove(invitingUserName);

        DeleteMessage message = DeleteMessage.builder()
                .messageId(messageId)
                .chatId(user.getChatId()).build();
        try
        {
            execute(message);
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе deleteInvitationMessage(MyUser user, String invitingUserName).\n" +
                    e.getMessage());
        }
    }

    private void registerUserAndGreet(Long chatId, User user) throws ClassNotFoundException
    {
        MyUser newUser = new MyUser(chatId, user.getUserName(), user.getFirstName(), State.IN_LOBBY);
        allUsers.put(user.getUserName(), newUser);
        dataBaseHandler.insertUserIntoDB(newUser);
        sendGreetings(newUser);
    }
    /**
     * Метод, высылающий пользователю сообщение-приветствие
     * @param user - кому отправить
     */
    public void sendGreetings(MyUser user)
    {
        sendMessageWithNoSave(user.getChatId(), user.getFirstName() + ", добро пожаловать в морской бой!");
        sendMainLobbyMenu(user);
    }

    private void sendMainLobbyMenu(MyUser user)
    {
        InputFile menuPicture = new InputFile(getClass()
                .getClassLoader()
                .getResourceAsStream("images/mainMenuPicture.png"), "mainMenuPicture.png");

        SendPhoto message = SendPhoto.builder()
                .chatId(user.getChatId())
                .photo(menuPicture)
                .caption("Ты находишься в лобби. Чтобы начать играть, пригласи пользователя, написав мне его @username "
                        + "(обязательно с символом «@»!)")
                .replyMarkup(getMainLobbyMenuKeyboard()).build();
        try
        {
            Message sendedMessage = execute(message);
            Stack<Message> currentMessageStack = messageStacks.get(user.getChatId());
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(user.getChatId(), newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе sendMainLobbyMenu(MyUser user).\n" +
                    e.getMessage());
        }

    }
    private InlineKeyboardMarkup getMainLobbyMenuKeyboard()
    {
        List<InlineKeyboardButton> row1 = Collections.singletonList(InlineKeyboardButton.builder()
                .text("Моя статистика\uD83D\uDCC8")
                .callbackData("my_stats").build());

        List<InlineKeyboardButton> row2 = Collections.singletonList(InlineKeyboardButton.builder()
                .text("Топ-10 пользователей\uD83C\uDFC6")
                .callbackData("top_10").build());

        List<InlineKeyboardButton> row3 = Collections.singletonList(InlineKeyboardButton.builder()
                .text("Правила❓")
                .callbackData("rules").build());

        List<InlineKeyboardButton> row4 = Collections.singletonList(InlineKeyboardButton.builder()
                .text("О проекте⚙️")
                .callbackData("prject_info").build());

        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2, row3, row4)).build();
    }
    /**
     * метод отправки сообщений
     * @param user - кому отправить
     * @param whatToSend - что отправить
     */
    private void sendMessage(MyUser user, String whatToSend)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text(whatToSend).build();
        try
        {
            Message sendedMessage = execute(message);
            Stack<Message> currentMessageStack = messageStacks.get(user.getChatId());
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(user.getChatId(), newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе sendMessage(MyUser user, String whatToSend).\n" +
                    e.getMessage());
        }
    }
    private void sendMessageWithNoSave(Long chatId, String whatToSend)
    {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(whatToSend).build();
        try
        {
            execute(message);
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "Внимание! произошла ошибка в методе " +
                    "sendMessageWithNoSave(Long chatId, String whatToSend).\n" +
                    e.getMessage());
        }
    }
    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param user - в диалоге с кем удалить
     */
    private void deleteLastMessage(MyUser user)
    {
        Stack<Message> currentMessageStack = messageStacks.get(user.getChatId());
        DeleteMessage deleteMessage = DeleteMessage.builder()
                        .chatId(user.getChatId())
                        .messageId(currentMessageStack.pop().getMessageId()).build();
        try
        {
            execute(deleteMessage);
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе deleteLastMessage(MyUser user).\n" +
                    e.getMessage());
        }
    }
    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param user - в диалоге с кем удалить
     */
    private void deleteLastMessage(MyUser user, int times)
    {
        Stack<Message> currentMessageStack = messageStacks.get(user.getChatId());
        for (int i = 0; i < times; i++)
        {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(user.getChatId())
                    .messageId(currentMessageStack.pop().getMessageId()).build();
            try
            {
                execute(deleteMessage);
            } catch (TelegramApiException e)
            {
                sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                        " произошла ошибка в методе deleteLastMessage(MyUser user, int times).\n" +
                        e.getMessage());
            }
        }
    }
    /**
     * Присылает сообщение для приглашающего и ожидающего пользователя,
     * предоставляющее ему возможность отменить приглашение
     *
     * @param user кому отправить
     */
    private void sendWaitingMessage(MyUser user)
    {
        KeyboardRow row1 = new KeyboardRow(List.of(new KeyboardButton("Отменить приглашение❌")));
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .keyboard(Collections.singletonList(row1)).build();

        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text("Приглашение отправлено. Ожидай ответа!")
                .replyMarkup(keyboardMarkup).build();
        try
        {
            Message sendedMessage = execute(message);
            Stack<Message> currentMessageStack = messageStacks.get(user.getChatId());
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(user.getChatId(), newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователя @" + user.getUserName() +
                    " произошла ошибка в методе sendWaitingMessage(MyUser user).\n" +
                    e.getMessage());
        }
    }
    /**
     * метод для отправки приглашения на поединок
     * @param whoIsInvited - приглашаемый пользователь
     * @param whoInvites - приглашающий пользователь
     */
    private void sendInvite(MyUser whoIsInvited, MyUser whoInvites)
    {
        SendMessage message = SendMessage.builder()
                        .chatId(whoIsInvited.getChatId())
                        .text(whoInvites.getFirstName() + " приглашает тебя поиграть в морской бой!")
                        .replyMarkup(getInviteKeyboard(whoInvites)).build();
        try
        {
            invitedUsers.put(whoInvites.getUserName(), whoIsInvited);
            Message sendedMessage = execute(message);
            Map<String, Integer> invitationTable = invitationMessages.get(whoIsInvited.getChatId());
            if (invitationTable == null)
            {
                invitationMessages.put(whoIsInvited.getChatId(), new HashMap<>());
                invitationTable = invitationMessages.get(whoIsInvited.getChatId());
            }
            invitationTable.put(whoInvites.getUserName(), sendedMessage.getMessageId());
        }
        catch (TelegramApiException e)
        {
            sendMessageWithNoSave(creatorChatId, "У пользователей @" + whoIsInvited.getUserName() + "и @" +
                      whoInvites.getUserName() + " произошла ошибка в методе sendInvite(MyUser whoIsInvited," +
                    " MyUser whoInvites).\n" + e.getMessage());
        }
    }
    /**
     * метод для формирования поля
     * @return поле для приглашаемого игрока
     */
    private InlineKeyboardMarkup getInviteKeyboard(MyUser user) {
        InlineKeyboardButton acceptButton = InlineKeyboardButton.builder()
                .text("Принять приглашение✅")
                .callbackData("accept_Invite" + user.getUserName()).build();
        InlineKeyboardButton refuseButton = InlineKeyboardButton.builder()
                .text("Отклонить❌")
                .callbackData("refuse_Invite" + user.getUserName()).build();
        List<InlineKeyboardButton> row1 = Collections.singletonList(acceptButton);
        List<InlineKeyboardButton> row2 = Collections.singletonList(refuseButton);
        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2)).build();
    }
    public static TelegramBotBuilder builder() { return new TelegramBotBuilder(); }
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
