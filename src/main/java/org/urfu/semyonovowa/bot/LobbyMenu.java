package org.urfu.semyonovowa.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class LobbyMenu
{
    private LobbyMenu(){}
    private static InlineKeyboardMarkup getBackToMainMenuButton()
    {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(Collections.singletonList(InlineKeyboardButton.builder()
                        .text("⬅️Вернуться в главное меню")
                        .callbackData("back_to_main").build()))).build();
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
        List<InlineKeyboardButton> row1 = Collections.singletonList(yesButton);
        List<InlineKeyboardButton> row2 = Collections.singletonList(noButton);
        return InlineKeyboardMarkup.builder()
                .keyboard(Arrays.asList(row1, row2)).build();
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
    public static final InlineKeyboardMarkup mainLobbyMenuKeyBoard = getMainLobbyMenuKeyboard();
}
