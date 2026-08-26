package org.urfu.semyonovowa.game;

import org.urfu.semyonovowa.field.FieldEmoji;
import org.urfu.semyonovowa.field.BaseField;
import org.urfu.semyonovowa.field.TelegramField;
import org.urfu.semyonovowa.ship.*;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.State;
import org.urfu.semyonovowa.dataBase.GameSnapshot;

import lombok.Getter;

import java.util.*;

import static java.lang.Math.*;

/**
 * основной класс для обработки логики единичной игры
 */
public class Game
{
    @Getter private final MyUser creator;
    private final MyUser invitedUser;
    @Getter private final Map<Long, TelegramField> ownFields;
    @Getter private final Map<Long, TelegramField> enemyFields;
    @Getter private final Map<Long, List<Ship>> ships;
    @Getter private final Map<Long, Boolean> firstMovement;
    private final Map<Long, BitBoard> hits;
    public Game(MyUser creator, MyUser invitedUser)
    {
        this.creator = creator;
        this.invitedUser = invitedUser;
        this.ownFields = new HashMap<>();
        this.enemyFields = new HashMap<>();
        this.ships = new HashMap<>();
        this.firstMovement = new HashMap<>();
        this.hits = new HashMap<>();

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
        hits.put(user.getChatId(), BitBoard.empty());
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

    /**
     * Первый клик по кораблю длиннее одной клетки: спрашивает у
     * {@link #findWaysToConfigureTheShip} число способов уложить корабль через эту
     * клетку. Единственный способ — раскладываем корабль целиком; ни одного — отказ;
     * несколько — ставим одну клетку и ждём уточнения направления следующим кликом.
     */
    private boolean placeFirstCell(Coord coord, TelegramField field, Ship ship)
    {
        ShipConfiguration configuration = findWaysToConfigureTheShip(coord, ship, field.getShipsMap());
        return switch (configuration.amountWays())
        {
            case 1 -> { configureTheShip(configuration, field, ship); yield true; }
            case 0 -> false;
            default -> { treatSingleCage(coord, field, ship); yield true; }
        };
    }

    /**
     * Ось клика относительно ориентации корабля.
     * @param variableAxis индекс переменной оси (0 — строка, 1 — столбец)
     * @param variableUnit координата клика вдоль переменной оси
     * @param fixedUnit    координата клика вдоль фиксированной оси
     */
    private record PlacementAxis(int variableAxis, int variableUnit, int fixedUnit) {}

    /**
     * Определяет ось клика для многоклеточного корабля: по первому доклику задаёт
     * ориентацию (и фиксированную координату) корабля, затем сверяет, что клик лежит
     * на той же линии. Общая «преамбула» для линкора, крейсера и эсминца.
     * @return ось клика, либо {@code null}, если клик несовместим (диагональ или
     *         другая линия) — вызывающий трактует это как отказ.
     */
    private PlacementAxis resolveAxis(Coord coord, Ship ship)
    {
        Orientation orientation = (ship.getCoordinatesSet().size() == 1)
                ? defineShipOrientation(coord, ship).orElse(null)
                : ship.getOrientation();
        if (orientation == null)
            return null;

        int variableAxis = orientation.axisIndex();
        int fixedUnit = coord.axis(1 - variableAxis);
        if (fixedUnit != ship.getFixedVal())
            return null;

        return new PlacementAxis(variableAxis, coord.axis(variableAxis), fixedUnit);
    }

    /** Диапазон корабля вдоль переменной оси: минимум и максимум занятых координат. */
    private record Span(int min, int max) {}

    /**
     * Считает границы корабля вдоль переменной оси с учётом клика. Общий скан для
     * линкора и крейсера (для эсминца вырождается в проверку единственной клетки).
     * @return диапазон [min, max], либо {@code null}, если клик отстоит от уже
     *         занятой клетки не ближе длины корабля (отказ).
     */
    private Span variableSpan(Ship ship, int variableAxis, int variableUnit)
    {
        int lives = ship.getLives();
        int minVariable = variableUnit;
        int maxVariable = variableUnit;
        for (String cage : ship.getCoordinatesSet())
        {
            int current = Coord.parse(cage).axis(variableAxis);
            if (abs(variableUnit - current) >= lives)
                return null;
            minVariable = min(minVariable, current);
            maxVariable = max(maxVariable, current);
        }
        return new Span(minVariable, maxVariable);
    }

    private boolean setEsminezCage(Coord coord, TelegramField field, Ship ship)
    {
        if (!isInCorrectPosition(coord, ship, field.getShipsMap()))
            return false;
        if (ship.getCoordinatesSet().isEmpty())
            return placeFirstCell(coord, field, ship);

        PlacementAxis axis = resolveAxis(coord, ship);
        if (axis == null)
            return false;
        if (variableSpan(ship, axis.variableAxis(), axis.variableUnit()) == null)
            return false;

        treatSingleCage(coord, field, ship);
        return true;
    }

    private boolean setCruiserCage(Coord coord, TelegramField field, Ship ship)
    {
        Map<String, Ship> shipsMap = field.getShipsMap();
        if (!isInCorrectPosition(coord, ship, shipsMap))
            return false;
        if (ship.getCoordinatesSet().isEmpty())
            return placeFirstCell(coord, field, ship);

        PlacementAxis axis = resolveAxis(coord, ship);
        if (axis == null)
            return false;
        Span span = variableSpan(ship, axis.variableAxis(), axis.variableUnit());
        if (span == null)
            return false;

        int variableAxis = axis.variableAxis();
        int fixedUnit = axis.fixedUnit();
        int minVariable = span.min();
        int maxVariable = span.max();
        int lives = ship.getLives();

        if (maxVariable - minVariable == lives - 1)
        {
            fillShipCages(minVariable, fixedUnit, variableAxis, 1, ship, field);
            return true;
        }

        Coord beforeMin = Coord.of(minVariable - 1, fixedUnit, variableAxis);
        Coord afterMax = Coord.of(maxVariable + 1, fixedUnit, variableAxis);

        if ((minVariable == 0 || !isInCorrectPosition(beforeMin, ship, shipsMap))
                && isInCorrectPosition(afterMax, ship, shipsMap)
                && afterMax.isOnBoard())
        {
            fillShipCages(minVariable, fixedUnit, variableAxis, 1, ship, field);
            return true;
        }
        else if ((maxVariable == Coord.BOARD_SIZE - 1 || !isInCorrectPosition(afterMax, ship, shipsMap))
                && isInCorrectPosition(beforeMin, ship, shipsMap)
                && beforeMin.isOnBoard())
        {
            fillShipCages(maxVariable, fixedUnit, variableAxis, -1, ship, field);
            return true;
        }

        for (int i = minVariable; i < lives + minVariable; i++)
        {
            if (!isInCorrectPosition(Coord.of(i, fixedUnit, variableAxis), ship, shipsMap))
                return false;
        }
        treatSingleCage(coord, field, ship);
        return true;
    }

    private boolean setLinCoreCage(Coord coord, TelegramField field, Ship ship)
    {
        if (!ship.getCoordinatesSet().isEmpty())
        {
            PlacementAxis axis = resolveAxis(coord, ship);
            if (axis == null)
                return false;
            Span span = variableSpan(ship, axis.variableAxis(), axis.variableUnit());
            if (span == null)
                return false;

            int variableAxis = axis.variableAxis();
            int fixedUnit = axis.fixedUnit();
            int minVariable = span.min();
            int maxVariable = span.max();

            if (minVariable == 0)
            {
                fillShipCages(minVariable, fixedUnit, variableAxis, 1, ship, field);
                return true;
            }
            else if (maxVariable == Coord.BOARD_SIZE - 1)
            {
                fillShipCages(maxVariable, fixedUnit, variableAxis, -1, ship, field);
                return true;
            }

            if (maxVariable - minVariable == ship.getLives() - 1)
            {
                fillShipCages(minVariable, fixedUnit, variableAxis, 1, ship, field);
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

    public void configureTheShip(ShipConfiguration configuration, TelegramField field, Ship ship)
    {
        Coord start = new Coord(configuration.startRow(), configuration.startCol());
        int variableAxis = configuration.variableAxis();
        int variableUnit = start.axis(variableAxis);
        int fixedUnit = start.axis(1 - variableAxis);
        int lives = ship.getLives();
        for (int i = 0; abs(i) < lives; i += configuration.step())
            treatSingleCage(Coord.of(variableUnit + i, fixedUnit, variableAxis), field, ship);
    }

    private void treatSingleCage(Coord coord, TelegramField field, Ship ship)
    {
        String key = coord.toString();
        field.editCage(key, FieldEmoji.SHIP_SIGN);
        ship.getCoordinatesSet().add(key);
        field.getShipsMap().put(key, ship);
    }

    private void fillShipCages(int minVariableUnit, int fixed, int variableAxis, int step, Ship ship, TelegramField field)
    {
        int lives = ship.getLives();
        for (int i = 0; abs(i) < lives; i += step)
            treatSingleCage(Coord.of(minVariableUnit + i, fixed, variableAxis), field, ship);
    }

    public ShipConfiguration findWaysToConfigureTheShip(Coord coord, Ship ship, Map<String, Ship> shipsMap)
    {
        int lives = ship.getLives();
        int amountWays = 0;
        boolean configured = false;
        int startRow = 0, startCol = 0, configAxis = 0, configStep = 0;

        for (int variableAxis = 0; variableAxis < 2; variableAxis++)
        {
            int positiveFree = 0;
            int negativeFree = 0;
            for (int step = -1; step < 2; step += 2)
            {
                negativeFree = positiveFree;
                positiveFree = isValidWay(coord, variableAxis, step, ship, shipsMap);
                if (positiveFree == lives - 1 && !configured)
                {
                    startRow = coord.row();
                    startCol = coord.col();
                    configAxis = variableAxis;
                    configStep = step;
                    configured = true;
                }
            }
            if (positiveFree + negativeFree + 1 >= lives)
                amountWays += min(positiveFree, negativeFree) + 1;

            if (amountWays > 1)
                return new ShipConfiguration(startRow, startCol, configAxis, configStep, amountWays);
        }
        return new ShipConfiguration(startRow, startCol, configAxis, configStep, amountWays);
    }
    private int isValidWay(Coord coord, int variableAxis, int step, Ship ship, Map<String, Ship> shipsMap)
    {
        int lives = ship.getLives();
        int variableUnit = coord.axis(variableAxis);
        int fixedUnit = coord.axis(1 - variableAxis);

        int freeCages = 0;

        for (int i = step; abs(i) < lives
                && variableUnit + i < Coord.BOARD_SIZE && variableUnit + i >= 0; i += step)
        {
            if (!isInCorrectPosition(Coord.of(variableUnit + i, fixedUnit, variableAxis), ship, shipsMap))
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
     * Собирает битовую доску из набора координат-строк "y x".
     */
    private BitBoard bitBoardOf(Collection<String> coordinates)
    {
        BitBoard board = BitBoard.empty();
        for (String coordinate : coordinates)
            board = board.set(Coord.parse(coordinate));
        return board;
    }

    /**
     * Открывает на поле проигравшего непоражённые (выжившие) клетки кораблей победителя.
     * Перенесено из TelegramField, чтобы пакет field не зависел от game.
     */
    public void revealSurvivedShips(TelegramField loserEnemyField, Long winnerChatId)
    {
        TelegramField winnerOwnField = ownFields.get(winnerChatId);
        BitBoard winnerHits = hits.get(winnerChatId);
        for (String cell : winnerOwnField.getShipsMap().keySet())
            if (!winnerHits.test(Coord.parse(cell)))
                loserEnemyField.editCage(cell, FieldEmoji.SHIP_SIGN);
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
        Long defenderId = defender.getChatId();
        TelegramField enemyField = ownFields.get(defenderId);
        Ship currentShip = enemyField.getShipsMap().get(coordinates);

        if (currentShip != null)
        {
            hits.put(defenderId, hits.get(defenderId).set(Coord.parse(coordinates)));
            return hits.get(defenderId).contains(bitBoardOf(currentShip.getCoordinatesSet()))
                    ? treatShipKilling(attacker, defenderId, enemyField, currentShip)
                    : treatShipHurt(attacker, coordinates, enemyField);
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
    private MovingInformationForBothPlayers treatShipKilling(MyUser attacker, Long defenderId,
                                                             TelegramField enemyField, Ship currentShip)
    {
        for (String coordinate : currentShip.getCoordinatesSet())
        {
            enemyField.editCage(coordinate, FieldEmoji.KILL_SIGN);
            enemyFields.get(attacker.getChatId()).editCage(coordinate, FieldEmoji.KILL_SIGN);
        }
        return hits.get(defenderId).contains(bitBoardOf(enemyField.getShipsMap().keySet()))
                ? MovingInformationForBothPlayers.WIN_INFO
                : MovingInformationForBothPlayers.KILL_INFO;
    }
    private MovingInformationForBothPlayers treatShipHurt(MyUser attacker, String coordinates,
                                                          TelegramField enemyField)
    {
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

    // ==================== Персистентность ====================

    /**
     * Снимает слепок логического состояния партии для сохранения в БД.
     * Рендер полей не сохраняется — он детерминированно пересобирается в restore.
     */
    public GameSnapshot toSnapshot()
    {
        return new GameSnapshot(
                creator.getChatId(),
                invitedUser.getChatId(),
                List.of(playerState(creator), playerState(invitedUser)));
    }

    private GameSnapshot.PlayerState playerState(MyUser player)
    {
        Long id = player.getChatId();
        List<GameSnapshot.ShipState> shipStates = new ArrayList<>();
        for (Ship ship : ships.get(id))
        {
            List<String> cells = new ArrayList<>(ship.getCoordinatesSet());
            Collections.sort(cells); // детерминированный порядок вне зависимости от обхода HashSet
            String orientation = (ship.getOrientation() == null) ? null : ship.getOrientation().name();
            shipStates.add(new GameSnapshot.ShipState(cells, orientation, ship.getFixedVal()));
        }
        return new GameSnapshot.PlayerState(
                id,
                shipStates,
                hits.get(id).bits(),
                cellsToBits(enemyFields.get(id).getUsedCages()).bits(),
                player.getState().name(),
                firstMovement.getOrDefault(id, false));
    }

    /**
     * Восстанавливает партию из слепка. Игроки берутся из кэша/БД по актуальным
     * данным; их фаза (State) выставляется из слепка. Поля пересобираются полностью.
     */
    public static Game restore(GameSnapshot snapshot, MyUser creator, MyUser invitedUser)
    {
        Game game = new Game(creator, invitedUser);
        for (GameSnapshot.PlayerState playerState : snapshot.players())
        {
            MyUser user = (playerState.chatId() == creator.getChatId()) ? creator : invitedUser;
            game.restorePlayer(playerState, user);
        }
        game.renderRestoredBoards();
        return game;
    }

    private void restorePlayer(GameSnapshot.PlayerState playerState, MyUser user)
    {
        Long id = playerState.chatId();
        List<Ship> playerShips = ships.get(id);
        List<GameSnapshot.ShipState> shipStates = playerState.ships();
        TelegramField ownField = ownFields.get(id);
        for (int i = 0; i < playerShips.size(); i++)
        {
            Ship ship = playerShips.get(i);
            GameSnapshot.ShipState shipState = shipStates.get(i);
            for (String cell : shipState.cells())
            {
                ship.getCoordinatesSet().add(cell);
                ownField.getShipsMap().put(cell, ship);
            }
            ship.setOrientation(shipState.orientation() == null ? null : Orientation.valueOf(shipState.orientation()));
            ship.setFixedVal(shipState.fixedVal());
        }
        hits.put(id, new BitBoard(playerState.hits()));
        enemyFields.get(id).getUsedCages().addAll(bitsToCells(new BitBoard(playerState.usedCages())));
        // Наличие ключа = игрок уже ходил (живой код различает фазу по null,
        // а не по значению). Кладём только true, чтобы "ещё не ходил" остался null.
        if (playerState.firstMove())
            firstMovement.put(id, true);
        user.setState(State.valueOf(playerState.state()));
    }

    /**
     * Пересобирает видимое состояние всех четырёх полей из логического состояния:
     * свои корабли и входящие выстрелы соперника — на своём поле; свои выстрелы —
     * на поле соперника (промах/ранение/потопление).
     */
    private void renderRestoredBoards()
    {
        Long a = creator.getChatId();
        Long b = invitedUser.getChatId();
        renderPlayerView(a, b);
        renderPlayerView(b, a);
    }

    private void renderPlayerView(Long me, Long opponent)
    {
        TelegramField ownField = ownFields.get(me);
        for (Ship ship : ships.get(me))
            for (String cell : ship.getCoordinatesSet())
                ownField.editCage(cell, FieldEmoji.SHIP_SIGN);
        for (String cell : enemyFields.get(opponent).getUsedCages())
            ownField.editCage(cell, shotMark(me, cell));

        TelegramField myEnemyField = enemyFields.get(me);
        for (String cell : enemyFields.get(me).getUsedCages())
            myEnemyField.editCage(cell, shotMark(opponent, cell));
    }

    private String shotMark(Long defender, String cell)
    {
        Ship ship = ownFields.get(defender).getShipsMap().get(cell);
        if (ship == null)
            return FieldEmoji.MISS_SIGN;
        return hits.get(defender).contains(cellsToBits(ship.getCoordinatesSet()))
                ? FieldEmoji.KILL_SIGN
                : FieldEmoji.HURT_SIGN;
    }

    private BitBoard cellsToBits(Set<String> cells)
    {
        BitBoard board = BitBoard.empty();
        for (String cell : cells)
            board = board.set(Coord.parse(cell));
        return board;
    }

    private Set<String> bitsToCells(BitBoard board)
    {
        Set<String> cells = new HashSet<>();
        for (int row = 0; row < Coord.BOARD_SIZE; row++)
            for (int col = 0; col < Coord.BOARD_SIZE; col++)
            {
                Coord coord = new Coord(row, col);
                if (board.test(coord))
                    cells.add(coord.toString());
            }
        return cells;
    }
}
