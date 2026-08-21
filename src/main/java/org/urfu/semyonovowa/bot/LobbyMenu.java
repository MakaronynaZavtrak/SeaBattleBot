package org.urfu.semyonovowa.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Collections;
import java.util.List;

public final class LobbyMenu
{
    private LobbyMenu(){}
    private static InlineKeyboardMarkup getBackToMainMenuButton()
    {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text("⬅️Вернуться в главное меню")
                        .callbackData("back_to_main").build())).build();
    }
    public static final InlineKeyboardMarkup backToMainMenuButton = getBackToMainMenuButton();
    private static InlineKeyboardMarkup getKeyboardForSendingRepeatGame()
    {
        InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                .text("Сыграть еще раз✅")
                .callbackData("want_to_replay").build();
        InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                .text("Выйти в лобби ожидания❌")
                .callbackData("want_to_exit").build();
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(yesButton))
                .keyboardRow(new InlineKeyboardRow(noButton)).build();
    }
    public static final InlineKeyboardMarkup keyboardForSendingRepeatGame = getKeyboardForSendingRepeatGame();
    private static ReplyKeyboardMarkup getReplyMarkupForWaitingMessage()
    {
        KeyboardRow row1 = new KeyboardRow(List.of(new KeyboardButton("Отменить приглашение❌")));
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .keyboard(Collections.singletonList(row1)).build();
    }
    public static final ReplyKeyboardMarkup replyMarkupForWaitingMessage = getReplyMarkupForWaitingMessage();

    private static InlineKeyboardMarkup getMainLobbyMenuKeyboard()
    {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text("Моя статистика\uD83D\uDCC8")
                        .callbackData("my_stats").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text("Топ-10 пользователей\uD83C\uDFC6")
                        .callbackData("top_10").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text("Правила❓")
                        .callbackData("rules").build()))
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text("О проекте⚙️")
                        .callbackData("project_info").build())).build();
    }
    public static final InlineKeyboardMarkup mainLobbyMenuKeyBoard = getMainLobbyMenuKeyboard();
}
