package org.urfu.semyonovowa.game;

import org.urfu.semyonovowa.field.FieldEmoji;
import org.urfu.semyonovowa.field.BaseField;
import org.urfu.semyonovowa.field.TelegramField;
import org.urfu.semyonovowa.ship.*;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.State;

import java.util.*;

import static java.lang.Math.*;

/**
 * основной класс для обработки логики единичной игры
 */
public class Game
{
    private final MyUser creator;
    private final MyUser invitedUser;
    private final Map<String, TelegramField> ownFields;
    private final Map<String, TelegramField> enemyFields;
    private final Map<String, List<Ship>> ships;
    private final Map<String, Boolean> firstMovement;
    public MyUser getCreator(){return this.creator;}
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
        this.firstMovement = new HashMap<>();

        BaseField baseFieldTemplate = new BaseField();
        fieldInitialize(creator, baseFieldTemplate);
        fieldInitialize(invitedUser, baseFieldTemplate);
    }
    private void fieldInitialize (MyUser user, BaseField baseField)
    {
        TelegramField userOwnField = new TelegramField();
        userOwnField.setTelegramOwnField(baseField);
        ownFields.put(user.getUserName(), userOwnField);

        TelegramField userEnemyField = new TelegramField();
        userEnemyField.setTelegramEnemyField(baseField);
        enemyFields.put(user.getUserName(), userEnemyField);

        ships.put(user.getUserName(), getUserShips());
    }
    private List<Ship> getUserShips()
    {
        LinCore linCore = new LinCore();
        Cruiser cruiser = new Cruiser();
        Esminez esminez1 = new Esminez();
        Esminez esminez2 = new Esminez();
        Boat boat1 = new Boat();
        Boat boat2 = new Boat();
        Boat boat3 = new Boat();
        return new ArrayList<>(Arrays.asList(linCore, cruiser, esminez1, esminez2, boat1, boat2, boat3));
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
        TelegramField field = ownFields.get(user.getUserName());
        if (field.getShipsMap().containsKey(coordinates))
            return false;
        switch (user.getState())
        {
            case State.LINCORE_SETTING -> {return setLinCoreCage(coordinates, field, ship);}
            case State.CRUISER_SETTING -> {return setCruiserCage(coordinates, field, ship);}
            case State.ESMINEZ_1_SETTTING, State.ESMINEZ_2_SETTTING -> {return setEsminezCage(coordinates, field, ship);}
            default -> {return setBoatCage(coordinates, field, ship);}
        }
    }

    private boolean setBoatCage(String coordinates, TelegramField field, Ship ship)
    {
        String[] separatedCoordinates = coordinates.split(" ");
        int y = Integer.parseInt(separatedCoordinates[0]);
        int x = Integer.parseInt(separatedCoordinates[1]);

        if (!isInCorrectPosition(y, x, ship, field.getShipsMap()))
            return false;

        treatSingleCage(coordinates, field, ship);
        return true;
    }

    private boolean setEsminezCage(String coordinates, TelegramField field, Ship ship)
    {
        String[] separatedCoordinates = coordinates.split(" ");
        int y = Integer.parseInt(separatedCoordinates[0]);
        int x = Integer.parseInt(separatedCoordinates[1]);

        if (!isInCorrectPosition(y, x, ship, field.getShipsMap()))
            return false;
        if (ship.getCoordinatesSet().isEmpty())
        {
            ShipConfiguration configuration = findWaysToConfigureTheShip(y, x, ship, field.getShipsMap());
            switch (configuration.getAmountWays())
            {
                case 1 -> {configureTheShip(configuration, field, ship); return true;}
                case 0 -> {return false;}
                default -> {treatSingleCage(coordinates, field, ship); return true;}
            }
        }
        else
        {
            int varUnitIdx = (ship.getCoordinatesSet().size() == 1)
                    ? defineShipOrientation(separatedCoordinates, ship)
                    : ship.getOrientation();
            if (varUnitIdx == 404)
                return false;

            int[] values = {y, x};
            int variableUnit = values[varUnitIdx];
            int fixedUnit = values[1 - varUnitIdx];

            if (fixedUnit != ship.getFixedVal())
                return false;

            String[] firstCage = ship.getCoordinatesSet().stream().findFirst().get().split(" ");
            int firstVariable = Integer.parseInt(firstCage[varUnitIdx]);
            if (abs(variableUnit - firstVariable) >= ship.getLives())
                return false;
            treatSingleCage(coordinates, field, ship);
            return true;
        }
    }

    private boolean setCruiserCage(String coordinates, TelegramField field, Ship ship)
    {
        String[] separatedCoordinates = coordinates.split(" ");
        int y = Integer.parseInt(separatedCoordinates[0]);
        int x = Integer.parseInt(separatedCoordinates[1]);

        if (!isInCorrectPosition(y, x, ship, field.getShipsMap()))
            return false;

        if (ship.getCoordinatesSet().isEmpty())
        {
            ShipConfiguration configuration = findWaysToConfigureTheShip(y, x, ship, field.getShipsMap());
            switch (configuration.getAmountWays())
            {
                case 1 -> {configureTheShip(configuration, field, ship); return true;}
                case 0 -> {return false;}
                default -> {treatSingleCage(coordinates, field, ship); return true;}
            }
        }
        else
        {
            int varUnitIdx = (ship.getCoordinatesSet().size() == 1)
                    ? defineShipOrientation(separatedCoordinates, ship)
                    : ship.getOrientation();
            if (varUnitIdx == 404)
                return false;

            int[] values = {y, x};
            int variableUnit = values[varUnitIdx];
            int fixedUnit = values[1 - varUnitIdx];

            if (fixedUnit != ship.getFixedVal())
                return false;

            int minVariable = variableUnit;
            int maxVariable = variableUnit;

            for (String cage: ship.getCoordinatesSet())
            {
                int currentVariable = Integer.parseInt(cage.split(" ")[varUnitIdx]);
                if (abs(variableUnit - currentVariable) >= ship.getLives())
                    return false;
                minVariable = min(minVariable, currentVariable);
                maxVariable = max(maxVariable, currentVariable);
            }

            if (maxVariable - minVariable == ship.getLives() - 1)
            {
                if (varUnitIdx == 0)
                {
                    for (int i = minVariable; i < ship.getLives() + minVariable; i++)
                        treatSingleCage(i + " " + fixedUnit, field, ship);
                }
                else
                {
                    for (int i = minVariable; i < ship.getLives() + minVariable; i++)
                        treatSingleCage(fixedUnit + " " + i, field, ship);
                }
                return true;
            }

            int[] beforeMinVar;
            int[] afterMaxVar;

            if (varUnitIdx == 0)
            {
                beforeMinVar = new int[]{minVariable - 1, fixedUnit};
                afterMaxVar = new int[]{maxVariable + 1, fixedUnit};
            }
            else
            {
                beforeMinVar = new int[]{fixedUnit, minVariable - 1};
                afterMaxVar = new int[]{fixedUnit, maxVariable + 1};
            }

            if ((minVariable == 0 || !isInCorrectPosition(beforeMinVar[0], beforeMinVar[1], ship, field.getShipsMap()))
                    && isInCorrectPosition(afterMaxVar[0], afterMaxVar[1], ship, field.getShipsMap())
                    && afterMaxVar[0] >= 0 && afterMaxVar[0] <= 7 && afterMaxVar[1] >= 0 && afterMaxVar[1] <= 7)
            {
                fillShipCages(minVariable, fixedUnit, varUnitIdx, 1, ship, field);
                return true;
            }
            else if ((maxVariable == 7 || !isInCorrectPosition(afterMaxVar[0], afterMaxVar[1], ship, field.getShipsMap()))
                    && isInCorrectPosition(beforeMinVar[0], beforeMinVar[1], ship, field.getShipsMap())
                    && beforeMinVar[0] >= 0 && beforeMinVar[0] <= 7 && beforeMinVar[1] >= 0 && beforeMinVar[1] <= 7)
            {
                fillShipCages(maxVariable, fixedUnit, varUnitIdx, -1, ship, field);
                return true;
            }

            if (varUnitIdx == 0)
            {
                for (int i = minVariable; i < ship.getLives() + minVariable; i++)
                {
                    if (!isInCorrectPosition(i, fixedUnit, ship, field.getShipsMap()))
                        return false;
                }
            }
            else
            {
                for (int i = minVariable; i < ship.getLives() + minVariable; i++)
                {
                    if (!isInCorrectPosition(fixedUnit, i, ship, field.getShipsMap()))
                        return false;
                }
            }
            treatSingleCage(coordinates, field, ship);
            return true;
        }
    }

    private boolean setLinCoreCage(String coordinates, TelegramField field, Ship ship)
    {
        if (!ship.getCoordinatesSet().isEmpty())
        {
            String[] separatedCoordinates = coordinates.split(" ");
            int varUnitIdx = (ship.getCoordinatesSet().size() == 1)
                    ? defineShipOrientation(separatedCoordinates, ship)
                    : ship.getOrientation();
            if (varUnitIdx == 404)
                return false;

            int variableUnit = Integer.parseInt(separatedCoordinates[varUnitIdx]);
            int fixedUnit = Integer.parseInt(separatedCoordinates[1 - varUnitIdx]);

            if (fixedUnit != ship.getFixedVal())
                return false;

            int minVariable = variableUnit;
            int maxVariable = variableUnit;

            for (String cage : ship.getCoordinatesSet())
            {
                int currentVariable = Integer.parseInt(cage.split(" ")[varUnitIdx]);
                if (abs(variableUnit - currentVariable) >= ship.getLives())
                    return false;
                minVariable = min(minVariable, currentVariable);
                maxVariable = max(maxVariable, currentVariable);
            }

            if (minVariable == 0)
            {
                fillShipCages(minVariable, fixedUnit, varUnitIdx, 1, ship, field);
                return true;
            }
            else if (maxVariable == 7)
            {
                fillShipCages(maxVariable, fixedUnit, varUnitIdx, -1, ship, field);
                return true;
            }

            if (maxVariable - minVariable == ship.getLives() - 1)
            {
                fillShipCages(minVariable, fixedUnit, varUnitIdx, 1, ship, field);
                return true;
            }
        }
        treatSingleCage(coordinates, field, ship);
        return true;
    }

    private int defineShipOrientation(String[] separatedCoordinates, Ship ship)
    {
        String[] separatedFirstCoordinates = ship.getCoordinatesSet().stream().findFirst().get().split(" ");
        int firstY = Integer.parseInt(separatedFirstCoordinates[0]);
        int firstX = Integer.parseInt(separatedFirstCoordinates[1]);

        int y = Integer.parseInt(separatedCoordinates[0]);
        int x = Integer.parseInt(separatedCoordinates[1]);

        if (x == firstX)
        {
            ship.setOrientation(0);
            ship.setFixedVal(x);
            return 0;
        }
        if (y == firstY)
        {
            ship.setOrientation(1);
            ship.setFixedVal(y);
            return 1;
        }
        return 404;
    }

    private void configureTheShip(ShipConfiguration configuration, TelegramField field, Ship ship)
    {
        int[] values = {configuration.getY(), configuration.getX()};
        int variableUnit = values[configuration.getVarUnitIdx()];
        int fixedUnit = values[1 - configuration.getVarUnitIdx()];
        if (configuration.getVarUnitIdx() == 1)
        {
            for (int i = 0; abs(i) < ship.getLives(); i += configuration.getStep())
                treatSingleCage(fixedUnit + " " + (variableUnit + i), field, ship);
        }
        else
        {
            for (int i = 0; abs(i) < ship.getLives(); i += configuration.getStep())
                treatSingleCage((variableUnit + i) + " " + fixedUnit, field, ship);
        }
    }

    private void treatSingleCage(String coordinates, TelegramField field, Ship ship)
    {
        field.editCage(coordinates, FieldEmoji.SHIP_SIGN);
        ship.getCoordinatesSet().add(coordinates);
        field.getShipsMap().put(coordinates, ship);
    }

    private void fillShipCages(int minVariableUnit, int fixed, int varUnitIdx, int step, Ship ship, TelegramField field)
    {
     if (varUnitIdx == 1)
     {
         for (int i = 0; abs(i) < ship.getLives(); i += step)
            treatSingleCage(fixed + " " + (minVariableUnit + i), field, ship);
     }
     else
     {
         for (int i = 0; abs(i) < ship.getLives(); i += step)
             treatSingleCage((minVariableUnit + i) + " " + fixed, field, ship);
     }
    }

    private ShipConfiguration findWaysToConfigureTheShip(int y, int x, Ship ship, Map<String, Ship> shipsMap)
    {
        int amountWays = 0;
        ShipConfiguration configuration = new ShipConfiguration();

        for (int varUnitIdx = 0; varUnitIdx < 2; varUnitIdx++)
        {
            int positiveDif = 0;
            int negativeDif = 0;
            for (int step = -1; step < 2; step += 2)
            {
                negativeDif = positiveDif;
                positiveDif = isValidWay(y, x, varUnitIdx, step, ship, shipsMap);
                if (positiveDif == ship.getLives() - 1)
                    if (!configuration.canBeConfigured())
                        configuration.initializeConfiguration(y, x, varUnitIdx, step);
            }
            if (positiveDif + negativeDif + 1 >= ship.getLives())
                amountWays += min(positiveDif, negativeDif) + 1;

            if (amountWays > 1)
            {
                configuration.setAmountWays(amountWays);
                return configuration;
            }

        }
        configuration.setAmountWays(amountWays);
        return configuration;
    }
    private int isValidWay(int y, int x, int varUnitIdx, int step, Ship ship, Map<String, Ship> shipsMap)
    {
        int[] values = {y, x};
        int variableUnit = values[varUnitIdx];
        int fixedUnit = values[1 - varUnitIdx];

        int freeCages = 0;

        if (varUnitIdx == 1)
        {
            for (int i = step; abs(i) < ship.getLives() && variableUnit + i <= 7 && variableUnit + i >= 0; i += step)
            {
                if (!isInCorrectPosition(fixedUnit, variableUnit + i, ship, shipsMap))
                    return freeCages;
                freeCages++;
            }
        }
        else
        {
            for (int i = step; abs(i) < ship.getLives() && variableUnit + i <= 7 && variableUnit + i >= 0; i += step)
            {
                if (!isInCorrectPosition(variableUnit + i, fixedUnit, ship, shipsMap))
                    return freeCages;
                freeCages++;
            }
        }
        return freeCages;
    }
    /**
     * Проверяет, находится ли хотя бы одна ячейка другого корабля в расстоянии одной клетки от координат coordinates
     * @param currentShip корабль, чью ячейку жизни проверяют
     * @return true
     */
    public boolean isInCorrectPosition(int y, int x, Ship currentShip, Map<String, Ship> shipsMap)
    {
        for (int i = -1; i < 2; i++)
        {
            for (int j = -1; j < 2; j++)
            {
                if (y + i >= 0 && x + j >= 0)
                    if (nearOtherShip((y + i) + " " + (x + j), currentShip, shipsMap))
                        return false;
            }
        }
        return true;
    }
    public boolean nearOtherShip(String coordinates, Ship currentShip, Map<String, Ship> shipsMap)
    {
        Ship someShip = shipsMap.get(coordinates);
        return someShip != null && !someShip.equals(currentShip);
    }
    /**
     * Содержит в себе логическую обработку хода каждого игрока, который отображает на поле
     * @param attacker пользователь, который ходит
     * @param coordinates координаты, на которые ткнул ходящий пользователь
     * @return сообщение о результате выстрела
     */
    public MovingInformationForBothPlayers attack(MyUser attacker, String coordinates)
    {
        MyUser defender = (attacker.getUserName().equals(creator.getUserName())) ?  invitedUser : creator;
        TelegramField enemyField = ownFields.get(defender.getUserName());
        Ship currentShip = enemyField.getShipsMap().get(coordinates);

        if (currentShip != null)
        {
            enemyField.decreaseAllLivesByOne();
            currentShip.getDamagedCages().add(coordinates);
            return (enemyField.getShipsMap().get(coordinates).getLives() - 1 > 0)
                    ? treatShipHurt(attacker, coordinates, enemyField, currentShip)
                    : treatShipKilling(attacker, enemyField, currentShip);
        }

        return treatMissMovement(enemyField, coordinates, attacker);
    }
    private MovingInformationForBothPlayers treatMissMovement(TelegramField enemyField, String coordinates,
                                                              MyUser attacker)
    {
        enemyField.editCage(coordinates, FieldEmoji.MISS_SIGN);
        enemyFields.get(attacker.getUserName()).editCage(coordinates, FieldEmoji.MISS_SIGN);
        return MovingInformationForBothPlayers.MISS_INFO;
    }
    private MovingInformationForBothPlayers treatShipKilling(MyUser attacker, TelegramField enemyField,
                                                             Ship currentShip)
    {
        for (String coordinate : currentShip.getDamagedCages())
        {
            enemyField.editCage(coordinate, FieldEmoji.KILL_SIGN);
            enemyFields.get(attacker.getUserName()).editCage(coordinate, FieldEmoji.KILL_SIGN);
        }
        return (enemyField.getAllLives() > 0)
                ? MovingInformationForBothPlayers.KILL_INFO
                : MovingInformationForBothPlayers.WIN_INFO;
    }
    private MovingInformationForBothPlayers treatShipHurt(MyUser attacker, String coordinates,
                                                          TelegramField enemyField, Ship currentShip)
    {
        currentShip.decreaseLivesByOne();
        enemyField.editCage(coordinates, FieldEmoji.HURT_SIGN);
        enemyFields.get(attacker.getUserName()).editCage(coordinates, FieldEmoji.HURT_SIGN);
        return MovingInformationForBothPlayers.HURT_INFO;
    }
    public void resetOwnField(MyUser currentUser)
    {
        TelegramField newField = new TelegramField();
        newField.setTelegramOwnField(new BaseField());
        ownFields.put(currentUser.getUserName(), newField);
        ships.put(currentUser.getUserName(), getUserShips());
    }

    public Map<String, Boolean> getFirstMovement() {return firstMovement;}
}
