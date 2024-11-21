package org.university.game;

import org.university.field.BaseField;
import org.university.field.TelegramField;
import org.university.ship.*;
import org.university.user.MyUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * основной класс для обработки логики единичной игры
 */
public class Game
{
    BaseField baseFieldCreator = new BaseField();
    BaseField baseFieldInvited = new BaseField();
    private final MyUser creator;
    private final MyUser invitedUser;
    private final Map<String, TelegramField> ownFields;
    private final Map<String, TelegramField> enemyFields;
    private final Map<String, List<Ship>> ships;
    private boolean firstMove;
    public MyUser getCreator(){return this.creator;}
    public MyUser getInvitedUser(){return this.invitedUser;}
    public Map<String, TelegramField> getOwnFields(){return this.ownFields;}
    public Map<String, TelegramField> getEnemyFields(){return this.enemyFields;}
    public Map<String, List<Ship>> getShips(){return this.ships;}
    public Game(MyUser creator, MyUser invitedUser)
    {
        this.creator = creator;
        this.invitedUser = invitedUser;
        this.ownFields = new HashMap<>();
        this.enemyFields = new HashMap<>();
        this.ships = new HashMap<>();
        this.firstMove = true;

        fieldInitialize(creator, baseFieldCreator);
        fieldInitialize(invitedUser, baseFieldInvited);
    }
    private void fieldInitialize (MyUser user, BaseField baseField)
    {
        TelegramField userOwnField = new TelegramField();
        userOwnField.setTelegramOwnField(baseField);

        ownFields.put(user.getUsername(), userOwnField);

        TelegramField userEnemyField = new TelegramField();
        userEnemyField.setTelegramEnemyField(baseField);

        enemyFields.put(user.getUsername(), userEnemyField);

        LinCore userLincore = new LinCore();

        Cruiser userCruiser = new Cruiser();

        Esminez userEsminez1 = new Esminez();
        Esminez userEsminez2 = new Esminez();

        Boat userBoat1 = new Boat();
        Boat userBoat2 = new Boat();
        Boat userBoat3 = new Boat();

        List<Ship> userShips = new ArrayList<>();

        userShips.add(userLincore);
        userShips.add(userCruiser);
        userShips.add(userEsminez1);
        userShips.add(userEsminez2);
        userShips.add(userBoat1);
        userShips.add(userBoat2);
        userShips.add(userBoat3);

        ships.put(user.getUsername(), userShips);
    }
    /**
     * Устанавливает у пользователя user ячейку жизни корабля ship по координатам coordinates, если это возможно
     * @param coordinates координаты, куда ставится ячейка корабля
     * @param user пользователь, чье поле изменяется
     * @param ship корабль, чья ячейка жизни ставится
     * @return true - если изменения на поле произошли, false - иначе
     */
    public boolean setCage(String coordinates, MyUser user, Ship ship)
    {
        boolean didFieldChanged = false;
        TelegramField currentField = ownFields.get(user.getUsername());
        if (currentField.isInCorrectPosition(coordinates, ship))
        {
            if (ship.addCoordinates(coordinates))
            {
                currentField.changeCage(coordinates, "\uD83D\uDEA2");
                currentField.getField().put(coordinates, ship);
                didFieldChanged = true;
            }
        }
        return didFieldChanged;
    }
    /**
     * Содержит в себе логическую обработку хода каждого игрока, который отображает на поле
     * @param atacker пользователь, который ходит
     * @param coordinates координаты, на которые ткнул ходящий пользователь
     * @return сообщение о результате выстрела
     */
    public String attack(MyUser atacker, String coordinates)
    {
        String information = "Ты промахнулся :(";
        MyUser defender;
        if (atacker.getUsername().equals(creator.getUsername()))
            defender = invitedUser;
        else defender = creator;
        TelegramField enemyField = ownFields.get(defender.getUsername());
        if (enemyField.getField().containsKey(coordinates))
        {
            if (enemyField.getField().get(coordinates).getLives() - 1 > 0)
            {
                enemyField.getField().get(coordinates).setLives(enemyField.getField().get(coordinates).getLives() - 1);
                information = "Ты нанес ранение!";
                enemyField.changeCage(coordinates, "\uD83C\uDFAF");
                enemyFields.get(atacker.getUsername()).changeCage(coordinates, "\uD83C\uDFAF");
                enemyField.getField().get(coordinates).getDamagedCages().add(coordinates);
            }
            else
            {
                information = "Ты потопил вражеский корабль!";
                enemyField.changeCage(coordinates, "☠");
                enemyFields.get(atacker.getUsername()).changeCage(coordinates, "☠");
                for (String coord : enemyField.getField().get(coordinates).getDamagedCages())
                {
                    enemyField.changeCage(coord, "☠");
                    enemyFields.get(atacker.getUsername()).changeCage(coord, "☠");
                }
            }
            enemyField.setAllLives(enemyField.getAllLives() - 1);
        }
        //miss
        else
        {
            enemyField.changeCage(coordinates, "\uD83D\uDEAB");
            enemyFields.get(atacker.getUsername()).changeCage(coordinates, "\uD83D\uDEAB");
        }
        if (enemyField.getAllLives() == 0)
            information = "Поздравляю, ты победил!\uD83C\uDFC6";

        return information;
    }

    public boolean isFirstMove() {return firstMove;}

    public void setFirstMove(boolean firstMove) {this.firstMove = firstMove;}
}
