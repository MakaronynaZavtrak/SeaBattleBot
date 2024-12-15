package org.urfu.semyonovowa.field;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.urfu.semyonovowa.ship.Ship;
import java.util.*;
public class TelegramField
{
    private final Map<String, Ship> shipsMap;
    private int allLives;
    private final InlineKeyboardMarkup keyboardMarkup;
    private final Set<String> usedCages;
    public TelegramField()
    {
        this.shipsMap = new HashMap<>();
        this.allLives = 14;
        this.keyboardMarkup = new InlineKeyboardMarkup();
        this.usedCages = new HashSet<>();
    }
    public int getAllLives(){return this.allLives;}
    public void decreaseAllLivesByOne(){this.allLives--;}
    public Map<String, Ship> getShipsMap(){return this.shipsMap;}
    public InlineKeyboardMarkup getKeyboardMarkup(){return this.keyboardMarkup;}
    public Set<String> getUsedCages(){return this.usedCages;}
    /**
     * Обобщенный метод установки поля
     * @param baseField - базовое изолированное от Telegram API поле
     * @param mark - пометка является поле вражеским или союзным
     */
    private void setTelegramField(BaseField baseField, String mark)
    {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (ArrayList<FieldCell> readRow: baseField.getFieldCellList())
        {
            List<InlineKeyboardButton> currentRow = new ArrayList<>();
            readRow.forEach((elem) -> currentRow.add(InlineKeyboardButton.builder()
                                        .text(elem.emoji)
                                        .callbackData(mark + elem.coordinate).build()));
            rows.add(currentRow);
        }
        keyboardMarkup.setKeyboard(rows);
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
        List<List<InlineKeyboardButton>> buttons = keyboardMarkup.getKeyboard();
        buttons.get(y).get(x).setText(emoji);
        keyboardMarkup.setKeyboard(buttons);
    }
}
