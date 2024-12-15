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
import org.urfu.semyonovowa.game.MovingInformation;
import org.urfu.semyonovowa.game.MovingInformationForBothPlayers;
import org.urfu.semyonovowa.field.TelegramField;
import org.urfu.semyonovowa.game.Game;
import org.urfu.semyonovowa.ship.Ship;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.State;

import java.util.*;
/**
 * Класс, в котором изложена логика обработки взаимодействия с пользователями телеграмма
 */
public class TelegramBot extends TelegramLongPollingBot
{
    private final Map<String, MyUser> allUsers;
    private final Map<String, String> userPairs;
    private final Map<Long, Stack<Message>> messageStacks = new HashMap<>();
    private final Map<String, Game> games = new HashMap<>();
    private final String botUserName;
    private final String botToken;
    public TelegramBot(String botUserName, String token)
    {
        super(token);
        this.botUserName = botUserName;
        this.botToken = token;
        allUsers = new HashMap<>();
        userPairs = new HashMap<>();
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
    /**
     * метод для обработки разрыва сеанса между двумя игроками после игры
     * @param currentUser - текущий пользователь 
     */
    private void leaveAfterGame(MyUser currentUser)
    {
        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));
        cleanTrailsLeaveAfterGame(currentUser, "", 4);
        cleanTrailsLeaveAfterGame(pairUser, currentUser.getUsername(), 4);
    }
    private void leaveAfterGame(MyUser currentUser, int times)
    {
        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));
        cleanTrailsLeaveAfterGame(currentUser, "", times);
        cleanTrailsLeaveAfterGame(pairUser, currentUser.getUsername(), times);
    }
    private void cleanTrailsLeaveAfterGame(MyUser user, String pairUserName, int times)
    {
        user.setState(State.IN_LOBBY);
        deleteLastMessage(user.getChatId(), times);

        String instructionMessage = "Ты снова оказался в лобби и " +
                "через @ можешь пригласить играть другого пользователя";

        if (!pairUserName.isEmpty())
            instructionMessage = "К сожалению, " + user.getFirstName() + " не хочет играть.\n" + instructionMessage;

        sendMessage(user.getChatId(), instructionMessage);
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

        MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));
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
        games.put(user.getUsername(), game);
        deleteLastMessage(user.getChatId(), 4);
        sendField(user.getChatId(), game.getOwnFields().get(user.getUsername()), TIP.LINCORE);
    }
    /**
     * метод для обработки любых ходов во время игры
     * @param currentUser - текущий пользователь
     * @param update - изменения
     */
    private void movingHandler(MyUser currentUser, Update update)
    {
        Game currentGame = games.get(currentUser.getUsername());
        String coordinates = update.getCallbackQuery().getData().substring(1);
        String pairUsername = userPairs.get(currentUser.getUsername());
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
        deleteLastMessage(user.getChatId(), 2);
        Stack<Message> userStack = messageStacks.get(user.getChatId());

        if (user.getState().equals(State.WAITING))
        {
            Message userMessagePeek = userStack.pop();
            editField(user.getChatId(), userStack.peek().getMessageId(),
                    game.getOwnFields().get(user.getUsername()));
            userStack.add(userMessagePeek);
        }
        else editField(user.getChatId(), userStack.peek().getMessageId(),
                game.getEnemyFields().get(user.getUsername()));

        user.setState(State.FINISHED_GAME);
        sendMessage(user.getChatId(), information);
        games.remove(user.getUsername());
        sendRepeatGame(user.getChatId());
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

        Boolean flag = game.getFirstMovement().get(user.getUsername());
        if (flag != null)
            treatNotFirstMovement(user, information, lastMessage);
        else
        {
            treatFirstMovement(user, information, lastMessage);
            game.getFirstMovement().put(user.getUsername(), true);
        }

        Message turn = userStack.pop();
        Message event = userStack.pop();

        if (userState.equals(State.WAITING))
        {
            Message userMessagePeek = userStack.pop();
            editField(user.getChatId(), userStack.peek().getMessageId(), game.getOwnFields().get(user.getUsername()));
            userStack.add(userMessagePeek);
        }
        else
            editField(user.getChatId(), userStack.peek().getMessageId(), game.getEnemyFields().get(user.getUsername()));

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
        editMessage(user.getChatId(), lastMessage, information);
        lastMessage.setText(information);
        String turn;
        switch (information)
        {
            case MovingInformation.CURRENT_USER_MISS -> {turn = "Сейчас ходит противник"; user.setState(State.WAITING);}
            case MovingInformation.PAIR_USER_MISS -> {turn = "Сейчас ходишь ты"; user.setState(State.MOVING);}
            case MovingInformation.CURRENT_USER_HURT, MovingInformation.CURRENT_USER_KILL -> turn = "Сейчас ходишь ты";
            default -> turn = "Сейчас ходит противник";
        }
        sendMessage(user.getChatId(), turn);//4
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
            editMessage(user.getChatId(), infoMsg, userInformation);
            infoMsg.setText(userInformation);
        }
        if (userInformation.equals(MovingInformation.CURRENT_USER_MISS))
        {
            user.setState(State.WAITING);
            editMessage(user.getChatId(), lastMessage, "Сейчас ходит противник");
            lastMessage.setText("Сейчас ходит противник");

        }
        else if (userInformation.equals(MovingInformation.PAIR_USER_MISS))
        {
            user.setState(State.MOVING);
            editMessage(user.getChatId(), lastMessage, "Сейчас ходишь ты");
            lastMessage.setText("Сейчас ходишь ты");
        }
        userMessageStack.add(lastMessage);
    }
    /**
     * метод для отправки сообщения на повторную игру
     * @param chatId - куда отправить
     */
    public void sendRepeatGame(Long chatId)
    {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Хочешь сыграть с этим игроком еще раз?")
                .replyMarkup(getKeyboardForSendingRepeatGame()).build();
        try
        {
            messageStacks.get(chatId).add(execute(message));
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
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
     * @param chatId - где отредактировать
     * @param message - какое сообщение отредактировать
     * @param editedText - на что отредактировать
     */
    public void editMessage(Long chatId, Message message, String editedText)
    {
        EditMessageText editedMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(message.getMessageId())
                .text(editedText).build();
        try
        {
            execute(editedMessage);
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
        final int LAST_SHIP = 6;
        Ship boat3 = currentGame.getShips().get(currentUser.getUsername()).get(LAST_SHIP);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, boat3))
        {
            if (boat3.getCoordinatesSet().size() == boat3.getLives())
            {
                currentUser.setState(State.READY_TO_PLAY);
                MyUser pairUser = allUsers.get(userPairs.get(currentUser.getUsername()));
                if (!pairUser.getState().equals(State.READY_TO_PLAY))
                {
                    editMessage(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek(),
                            "Подожди, твой противник еще расставляет корабли");
                    editField(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
                            currentGame.getOwnFields().get(currentUser.getUsername()));
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
        deleteLastMessage(user.getChatId());
        sendField(user.getChatId(), game.getOwnFields().get(user.getUsername()), "Твое поле:");
        sendField(user.getChatId(), game.getEnemyFields().get(user.getUsername()), "Поле твоего противника:");
        if (user.getUsername().equals((game.getCreator().getUsername())))
        {
            user.setState(State.MOVING);
            sendMessage(user.getChatId(), "Cейчас ходишь ты");
        }
        else
        {
            user.setState(State.WAITING);
            sendMessage(user.getChatId(), "Cейчас ходит противник");
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
        Ship boat2 = currentGame.getShips().get(currentUser.getUsername()).get(5);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, boat2))
        {
            if (boat2.getCoordinatesSet().size() == boat2.getLives())
                currentUser.setState(State.BOAT_3_SETTING);
            editField(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
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
        Ship boat1 = currentGame.getShips().get(currentUser.getUsername()).get(4);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, boat1))
        {
            if (boat1.getCoordinatesSet().size() == boat1.getLives())
                currentUser.setState(State.BOAT_2_SETTING);
            editField(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
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
        Ship esminez2 = currentGame.getShips().get(currentUser.getUsername()).get(3);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, esminez2))
        {
            if (esminez2.getCoordinatesSet().size() == esminez2.getLives())
            {
                currentUser.setState(State.BOAT_1_SETTING);
                editMessage(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek(), TIP.BOATS);
            }
            editField(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
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
        Ship esminez1 = currentGame.getShips().get(currentUser.getUsername()).get(2);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, esminez1))
        {
            if (esminez1.getCoordinatesSet().size() == esminez1.getLives())
                currentUser.setState(State.ESMINEZ_2_SETTTING);
            editField(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
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
        Ship cruiser = currentGame.getShips().get(currentUser.getUsername()).get(1);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, cruiser))
        {
            if (cruiser.getCoordinatesSet().size() == cruiser.getLives())
            {
                currentUser.setState(State.ESMINEZ_1_SETTTING);
                editMessage(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek(), TIP.ESMINEZS);
            }
            editField(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
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
        Ship linCore = currentGame.getShips().get(currentUser.getUsername()).get(0);
        if (currentGame.setCage(update.getCallbackQuery().getData(), currentUser, linCore))
        {
            if (linCore.getCoordinatesSet().size() == linCore.getLives())
            {
                currentUser.setState(State.CRUISER_SETTING);
                editMessage(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek(), TIP.CRUISER);
            }
            editField(currentUser.getChatId(), messageStacks.get(currentUser.getChatId()).peek().getMessageId(),
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
        EditMessageReplyMarkup editedField = EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(field.getKeyboardMarkup()).build();
        try
        {
            execute(editedField);
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
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
        String information = callBackData.substring(0, 13);
        String pairUserName = callBackData.substring(13);
        MyUser pairUser = allUsers.get(pairUserName);

        switch (information)
        {
            case "accept_Invite" -> treatAcceptInvite(currentUser, pairUser);
            case "refuse_Invite" -> treatRefuseInvite(currentUser, pairUser);
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
        prepareForShipSetting(whoAccepts, whoInvites, newGame);
        prepareForShipSetting(whoInvites, whoAccepts, newGame);
    }
    private void prepareForShipSetting(MyUser user1, MyUser user2, Game game)
    {
        deleteLastMessage(user1.getChatId());
        user1.setState(State.LINCORE_SETTING);
        games.put(user1.getUsername(), game);
        userPairs.put(user1.getUsername(), user2.getUsername());
        sendField(user1.getChatId(), game.getOwnFields().get(user1.getUsername()), TIP.LINCORE);
    }
    /**
     * метод для отправки игрового поля
     * @param chatId куда отправить
     * @param field какое поле отправить
     * @param caption надпись над полем
     */
    public void sendField(Long chatId, TelegramField field, String caption)
    {
        InlineKeyboardMarkup keyboardMarkup = field.getKeyboardMarkup();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(caption)
                .replyMarkup(keyboardMarkup).build();
        try
        {
            messageStacks.get(chatId).add(execute(message));
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * метод для обработки отклонения приглашения
     * @param whoRefuses - кто отклоняет
     * @param whoInvites - кто приглашает
     */
    private void treatRefuseInvite(MyUser whoRefuses, MyUser whoInvites)
    {
        cleanTrailsAfterInvitationRejection(whoRefuses, "Ты отклонил приглашение");
        cleanTrailsAfterInvitationRejection(whoInvites, whoRefuses.getFirstName() + " отклонил твое приглашение.");
    }
    private void cleanTrailsAfterInvitationRejection(MyUser user, String text)
    {
        deleteLastMessage(user.getChatId());
        sendMessage(user.getChatId(), text);
    }
    /**
     * метод для обработки всех тектовых сообщений, вводимых пользователем
     * @param update - входящие изменения
     */
    private void handleMessage(Update update)
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
            sendMessage(chatId, "Для авторизации напиши команду /start");
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
        else if (games.get(currentUser.getUsername()).getFirstMovement().get(currentUser.getUsername()) == null)
            deleteMessageCounter = 3;
        else
            deleteMessageCounter = 4;
        leaveAfterGame(currentUser, deleteMessageCounter);
    }
    private void permuteField(MyUser currentUser)
    {
        deleteLastMessage(currentUser.getChatId());
        Game currentGame = games.get(currentUser.getUsername());
        currentGame.resetOwnField(currentUser);
        currentUser.setState(State.LINCORE_SETTING);
        sendField(currentUser.getChatId(), currentGame.getOwnFields().get(currentUser.getUsername()), TIP.LINCORE);
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
                sendMessage(currentUser.getChatId(),
                        "Извини, данного пользователя нет в системе.");
            }
            else if (invitedUser.getState().equals(State.IN_LOBBY))
            {
                sendInvite(invitedUser.getChatId(), currentUser);
                sendWaitingMessage(currentUser.getChatId());
                //TODO: сделать очередь приглашений
            }
            else if (!currentUser.getState().equals(State.IN_LOBBY))
            {
                sendMessage(currentUser.getChatId(),
                        "Извини, данный пользователь уже с кем-то играет");
            }
        }
    }
    /**
     * метод для отмены приглашения у приглашающего пользователя
     * @param invitingUser - приглашающий пользователь
     */
    private void cancelInvitation(MyUser invitingUser)
    {
        MyUser pairUser = allUsers.get(userPairs.get(invitingUser.getUsername()));
        deleteLastMessage(invitingUser.getChatId());
        deleteLastMessage(pairUser.getChatId());
    }
    private void registerUserAndGreet(Long userChatId, User user)
    {
            allUsers.put(user.getUserName(),
                    new MyUser(userChatId, user.getUserName(), user.getFirstName(), State.IN_LOBBY));
            sendGreetings(userChatId, user.getFirstName());
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
        SendPhoto message = SendPhoto.builder()
                .photo(photo)
                .caption(caption)
                .chatId(chatId).build();
        try
        {
            execute(message);
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
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
            Stack<Message> currentMessageStack = messageStacks.get(chatId);
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(chatId, newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param chatId - в каком диалоге удалить
     */
    private void deleteLastMessage(Long chatId)
    {
        Stack<Message> currentMessageStack = messageStacks.get(chatId);
        DeleteMessage deleteMessage = DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(currentMessageStack.pop().getMessageId()).build();
        try
        {
            execute(deleteMessage);
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * по заданному chatId удаляет последнее сообщение бота в диалоге
     * @param chatId - в каком диалоге удалить
     */
    private void deleteLastMessage(Long chatId, int times)
    {
        Stack<Message> currentMessageStack = messageStacks.get(chatId);
        for (int i = 0; i < times; i++)
        {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(currentMessageStack.pop().getMessageId()).build();
            try
            {
                execute(deleteMessage);
            } catch (TelegramApiException e)
            {
                e.printStackTrace();
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
        KeyboardRow row1 = new KeyboardRow(List.of(new KeyboardButton("Отменить приглашение❌")));
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .keyboard(Collections.singletonList(row1)).build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Приглашение отправлено. Ожидай ответа!")
                .replyMarkup(keyboardMarkup).build();
        try
        {
            Message sendedMessage = execute(message);
            Stack<Message> currentMessageStack = messageStacks.get(chatId);
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(chatId, newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * метод для отправки приглашения на поединок
     * @param chatId - куда отправить
     * @param whoInvites - имя приглашающего пользователя
     */
    private void sendInvite(Long chatId, MyUser whoInvites)
    {
        SendMessage message = SendMessage.builder()
                        .chatId(chatId)
                        .text(whoInvites.getFirstName() + " приглашает тебя поиграть в морской бой!")
                        .replyMarkup(getInviteKeyboard(whoInvites)).build();
        try
        {
            Message sendedMessage = execute(message);
            Stack<Message> currentMessageStack = messageStacks.get(chatId);
            if (currentMessageStack == null)
            {
                Stack<Message> newStack = new Stack<>();
                newStack.add(sendedMessage);
                messageStacks.put(chatId, newStack);
            }
            else
            {
                currentMessageStack.add(sendedMessage);
            }
        }
        catch (TelegramApiException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * метод для формирования поля
     * @return поле для приглашаемого игрока
     */
    private InlineKeyboardMarkup getInviteKeyboard(MyUser user) {
        InlineKeyboardButton acceptButton = InlineKeyboardButton.builder()
                .text("Принять приглашение✅")
                .callbackData("accept_Invite" + user.getUsername()).build();
        InlineKeyboardButton refuseButton = InlineKeyboardButton.builder()
                .text("Отклонить❌")
                .callbackData("refuse_Invite" + user.getUsername()).build();
        List<InlineKeyboardButton> row1 = Collections.singletonList(acceptButton);
        List<InlineKeyboardButton> row2 = Collections.singletonList(refuseButton);
        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2)).build();
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
