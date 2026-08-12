package org.urfu.semyonovowa.field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Характеризационные тесты {@link TelegramField}: раскладка кнопок 8x8,
 * callbackData союзного/вражеского поля, счётчик жизней и правка клетки.
 * Эта раскладка «строка-координата» изменится на этапе 6 (битборд) — сетка
 * фиксирует текущее поведение.
 */
class TelegramFieldCharacterizationTest
{
    private TelegramField ownField()
    {
        TelegramField field = new TelegramField();
        field.setTelegramOwnField(new BaseField());
        return field;
    }

    @Test
    @DisplayName("поле состоит из 8 рядов по 8 кнопок")
    void boardIsEightByEight()
    {
        List<List<InlineKeyboardButton>> keyboard = ownField().getKeyboardMarkup().getKeyboard();

        assertThat(keyboard).hasSize(8);
        assertThat(keyboard.get(0)).hasSize(8);
    }

    @Test
    @DisplayName("клетки союзного поля инициализируются волной без префикса")
    void ownFieldCellsUseWaterWaveAndNoPrefix()
    {
        InlineKeyboardButton cell = ownField().getKeyboardMarkup().getKeyboard().get(0).get(0);

        assertThat(cell.getText()).isEqualTo(FieldEmoji.WATER_WAVE);
        assertThat(cell.getCallbackData()).isEqualTo("0 0");
    }

    @Test
    @DisplayName("клетки вражеского поля получают префикс E в callbackData")
    void enemyFieldCellsUsePrefix()
    {
        TelegramField enemy = new TelegramField();
        enemy.setTelegramEnemyField(new BaseField());

        InlineKeyboardButton cell = enemy.getKeyboardMarkup().getKeyboard().get(0).get(0);

        assertThat(cell.getCallbackData()).isEqualTo("E0 0");
    }

    @Test
    @DisplayName("editCage меняет эмодзи нужной клетки по координатам y x")
    void editCageUpdatesTargetCell()
    {
        TelegramField field = ownField();

        field.editCage("0 0", FieldEmoji.MISS_SIGN);

        InlineKeyboardButton cell = field.getKeyboardMarkup().getKeyboard().get(0).get(0);
        assertThat(cell.getText()).isEqualTo(FieldEmoji.MISS_SIGN);
    }
}
