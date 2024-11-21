package org.university.field;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.university.ship.Ship;
import java.util.*;
public class TelegramField
{
    private final Map<String, Ship> field;
    private int allLives;
    private final InlineKeyboardMarkup keyboardMarkup;
    private final Set<String> usedCages;
    public TelegramField()
    {
        this.field = new HashMap<>();
        this.allLives = 14;
        this.keyboardMarkup = new InlineKeyboardMarkup();
        this.usedCages = new HashSet<>();
    }
    public int getAllLives(){return this.allLives;}
    public void setAllLives(int lives){this.allLives = lives;}
    public Map<String, Ship> getField(){return this.field;}
    public InlineKeyboardMarkup getKeyboardMarkup(){return this.keyboardMarkup;}
    public Set<String> getUsedCages(){return this.usedCages;}

    /**
     * Обобщенный метод установки поля
     * @param baseField - базовое изолированное от API поле
     * @param mark - пометка является поле вражеским или союзным
     */
    private void setTelegramField(BaseField baseField, String mark)
    {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < 8; i++)
        {
            List<InlineKeyboardButton> currentRow = new ArrayList<>();
            for (int j = 0; j < 8; j++)
            {
                InlineKeyboardButton currentButton = new InlineKeyboardButton();
                FieldCell currentCell = baseField.getFieldCellList().get(i).get(j);
//                char row = (char)(currentCell.getX() + 97);
//                char column = (char)(currentCell.getY() + 48);
                String row = String.valueOf((char)(currentCell.getY() + 97));
                String column = String.valueOf(currentCell.getX());

                String coordinate = mark + row + column;
                currentButton.setText(currentCell.getEmodji());
                currentButton.setCallbackData(coordinate);
                currentRow.add(currentButton);
            }
            rows.add(currentRow);
        }
        keyboardMarkup.setKeyboard(rows);
    }
    /**
     * Предназначен для полей, которые высвечиваются у пользователей, как их собственные
     */
    public void setTelegramOwnField(BaseField baseField)
    {
        setTelegramField(baseField, "");
    }
    /**
     * Предназначен для инициализации полей для пользователей, которые являются для них как поля противников
     */
    public void setTelegramEnemyField(BaseField baseField) {setTelegramField(baseField, "E");}
    /**
     * Проверяет, находится ли хотя бы одна ячейка другого корабля в расстоянии одной клетки от координат coordinates
     * @param coordinates координаты, от которых ведется проверка
     * @param currentShip корабль, чью ячейку жизни проверяют
     * @return true
     */
    public boolean isInCorrectPosition(String coordinates, Ship currentShip)
    {
        if (field.containsKey(coordinates))
            return false;

        int y = coordinates.charAt(0) - (int)'a';
        int x = Integer.parseInt(coordinates.substring(1));

        String neighbourCellCoordinates;

        if (y > 0)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97 - 1)) + String.valueOf(x);
            if (isNotInCorrectPosition(neighbourCellCoordinates, currentShip))
                return false;
        }
        if (y > 0 && x < 7)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97 - 1)) + String.valueOf(x + 1);
            if (isNotInCorrectPosition(neighbourCellCoordinates, currentShip))
                return false;
        }
        if (x < 7)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97)) + String.valueOf(x + 1);
            if (isNotInCorrectPosition(neighbourCellCoordinates, currentShip))
                return false;
        }
        if (y < 7 && x < 7)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97 + 1)) + String.valueOf(x + 1);
            if (isNotInCorrectPosition(neighbourCellCoordinates, currentShip))
                return false;
        }
        if (y < 7)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97 + 1)) + String.valueOf(x);
            if (isNotInCorrectPosition(neighbourCellCoordinates, currentShip))
                return false;
        }
        if (y < 7 && x > 0)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97 + 1)) + String.valueOf(x - 1);
            if (isNotInCorrectPosition(neighbourCellCoordinates, currentShip))
                return false;
        }
        if (x > 0)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97)) + String.valueOf(x - 1);
            if (isNotInCorrectPosition(neighbourCellCoordinates, currentShip))
                return false;
        }
        if (y > 0 && x > 0)
        {
            neighbourCellCoordinates = String.valueOf((char)(y + 97 - 1)) + String.valueOf(x - 1);
            return !isNotInCorrectPosition(neighbourCellCoordinates, currentShip);
        }
        return true;
    }

    public boolean isNotInCorrectPosition(String neighbourCellCoordinates, Ship currentShip)
    {
        return field.containsKey(neighbourCellCoordinates) && !field.get(neighbourCellCoordinates).equals(currentShip);
    }
    public void changeCage(String coordinates, String emoji)
    {
        int y = coordinates.charAt(0) - (int)'a';
        int x = Integer.parseInt(coordinates.substring(1));
        List<List<InlineKeyboardButton>> buttons = keyboardMarkup.getKeyboard();
        buttons.get(x).get(y).setText(emoji);
        keyboardMarkup.setKeyboard(buttons);
    }
}
