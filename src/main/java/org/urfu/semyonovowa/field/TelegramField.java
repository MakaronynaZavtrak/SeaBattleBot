package org.urfu.semyonovowa.field;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.urfu.semyonovowa.ship.Ship;

import java.util.*;
public class TelegramField
{
    private final Map<String, Ship> shipsMap;
    private List<InlineKeyboardRow> keyboard;
    private final Set<String> usedCages;
    public TelegramField()
    {
        this.shipsMap = new HashMap<>();
        this.keyboard = new ArrayList<>();
        this.usedCages = new HashSet<>();
    }
    public Map<String, Ship> getShipsMap(){return this.shipsMap;}
    /**
     * Строит свежую разметку из текущих рядов. В telegrambots 10.x объекты API
     * неизменяемы (builder-only), поэтому храним ряды сами и собираем markup по требованию.
     */
    public InlineKeyboardMarkup getKeyboardMarkup(){return InlineKeyboardMarkup.builder().keyboard(keyboard).build();}
    public Set<String> getUsedCages(){return this.usedCages;}
    /**
     * Обобщенный метод установки поля
     * @param baseField - базовое изолированное от Telegram API поле
     * @param mark - пометка является поле вражеским или союзным
     */
    private void setTelegramField(BaseField baseField, String mark)
    {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (ArrayList<FieldCell> readRow: baseField.getFieldCellList())
        {
            InlineKeyboardRow currentRow = new InlineKeyboardRow();
            readRow.forEach((elem) -> currentRow.add(InlineKeyboardButton.builder()
                                        .text(elem.emoji)
                                        .callbackData(mark + elem.coordinate).build()));
            rows.add(currentRow);
        }
        this.keyboard = rows;
    }
    /**
     * Предназначен для полей, которые высвечиваются у пользователей, как их собственные
     */
    public void setTelegramOwnField(BaseField baseField) {setTelegramField(baseField, "");}
    /**
     * Предназначен для инициализации полей для пользователей, которые являются для них как поля противников
     */
    public void setTelegramEnemyField(BaseField baseField) {setTelegramField(baseField, "E");}
    public void editCage(String coordinates, String emoji)
    {
        String[] separatedCoordinates = coordinates.split(" ");
        int y = Integer.parseInt(separatedCoordinates[0]);
        int x = Integer.parseInt(separatedCoordinates[1]);
        InlineKeyboardRow row = keyboard.get(y);
        InlineKeyboardButton updated = InlineKeyboardButton.builder()
                .text(emoji)
                .callbackData(row.get(x).getCallbackData()).build();
        row.set(x, updated);
    }
}
