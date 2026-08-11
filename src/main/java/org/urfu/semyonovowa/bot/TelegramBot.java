package org.urfu.semyonovowa.bot;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Класс, в котором изложена логика обработки взаимодействия с пользователями телеграмма
 */
public class TelegramBot extends TelegramLongPollingBot
{
    private final SessionRegistry sessions;
    private final Cache<Long, MyUser> userCache;
    private final DataBaseHandler dataBaseHandler;
    private final String botUserName;
    private final Long creatorChatId;
    private final String botToken;
    private final TelegramGateway gateway;
    public TelegramBot(String botUserName, String token, Long creatorChatId, DataBaseHandler dataBaseHandler)
    {
        super(token);
        this.gateway = new TelegramGateway(this, creatorChatId);
        this.botUserName = botUserName;
        this.botToken = token;
        this.creatorChatId = creatorChatId;
        this.sessions = new SessionRegistry();
        this.dataBaseHandler = dataBaseHandler;
        this.userCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .removalListener(this::notificationHandler).build();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(userCache::cleanUp, 1, 1, TimeUnit.MINUTES);
    }

    private void notificationHandler(RemovalNotification<Long, MyUser> notification)
    {
        MyUser user = notification.getValue();
        if (user == null)
            return;
        if (!user.getState().equals(State.IN_LOBBY))
            cleanTrailsBeforeFreeze(user);
        else
        {
            Stack<Message> currentStack = sessions.messageStacks().get(user.getChatId());
            if (currentStack != null)
                deleteLastMessage(user, currentStack.size() - 1);
        }

        Long pairUserChatId = sessions.userPairs().get(user.getChatId());
        if (pairUserChatId != null)
        {
            MyUser pairUser = userCache.getIfPresent(pairUserChatId);
            if (pairUser != null)
                cleanTrailsBeforeFreeze(pairUser);
        }
        try
        {
            Integer messageId;
            Stack<Message> currentStack = sessions.messageStacks().get(user.getChatId());
            messageId = (currentStack == null || currentStack.isEmpty())
                    ? user.getLastMessageId()
                    : currentStack.pop().getMessageId();
            dataBaseHandler.freezeUser(user, messageId);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        freeMemoryFrom(user);
    }

    private void cleanTrailsBeforeFreeze(MyUser user)
    {
        user.setState(State.IN_LOBBY);
        while (!sessions.messageStacks().get(user.getChatId()).isEmpty())
            deleteLastMessage(user);
        sendMessageWithNoSave(user.getChatId(), TIP.TIMEOUT);
        sendMainLobbyMenu(user);
    }

    private void freeMemoryFrom(MyUser user)
    {
        if (sessions.invitationMessages().get(user.getChatId()) != null)
        {
            if (!sessions.invitationMessages().get(user.getChatId()).isEmpty())
            {
                for (Map.Entry<Long, Integer> entry : sessions.invitationMessages().get(user.getChatId()).entrySet())
                {
                    deleteMessage(user, entry.getValue());
                }
            }
            sessions.invitationMessages().remove(user.getChatId());
        }

        Stack<Message> currentStack = sessions.messageStacks().get(user.getChatId());
        if (currentStack != null)
            while (!currentStack.isEmpty())
                deleteMessage(user, currentStack.pop().getMessageId());

        Long pairUserChatId = sessions.userPairs().get(user.getChatId());
        if (pairUserChatId != null)
        {
            sessions.userPairs().remove(user.getChatId());
            sessions.userPairs().remove(pairUserChatId);
        }
        Game game = sessions.games().get(user.getChatId());
        if (game != null)
        {
            sessions.games().remove(user.getChatId());
            if (pairUserChatId != null)
                sessions.games().remove(pairUserChatId);
        }
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
     * @param update входящие изменения
     */
    private void handleCallbackQuery(Update update) throws SQLException, ClassNotFoundException {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        MyUser currentUser = userCache.getIfPresent(chatId);
        if (currentUser != null)
        {
            switch (currentUser.getState())
            {
                case IN_LOBBY -> lobbyCallbackQueryHandler(currentUser, update);
                case LINCORE_SETTING -> handleShipSetting(currentUser, update, 0, State.CRUISER_SETTING, TIP.CRUISER);
                case CRUISER_SETTING -> handleShipSetting(currentUser, update, 1, State.ESMINEZ_1_SETTING, TIP.ESMINEZS);
                case ESMINEZ_1_SETTING -> handleShipSetting(currentUser, update, 2, State.ESMINEZ_2_SETTING, null);
                case ESMINEZ_2_SETTING -> handleShipSetting(currentUser, update, 3, State.BOAT_1_SETTING, TIP.BOATS);
                case BOAT_1_SETTING -> handleShipSetting(currentUser, update, 4, State.BOAT_2_SETTING, null);
                case BOAT_2_SETTING -> handleShipSetting(currentUser, update, 5, State.BOAT_3_SETTING, null);
                case BOAT_3_SETTING -> boat3SettingCallbackQueryHandler(currentUser, update);
                case MOVING -> movingHandler(currentUser, update);
                case FINISHED_GAME, WANT_TO_REPLAY -> revengeHandler(currentUser, update);
                default -> {}
            }
        }
        else
        {
            currentUser = dataBaseHandler.pullUserFromDB(chatId);
            if (currentUser != null)
            {
                User user = update.getCallbackQuery().getFrom();
                checkUserNameUpdate(currentUser, user);
                if (!userCache.asMap().containsKey(currentUser.getChatId()))
                    userCache.put(currentUser.getChatId(), currentUser);
                handleCallbackQuery(update);
            }
        }
    }
    private void checkUserNameUpdate(MyUser myUser, User user) throws ClassNotFoundException
    {
        if ((myUser.getUserName() != null && user.getUserName() == null)
        || (myUser.getUserName() == null && user.getUserName() != null)
        || (myUser.getUserName() != null && user.getUserName() != null
                && !myUser.getUserName().equals(user.getUserName())))
        {
            myUser.setUserName(user.getUserName());
            dataBaseHandler.updateUserName(myUser, user.getUserName());
        }
    }
    /**
     * метод для обработки реванша
     * @param currentUser текущий пользователь
     * @param update изменения
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
        commonLeaveGame(currentUser, times, " сбежал(-а) с поля битвы!");
    }

    private void commonLeaveGame(MyUser currentUser, int times, String textAfter)
    {
        Long pairUserChatId = sessions.userPairs().get(currentUser.getChatId());
        MyUser pairUser = userCache.getIfPresent(pairUserChatId);
        sessions.userPairs().remove(pairUserChatId);
        sessions.userPairs().remove(currentUser.getChatId());
        commonCleanTrailsEndGame(currentUser, times, "");
        if (pairUser != null)
            commonCleanTrailsEndGame(pairUser, times, currentUser.getFirstName() + textAfter);
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
     * @param currentUser текущий пользователь
     */
    private void leaveAfterGame(MyUser currentUser)
    {
        commonLeaveGame(currentUser, 4, " наигрался(-ась) с тобой.");
    }

    /**
     * метод для обработки начала новой игры с сохранием исходной пары игроков
     * @param currentUser текущий пользователь
     */
    private void replayHandler(MyUser currentUser)
    {
        if (currentUser.getState().equals(State.WANT_TO_REPLAY))
            return;

        currentUser.setState(State.WANT_TO_REPLAY);

        MyUser pairUser = userCache.getIfPresent(sessions.userPairs().get(currentUser.getChatId()));
        if (pairUser != null && pairUser.getState().equals(State.WANT_TO_REPLAY))
        {
            Game newGame = new Game(pairUser, currentUser);
            prepareForReplay(currentUser, newGame);
            prepareForReplay(pairUser, newGame);
        }
    }
    private void prepareForReplay(MyUser user, Game game)
    {
        user.setState(State.LINCORE_SETTING);
        sessions.games().put(user.getChatId(), game);
        deleteLastMessage(user, 4);
        sendField(user, game.getOwnFields().get(user.getChatId()), TIP.LINCORE);
    }
    /**
     * метод для обработки любых ходов во время игры
     * @param currentUser текущий пользователь
     * @param update изменения
     */
    private void movingHandler(MyUser currentUser, Update update) throws ClassNotFoundException
    {
        Game currentGame = sessions.games().get(currentUser.getChatId());
        String coordinates = update.getCallbackQuery().getData().substring(1);
        Long pairUserChatId = sessions.userPairs().get(currentUser.getChatId());
        Set<String> usedCages = currentGame.getOwnFields().get(pairUserChatId).getUsedCages();

        if (usedCages.contains(coordinates))
            return;
        MovingInformationForBothPlayers information = currentGame.attack(currentUser, coordinates);
        usedCages.add(coordinates);

        MyUser pairUser = userCache.getIfPresent(pairUserChatId);
        if (!information.currentUserInformation.equals(MovingInformation.CURRENT_USER_WIN))
        {
            treatNotWinMovement(currentUser, currentGame, information.currentUserInformation);
            if (pairUser != null)
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
     * @param user текущий пользователь
     * @param game текущая играя
     * @param information информация о текущем ходе
     */
    private void treatWinMovement(MyUser user, Game game, String information)
    {
        deleteLastMessage(user, 2);
        Stack<Message> userStack = sessions.messageStacks().get(user.getChatId());

        if (user.getState().equals(State.WAITING))
        {
            MyUser winner = userCache.getIfPresent(sessions.userPairs().get(user.getChatId()));
            if (winner != null)
            {
                TelegramField winnerOwnField = game.getOwnFields().get(winner.getChatId());
                TelegramField loserEnemyField = game.getEnemyFields().get(user.getChatId());
                loserEnemyField.showAllSurvivedEnemyShips(winnerOwnField);
                editField(user, userStack.peek().getMessageId(), loserEnemyField);
            }
            user.increaseExperience(5);
            user.incrementLoses();
            dataBaseHandler.addBatch(Query.UPDATE_LOSES_SQL, user.getChatId(), user.getLoses());
            Message userMessagePeek = userStack.pop();
            editField(user, userStack.peek().getMessageId(),
                    game.getOwnFields().get(user.getChatId()));
            userStack.add(userMessagePeek);
        }
        else
        {
            user.increaseExperience(10);
            user.incrementWins();
            dataBaseHandler.addBatch(Query.UPDATE_WINS_SQL, user.getChatId(), user.getWins());
            editField(user, userStack.peek().getMessageId(),
                    game.getEnemyFields().get(user.getChatId()));
        }

        user.setState(State.FINISHED_GAME);
        dataBaseHandler.addBatch(Query.UPDATE_EXPERIENCE_SQL, user.getChatId(), user.getExperience());

        if (user.getExperience() >= RankList.ranks.get(user.getCurrentRankIdx()).experience)
        {
            user.incrementCurrentRankIdx();
            dataBaseHandler.addBatch(Query.UPDATE_RANK_INDEX_SQL, user.getChatId(), user.getCurrentRankIdx());
            String[] splittedRank = RankList.ranks.get(user.getCurrentRankIdx()).rank.split(" ");
            information += "\nТвое звание повышено до " + splittedRank[0] + "a";
            information += (splittedRank.length > 1) ? " " + splittedRank[1] + "!" : "!";
        }

        sendMessage(user, information);
        sessions.games().remove(user.getChatId());
        sendRepeatGame(user);
    }
    /**
     * метод для обработки ситуации, когда при очередном шаге победитель не выявился
     * @param user текущий пользователь
     * @param game текущая играя
     * @param information информация о текущем ходе
     */
    private void treatNotWinMovement(MyUser user, Game game, String information)
    {
        Stack<Message> userStack = sessions.messageStacks().get(user.getChatId());
        Message lastMessage = userStack.peek();
        State userState = user.getState();

        Boolean flag = game.getFirstMovement().get(user.getChatId());
        if (flag != null)
            treatNotFirstMovement(user, information, lastMessage);
        else
        {
            treatFirstMovement(user, information, lastMessage);
            game.getFirstMovement().put(user.getChatId(), true);
        }

        Message turn = userStack.pop();
        Message event = userStack.pop();

        if (userState.equals(State.WAITING))
        {
            Message userMessagePeek = userStack.pop();
            editField(user, userStack.peek().getMessageId(), game.getOwnFields().get(user.getChatId()));
            userStack.add(userMessagePeek);
        }
        else
            editField(user, userStack.peek().getMessageId(), game.getEnemyFields().get(user.getChatId()));

        userStack.add(event);
        userStack.add(turn);
    }
    /**
     * метод для обработки самого перва шага игры
     * @param user текущий пользователь
     * @param information информация о текущем ходе
     * @param lastMessage последнее сообщение, пришедшее текущему пользователю
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
     * @param user текущий пользователь
     * @param userInformation информация о текущем ходе
     * @param lastMessage последнее сообщение, пришедшее текущему пользователю
     */
    private void treatNotFirstMovement(MyUser user, String userInformation, Message lastMessage)
    {
        Stack<Message> userMessageStack = sessions.messageStacks().get(user.getChatId());
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
     * @param user кому отправить
     */
    public void sendRepeatGame(MyUser user)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text("Хочешь сыграть с этим игроком еще раз?")
                .replyMarkup(LobbyMenu.keyboardForSendingRepeatGame).build();
        saveToStack(user, gateway.send(message));
    }
    /**
     * метод для редактирования сообщений
     * @param user в диалоге с кем отредактировать
     * @param message какое сообщение отредактировать
     * @param editedText на что отредактировать
     */
    public void editMessage(MyUser user, Message message, String editedText)
    {
        EditMessageText editedMessage = EditMessageText.builder()
                .chatId(user.getChatId())
                .messageId(message.getMessageId())
                .text(editedText).build();
        gateway.edit(editedMessage);
    }
    /**
     * метод для установки третьего одножизненного корабля
     * @param currentUser текущий пользователь
     * @param update имзенения
     */
    private void boat3SettingCallbackQueryHandler(MyUser currentUser, Update update)
    {
        Game currentGame = sessions.games().get(currentUser.getChatId());
        final int LAST_SHIP = 6;
        Ship boat3 = currentGame.getShips().get(currentUser.getChatId()).get(LAST_SHIP);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, boat3))
        {
            if (boat3.getCoordinatesSet().size() == boat3.getLives())
            {
                currentUser.setState(State.READY_TO_PLAY);
                MyUser pairUser = userCache.getIfPresent(sessions.userPairs().get(currentUser.getChatId()));
                if (pairUser != null && !pairUser.getState().equals(State.READY_TO_PLAY))
                {
                    editMessage(currentUser, sessions.messageStacks().get(currentUser.getChatId()).peek(),
                            "Подожди, твой противник еще расставляет корабли");
                    editField(currentUser, sessions.messageStacks().get(currentUser.getChatId()).peek().getMessageId(),
                            currentGame.getOwnFields().get(currentUser.getChatId()));
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
        sendField(user, game.getOwnFields().get(user.getChatId()), "Твое поле:");
        sendField(user, game.getEnemyFields().get(user.getChatId()), "Поле твоего противника:");
        if (user.getChatId() == game.getCreator().getChatId())
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
     * Общий обработчик расстановки корабля: ставит клетку по клику; при завершении
     * корабля переводит пользователя в следующее состояние и, если задано, показывает
     * подсказку. Заменил семь почти одинаковых обработчиков (последний корабль —
     * особый, см. boat3SettingCallbackQueryHandler).
     *
     * @param shipIndex индекс корабля в списке кораблей игрока (0..5)
     * @param nextState состояние после полной расстановки корабля
     * @param tip       подсказка для следующего корабля, либо null
     */
    private void handleShipSetting(MyUser currentUser, Update update, int shipIndex, State nextState, String tip)
    {
        Game currentGame = sessions.games().get(currentUser.getChatId());
        Ship ship = currentGame.getShips().get(currentUser.getChatId()).get(shipIndex);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, ship))
        {
            if (ship.getCoordinatesSet().size() == ship.getLives())
            {
                currentUser.setState(nextState);
                if (tip != null)
                    editMessage(currentUser, sessions.messageStacks().get(currentUser.getChatId()).peek(), tip);
            }
            editField(currentUser, sessions.messageStacks().get(currentUser.getChatId()).peek().getMessageId(),
                    currentGame.getOwnFields().get(currentUser.getChatId()));
        }
    }
    /**
     * метод для редактирования поля
     * @param user кому отправить
     * @param messageId в каком сообщении
     * @param field на какое поле заменить исходное
     */
    public void editField(MyUser user, Integer messageId, TelegramField field)
    {
        EditMessageReplyMarkup editedField = EditMessageReplyMarkup.builder()
                        .chatId(user.getChatId())
                        .messageId(messageId)
                        .replyMarkup(field.getKeyboardMarkup()).build();
        gateway.edit(editedField);
    }
    /**
     * метод для обработки callback-действий, когда currentUser находится в лобби
     * @param currentUser текущий пользователь
     * @param update изменения
     */
    private void lobbyCallbackQueryHandler(MyUser currentUser, Update update) throws ClassNotFoundException {
        String rawInformation = update.getCallbackQuery().getData();
        if (rawInformation.length() > 13)
        {
            String information = rawInformation.substring(0, 13);
            Long pairUserChatId = Long.parseLong(rawInformation.substring(13));
            MyUser pairUser = userCache.getIfPresent(pairUserChatId);
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

        try(InputStream inputStream = this.getClass().getResourceAsStream("/texts/project_info.txt"))
        {
            if (inputStream == null)
                throw new IllegalArgumentException("Файл не найден!");

            String content = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));
            sendWindow(user, content);
        }
        catch (IOException e)
        {
            sendMessageWithNoSave(creatorChatId, "Ошибка при чтении из файла \"project_info.txt\"!");
        }
    }

    private void sendRules(MyUser user)
    {
        try(InputStream inputStream = this.getClass().getResourceAsStream("/texts/rules.txt"))
        {
            if (inputStream == null)
                throw new IllegalArgumentException("Файл не найден!");

            String content = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));
            sendWindow(user, content);
        }
        catch (IOException e)
        {
            sendMessageWithNoSave(creatorChatId, "Ошибка при чтении из файла \"rules.txt\"!");
        }
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



    private void sendWindow(MyUser user, String text)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text(text)
                .replyMarkup(LobbyMenu.backToMainMenuButton).build();
        saveToStack(user, gateway.send(message));
    }

    /**
     * метод для обработки принятия приглашения
     * @param whoAccepts кто принимает
     * @param whoInvites кто приглашает
     */
    private void treatAcceptInvite(MyUser whoAccepts, MyUser whoInvites)
    {
        Game newGame = new Game(whoInvites, whoAccepts);
        deleteLastMessage(whoInvites);
        deleteLastMessage(whoInvites);
        deleteInvitationMessage(whoAccepts, whoInvites.getChatId());
        deleteLastMessage(whoAccepts);
        prepareForShipSetting(whoInvites, whoAccepts, newGame);
        prepareForShipSetting(whoAccepts, whoInvites, newGame);
    }
    private void prepareForShipSetting(MyUser user1, MyUser user2, Game game)
    {
        user1.setState(State.LINCORE_SETTING);
        sessions.games().put(user1.getChatId(), game);
        sessions.userPairs().put(user1.getChatId(), user2.getChatId());
        sendField(user1, game.getOwnFields().get(user1.getChatId()), TIP.LINCORE);
    }
    /**
     * метод для отправки игрового поля
     * @param user кому отправить
     * @param field какое поле отправить
     * @param caption надпись над полем
     */
    public void sendField(MyUser user, TelegramField field, String caption)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text(caption)
                .replyMarkup(field.getKeyboardMarkup()).build();
        saveToStack(user, gateway.send(message));
    }
    /**
     * метод для обработки отклонения приглашения
     * @param whoRefuses кто отклоняет
     * @param whoInvites кто приглашает
     */
    private void treatRefuseInvite(MyUser whoRefuses, MyUser whoInvites)
    {
        deleteLastMessage(whoInvites);
        sendMessageWithNoSave(whoInvites.getChatId(),
                whoRefuses.getFirstName() + " отклонил твое приглашение.");
        deleteInvitationMessage(whoRefuses, whoInvites.getChatId());
    }
    /**
     * метод для обработки всех тектовых сообщений, вводимых пользователем
     * @param update входящие изменения
     */
    private void handleMessage(Update update) throws SQLException, ClassNotFoundException
    {
        Long chatId = update.getMessage().getChatId();
        MyUser currentUser = userCache.getIfPresent(chatId);
        if (currentUser != null)
        {
            switch (currentUser.getState())
            {
                case IN_LOBBY -> lobbyMessageHandler(currentUser, update);
                case WANT_TO_REPLAY, FINISHED_GAME -> endGameMessageHandler(currentUser, update);
                default -> gameMessageHandler(currentUser, update);
            }
        }
        else
        {
            currentUser = dataBaseHandler.pullUserFromDB(chatId);
            if (currentUser != null)
            {
                User user = update.getMessage().getFrom();
                checkUserNameUpdate(currentUser, user);
                userCache.put(chatId, currentUser);
                handleMessage(update);
                return;
            }
            if (update.getMessage().getText().equals(MessageCommand.START))
                registerUserAndGreet(chatId, update.getMessage().getFrom());
            else
                sendMessageWithNoSave(chatId, "Для авторизации напиши команду /start");
        }
    }
    private void gameMessageHandler(MyUser currentUser, Update update)
    {
        String text = update.getMessage().getText();
        switch (text)
        {
            case MessageCommand.PERMUTE -> { if (currentUser.getState().isPlacingShip()) permuteField(currentUser);}
            case MessageCommand.EXIT -> exitFromGameHandler(currentUser);
        }
    }
    private void exitFromGameHandler(MyUser currentUser)
    {
        int deleteMessageCounter;
        if (currentUser.getState().isPlacingShip() || currentUser.getState().equals(State.READY_TO_PLAY))
            deleteMessageCounter = 1;
        else if (sessions.games().get(currentUser.getChatId()).getFirstMovement().get(currentUser.getChatId()) == null)
            deleteMessageCounter = 3;
        else
            deleteMessageCounter = 4;
        leaveFromCurrentGame(currentUser, deleteMessageCounter);
    }
    private void permuteField(MyUser currentUser)
    {
        deleteLastMessage(currentUser);
        Game currentGame = sessions.games().get(currentUser.getChatId());
        currentGame.resetOwnField(currentUser);
        currentUser.setState(State.LINCORE_SETTING);
        sendField(currentUser, currentGame.getOwnFields().get(currentUser.getChatId()), TIP.LINCORE);
    }
    private void endGameMessageHandler(MyUser currentUser, Update update)
    {
        if (update.getMessage().getText().equals(MessageCommand.EXIT))
            leaveAfterGame(currentUser);
    }
    /**
     * метод для обработки текстовых сообщений пользователя, находящегося в лобби
     * @param currentUser текущий ползователь
     * @param update изменения
     */
    private void lobbyMessageHandler(MyUser currentUser, Update update)
    {
        Message message = update.getMessage();
        String text = message.getText();

        if (text.equals("Отменить приглашение❌"))
            cancelInvitation(currentUser);
        else if (text.charAt(0) == '@')
            treatPairUserPresence(text, currentUser);
    }
    /**
     * метод для проверки наличия в базе введенного текущим пользователем тега другого пользователя и
     * в случае успеха создание из них игровой пары
     * @param text текстовое сообщение
     * @param currentUser текущий пользователь
     */
    private void treatPairUserPresence(String text, MyUser currentUser)
    {
        String pairUserName = text.substring(1);

        MyUser invitedUser = dataBaseHandler.pullUserFromDB(pairUserName);
        if (invitedUser == null)
        {
            sendMessageWithNoSave(currentUser.getChatId(),
            """
                    Извини, я не вижу данного пользователя в своей системе по двум причинам:
                    1. Он поменял @userName и ему необходимо повзаимодействовать со мной, чтобы я обновил его данные
                    2. Он еще не писал мне команду /start""");
        }
        else
        {
            if (!userCache.asMap().containsKey(invitedUser.getChatId()))
                userCache.put(invitedUser.getChatId(), invitedUser);
            if (invitedUser.getState().equals(State.IN_LOBBY) &&
                    invitedUser.getChatId() != currentUser.getChatId())
            {
                sendInvite(invitedUser, currentUser);
                sendWaitingMessage(currentUser);
            }
            else if (!currentUser.getState().equals(State.IN_LOBBY))
            {
                sendMessageWithNoSave(currentUser.getChatId(),
                        "Извини, данный пользователь уже с кем-то играет");
            }
            else if (invitedUser.getChatId() == currentUser.getChatId())
                sendMessageWithNoSave(currentUser.getChatId(), "Опции игры с самим собой пока-что нет :(");
        }
    }
    /**
     * метод для отмены приглашения у приглашающего пользователя
     * @param invitingUser приглашающий пользователь
     */
    private void cancelInvitation(MyUser invitingUser)
    {
        deleteLastMessage(invitingUser);
        Long invitingUserChatId = invitingUser.getChatId();
        deleteInvitationMessage(sessions.invitedUsers().get(invitingUserChatId), invitingUserChatId);
    }

    private void deleteInvitationMessage(MyUser user, Long invitingUserChatId)
    {
        Integer messageId = sessions.invitationMessages().get(user.getChatId()).get(invitingUserChatId);
        sessions.invitationMessages().get(user.getChatId()).remove(invitingUserChatId);
        sessions.invitedUsers().remove(invitingUserChatId);

        DeleteMessage message = DeleteMessage.builder()
                .messageId(messageId)
                .chatId(user.getChatId()).build();
        gateway.delete(message);
    }

    private void registerUserAndGreet(Long chatId, User user) throws ClassNotFoundException
    {
        MyUser newUser = new MyUser(chatId, user.getUserName(), user.getFirstName(), State.IN_LOBBY);
        userCache.put(chatId, newUser);
        dataBaseHandler.insertUserIntoDB(newUser);
        sendGreetings(newUser);
    }
    /**
     * Метод, высылающий пользователю сообщение-приветствие
     * @param user кому отправить
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

        SendPhoto mainLobbyMenu = SendPhoto.builder()
                .chatId(user.getChatId())
                .photo(menuPicture)
                .caption("Ты находишься в лобби. Чтобы начать играть, пригласи пользователя, написав мне его @username "
            + "(обязательно с символом «@»!)")
                .replyMarkup(LobbyMenu.mainLobbyMenuKeyBoard).build();
        saveToStack(user, gateway.send(mainLobbyMenu));
    }
    /**
     * метод отправки сообщений
     * @param user кому отправить
     * @param whatToSend что отправить
     */
    /**
     * Сохраняет отправленное сообщение в стек пользователя, создавая стек при
     * необходимости. Если сообщение null (отправка не удалась), ничего не делает.
     */
    private void saveToStack(MyUser user, Message message)
    {
        if (message == null)
            return;
        sessions.messageStacks().computeIfAbsent(user.getChatId(), key -> new Stack<>()).add(message);
    }
    private void sendMessage(MyUser user, String whatToSend)
    {
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text(whatToSend).build();
        saveToStack(user, gateway.send(message));
    }
    private void sendMessageWithNoSave(Long chatId, String whatToSend)
    {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(whatToSend).build();
        gateway.send(message);
    }

    private void deleteMessage(MyUser user, Integer messageId)
    {
        DeleteMessage deleteMessage = DeleteMessage.builder()
                        .chatId(user.getChatId())
                        .messageId(messageId).build();
        gateway.delete(deleteMessage);
    }
    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param user в диалоге с кем удалить
     */
    private void deleteLastMessage(MyUser user)
    {
        Stack<Message> currentMessageStack = sessions.messageStacks().get(user.getChatId());
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(user.getChatId());
        if (currentMessageStack == null || currentMessageStack.isEmpty())
        {
            deleteMessage.setMessageId(user.getLastMessageId());
        }
        else
        {
            deleteMessage.setMessageId(currentMessageStack.pop().getMessageId());
        }

        gateway.delete(deleteMessage);
    }
    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param user в диалоге с кем удалить
     */
    private void deleteLastMessage(MyUser user, int times)
    {
        Stack<Message> currentMessageStack = sessions.messageStacks().get(user.getChatId());
        if (currentMessageStack == null)
            return;
        for (int i = 0; i < times && !currentMessageStack.isEmpty(); i++)
        {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(user.getChatId())
                    .messageId(currentMessageStack.pop().getMessageId()).build();
            gateway.delete(deleteMessage);
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
        SendMessage message = SendMessage.builder()
                .chatId(user.getChatId())
                .text("Приглашение отправлено. Ожидай ответа!")
                .replyMarkup(LobbyMenu.replyMarkupForWaitingMessage).build();
        saveToStack(user, gateway.send(message));
    }
    /**
     * метод для отправки приглашения на поединок
     * @param whoIsInvited приглашаемый пользователь
     * @param whoInvites приглашающий пользователь
     */
    private void sendInvite(MyUser whoIsInvited, MyUser whoInvites)
    {
        SendMessage message = SendMessage.builder()
                        .chatId(whoIsInvited.getChatId())
                        .text(whoInvites.getFirstName() + " приглашает тебя поиграть в морской бой!")
                        .replyMarkup(getInviteKeyboard(whoInvites)).build();
        sessions.invitedUsers().put(whoInvites.getChatId(), whoIsInvited);
        Message sendedMessage = gateway.send(message);
        if (sendedMessage != null)
        {
            Map<Long, Integer> invitationTable = sessions.invitationMessages().get(whoIsInvited.getChatId());
            if (invitationTable == null)
            {
                sessions.invitationMessages().put(whoIsInvited.getChatId(), new ConcurrentHashMap<>());
                invitationTable = sessions.invitationMessages().get(whoIsInvited.getChatId());
            }
            invitationTable.put(whoInvites.getChatId(), sendedMessage.getMessageId());
        }
    }
    /**
     * метод для формирования поля
     * @return поле для приглашаемого игрока
     */
    private InlineKeyboardMarkup getInviteKeyboard(MyUser user)
    {
        List<InlineKeyboardButton> row1 = Collections.singletonList(InlineKeyboardButton.builder()
                    .text("Принять приглашение✅")
                    .callbackData("accept_Invite" + user.getChatId()).build());

        List<InlineKeyboardButton> row2 = Collections.singletonList(InlineKeyboardButton.builder()
                    .text("Отклонить❌")
                    .callbackData("refuse_Invite" + user.getChatId()).build());

        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2)).build();
    }
    /**
     * геттер для имени бота
     * @return имя бота
     */
    @Override
    public String getBotUsername() { return this.botUserName; }
    /**
     * геттер для токена бота
     * @return токен бота
     */
    @Override
    public String getBotToken(){ return this.botToken; }
}
