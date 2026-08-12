package org.urfu.semyonovowa.game;

import org.urfu.semyonovowa.field.FieldEmoji;
import org.urfu.semyonovowa.field.BaseField;
import org.urfu.semyonovowa.field.TelegramField;
import org.urfu.semyonovowa.ship.*;
import org.urfu.semyonovowa.user.MyUser;

import java.util.*;

import static java.lang.Math.*;

/**
 * основной класс для обработки логики единичной игры
 */
public class Game
{
    private final MyUser creator;
    private final MyUser invitedUser;
    private final Map<Long, TelegramField> ownFields;
    private final Map<Long, TelegramField> enemyFields;
    private final Map<Long, List<Ship>> ships;
    private final Map<Long, Boolean> firstMovement;
    public MyUser getCreator(){return this.creator;}
    public Map<Long, TelegramField> getOwnFields(){return this.ownFields;}
    public Map<Long, TelegramField> getEnemyFields(){return this.enemyFields;}
    public Map<Long, List<Ship>> getShips(){return this.ships;}
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
        ownFields.put(user.getChatId(), userOwnField);

        TelegramField userEnemyField = new TelegramField();
        userEnemyField.setTelegramEnemyField(baseField);
        enemyFields.put(user.getChatId(), userEnemyField);

        ships.put(user.getChatId(), getUserShips());
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
        TelegramField field = ownFields.get(user.getChatId());
        if (field.getShipsMap().containsKey(coordinates))
            return false;
        Coord coord = Coord.parse(coordinates);
        switch (user.getState())
        {
            case LINCORE_SETTING -> {return setLinCoreCage(coord, field, ship);}
            case CRUISER_SETTING -> {return setCruiserCage(coord, field, ship);}
            case ESMINEZ_1_SETTING, ESMINEZ_2_SETTING -> {return setEsminezCage(coord, field, ship);}
            default -> {return setBoatCage(coord, field, ship);}
        }
    }

    private boolean setBoatCage(Coord coord, TelegramField field, Ship ship)
    {
        if (!isInCorrectPosition(coord, ship, field.getShipsMap()))
            return false;

        treatSingleCage(coord, field, ship);
        return true;
    }

    private boolean setEsminezCage(Coord coord, TelegramField field, Ship ship)
    {
        if (!isInCorrectPosition(coord, ship, field.getShipsMap()))
            return false;
        if (ship.getCoordinatesSet().isEmpty())
        {
            ShipConfiguration configuration = findWaysToConfigureTheShip(coord, ship, field.getShipsMap());
            switch (configuration.getAmountWays())
            {
                case 1 -> {configureTheShip(configuration, field, ship); return true;}
                case 0 -> {return false;}
                default -> {treatSingleCage(coord, field, ship); return true;}
            }
        }
        else
        {
            Orientation orientation = (ship.getCoordinatesSet().size() == 1)
                    ? defineShipOrientation(coord, ship).orElse(null)
                    : ship.getOrientation();
            if (orientation == null)
                return false;
            int varUnitIdx = orientation.axisIndex();

            int variableUnit = coord.axis(varUnitIdx);
            int fixedUnit = coord.axis(1 - varUnitIdx);

            if (fixedUnit != ship.getFixedVal())
                return false;

            Coord firstCage = Coord.parse(ship.getCoordinatesSet().stream().findFirst().get());
            int firstVariable = firstCage.axis(varUnitIdx);
            if (abs(variableUnit - firstVariable) >= ship.getLives())
                return false;
            treatSingleCage(coord, field, ship);
            return true;
        }
    }

    private boolean setCruiserCage(Coord coord, TelegramField field, Ship ship)
    {
        if (!isInCorrectPosition(coord, ship, field.getShipsMap()))
            return false;

        if (ship.getCoordinatesSet().isEmpty())
        {
            ShipConfiguration configuration = findWaysToConfigureTheShip(coord, ship, field.getShipsMap());
            switch (configuration.getAmountWays())
            {
                case 1 -> {configureTheShip(configuration, field, ship); return true;}
                case 0 -> {return false;}
                default -> {treatSingleCage(coord, field, ship); return true;}
            }
        }
        else
        {
            Orientation orientation = (ship.getCoordinatesSet().size() == 1)
                    ? defineShipOrientation(coord, ship).orElse(null)
                    : ship.getOrientation();
            if (orientation == null)
                return false;
            int varUnitIdx = orientation.axisIndex();

            int variableUnit = coord.axis(varUnitIdx);
            int fixedUnit = coord.axis(1 - varUnitIdx);

            if (fixedUnit != ship.getFixedVal())
                return false;

            int minVariable = variableUnit;
            int maxVariable = variableUnit;

            for (String cage: ship.getCoordinatesSet())
            {
                int currentVariable = Coord.parse(cage).axis(varUnitIdx);
                if (abs(variableUnit - currentVariable) >= ship.getLives())
                    return false;
                minVariable = min(minVariable, currentVariable);
                maxVariable = max(maxVariable, currentVariable);
            }

            if (maxVariable - minVariable == ship.getLives() - 1)
            {
                for (int i = minVariable; i < ship.getLives() + minVariable; i++)
                    treatSingleCage(Coord.of(i, fixedUnit, varUnitIdx), field, ship);
                return true;
            }

            Coord beforeMin = Coord.of(minVariable - 1, fixedUnit, varUnitIdx);
            Coord afterMax = Coord.of(maxVariable + 1, fixedUnit, varUnitIdx);

            if ((minVariable == 0 || !isInCorrectPosition(beforeMin, ship, field.getShipsMap()))
                    && isInCorrectPosition(afterMax, ship, field.getShipsMap())
                    && afterMax.isOnBoard())
            {
                fillShipCages(minVariable, fixedUnit, varUnitIdx, 1, ship, field);
                return true;
            }
            else if ((maxVariable == Coord.BOARD_SIZE - 1 || !isInCorrectPosition(afterMax, ship, field.getShipsMap()))
                    && isInCorrectPosition(beforeMin, ship, field.getShipsMap())
                    && beforeMin.isOnBoard())
            {
                fillShipCages(maxVariable, fixedUnit, varUnitIdx, -1, ship, field);
                return true;
            }

            for (int i = minVariable; i < ship.getLives() + minVariable; i++)
            {
                if (!isInCorrectPosition(Coord.of(i, fixedUnit, varUnitIdx), ship, field.getShipsMap()))
                    return false;
            }
            treatSingleCage(coord, field, ship);
            return true;
        }
    }

    private boolean setLinCoreCage(Coord coord, TelegramField field, Ship ship)
    {
        if (!ship.getCoordinatesSet().isEmpty())
        {
            Orientation orientation = (ship.getCoordinatesSet().size() == 1)
                    ? defineShipOrientation(coord, ship).orElse(null)
                    : ship.getOrientation();
            if (orientation == null)
                return false;
            int varUnitIdx = orientation.axisIndex();

            int variableUnit = coord.axis(varUnitIdx);
            int fixedUnit = coord.axis(1 - varUnitIdx);

            if (fixedUnit != ship.getFixedVal())
                return false;

            int minVariable = variableUnit;
            int maxVariable = variableUnit;

            for (String cage : ship.getCoordinatesSet())
            {
                int currentVariable = Coord.parse(cage).axis(varUnitIdx);
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
            else if (maxVariable == Coord.BOARD_SIZE - 1)
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
        treatSingleCage(coord, field, ship);
        return true;
    }

    private Optional<Orientation> defineShipOrientation(Coord coord, Ship ship)
    {
        Coord firstCoord = Coord.parse(ship.getCoordinatesSet().stream().findFirst().get());

        if (coord.col() == firstCoord.col())
        {
            ship.setOrientation(Orientation.VERTICAL);
            ship.setFixedVal(coord.col());
            return Optional.of(Orientation.VERTICAL);
        }
        if (coord.row() == firstCoord.row())
        {
            ship.setOrientation(Orientation.HORIZONTAL);
            ship.setFixedVal(coord.row());
            return Optional.of(Orientation.HORIZONTAL);
        }
        return Optional.empty();
    }

    private void configureTheShip(ShipConfiguration configuration, TelegramField field, Ship ship)
    {
        Coord start = new Coord(configuration.getY(), configuration.getX());
        int variableUnit = start.axis(configuration.getVarUnitIdx());
        int fixedUnit = start.axis(1 - configuration.getVarUnitIdx());
        for (int i = 0; abs(i) < ship.getLives(); i += configuration.getStep())
            treatSingleCage(Coord.of(variableUnit + i, fixedUnit, configuration.getVarUnitIdx()), field, ship);
    }

    private void treatSingleCage(Coord coord, TelegramField field, Ship ship)
    {
        String key = coord.toString();
        field.editCage(key, FieldEmoji.SHIP_SIGN);
        ship.getCoordinatesSet().add(key);
        field.getShipsMap().put(key, ship);
    }

    private void fillShipCages(int minVariableUnit, int fixed, int varUnitIdx, int step, Ship ship, TelegramField field)
    {
        for (int i = 0; abs(i) < ship.getLives(); i += step)
            treatSingleCage(Coord.of(minVariableUnit + i, fixed, varUnitIdx), field, ship);
    }

    private ShipConfiguration findWaysToConfigureTheShip(Coord coord, Ship ship, Map<String, Ship> shipsMap)
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
                positiveDif = isValidWay(coord, varUnitIdx, step, ship, shipsMap);
                if (positiveDif == ship.getLives() - 1)
                    if (!configuration.canBeConfigured())
                        configuration.initializeConfiguration(coord.row(), coord.col(), varUnitIdx, step);
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
    private int isValidWay(Coord coord, int varUnitIdx, int step, Ship ship, Map<String, Ship> shipsMap)
    {
        int variableUnit = coord.axis(varUnitIdx);
        int fixedUnit = coord.axis(1 - varUnitIdx);

        int freeCages = 0;

        for (int i = step; abs(i) < ship.getLives()
                && variableUnit + i < Coord.BOARD_SIZE && variableUnit + i >= 0; i += step)
        {
            if (!isInCorrectPosition(Coord.of(variableUnit + i, fixedUnit, varUnitIdx), ship, shipsMap))
                return freeCages;
            freeCages++;
        }
        return freeCages;
    }
    /**
     * Проверяет, находится ли хотя бы одна ячейка другого корабля в расстоянии одной клетки от координаты coord
     * @param currentShip корабль, чью ячейку жизни проверяют
     * @return true
     */
    public boolean isInCorrectPosition(Coord coord, Ship currentShip, Map<String, Ship> shipsMap)
    {
        return !BitBoard.blockAround(coord).intersects(occupancyOfOtherShips(currentShip, shipsMap));
    }

    /**
     * Собирает битовую доску клеток, занятых любыми кораблями, кроме currentShip.
     */
    private BitBoard occupancyOfOtherShips(Ship currentShip, Map<String, Ship> shipsMap)
    {
        BitBoard board = BitBoard.empty();
        for (Map.Entry<String, Ship> entry : shipsMap.entrySet())
            if (!entry.getValue().equals(currentShip))
                board = board.set(Coord.parse(entry.getKey()));
        return board;
    }
    /**
     * Содержит в себе логическую обработку хода каждого игрока, который отображает на поле
     * @param attacker пользователь, который ходит
     * @param coordinates координаты, на которые ткнул ходящий пользователь
     * @return сообщение о результате выстрела
     */
    public MovingInformationForBothPlayers attack(MyUser attacker, String coordinates)
    {
        MyUser defender = (attacker.getChatId() == creator.getChatId()) ?  invitedUser : creator;
        TelegramField enemyField = ownFields.get(defender.getChatId());
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
        enemyFields.get(attacker.getChatId()).editCage(coordinates, FieldEmoji.MISS_SIGN);
        return MovingInformationForBothPlayers.MISS_INFO;
    }
    private MovingInformationForBothPlayers treatShipKilling(MyUser attacker, TelegramField enemyField,
                                                             Ship currentShip)
    {
        for (String coordinate : currentShip.getDamagedCages())
        {
            enemyField.editCage(coordinate, FieldEmoji.KILL_SIGN);
            enemyFields.get(attacker.getChatId()).editCage(coordinate, FieldEmoji.KILL_SIGN);
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
        enemyFields.get(attacker.getChatId()).editCage(coordinates, FieldEmoji.HURT_SIGN);
        return MovingInformationForBothPlayers.HURT_INFO;
    }
    public void resetOwnField(MyUser currentUser)
    {
        TelegramField newField = new TelegramField();
        newField.setTelegramOwnField(new BaseField());
        ownFields.put(currentUser.getChatId(), newField);
        ships.put(currentUser.getChatId(), getUserShips());
    }

    public Map<Long, Boolean> getFirstMovement() {return firstMovement;}
}
