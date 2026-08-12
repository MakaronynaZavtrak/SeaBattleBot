package org.urfu.semyonovowa.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.urfu.semyonovowa.field.TelegramField;
import org.urfu.semyonovowa.ship.Boat;
import org.urfu.semyonovowa.ship.Ship;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.State;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Характеризационные тесты для {@link Game}: фиксируют ТЕКУЩЕЕ поведение логики
 * расстановки и стрельбы перед рефакторингом (этапы 3 и 6).
 *
 * Экзотические ветки расстановки (авто-конфигурация крейсера по amountWays,
 * краевое до-заполнение линкора) намеренно не покрыты точными ассертами — их
 * стоит зафиксировать «золотыми» значениями после первого локального прогона.
 */
class GameCharacterizationTest
{
    private MyUser creator()
    {
        return new MyUser(1L, "creator", "Creator", State.IN_LOBBY);
    }

    private MyUser invited()
    {
        return new MyUser(2L, "invited", "Invited", State.IN_LOBBY);
    }

    /** Ставит один и тот же корабль на все переданные клетки поля, минуя setCage. */
    private void place(TelegramField field, Ship ship, String... coordinates)
    {
        for (String coordinate : coordinates)
        {
            field.getShipsMap().put(coordinate, ship);
            ship.getCoordinatesSet().add(coordinate);
        }
    }

    @Nested
    @DisplayName("Расстановка кораблей (setCage)")
    class Placement
    {
        @Test
        @DisplayName("первая клетка линкора на пустом поле принимается")
        void firstLinCoreCellIsAccepted()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.LINCORE_SETTING);
            Ship linCore = game.getShips().get(user.getChatId()).get(0);

            boolean placed = game.setCage("0 0", user, linCore);

            assertThat(placed).isTrue();
            assertThat(linCore.getCoordinatesSet()).contains("0 0");
            assertThat(game.getOwnFields().get(user.getChatId()).getShipsMap()).containsKey("0 0");
        }

        @Test
        @DisplayName("занятая клетка отклоняется")
        void occupiedCellIsRejected()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.LINCORE_SETTING);
            Ship linCore = game.getShips().get(user.getChatId()).get(0);
            game.setCage("0 0", user, linCore);

            user.setState(State.CRUISER_SETTING);
            Ship cruiser = game.getShips().get(user.getChatId()).get(1);
            boolean placed = game.setCage("0 0", user, cruiser);

            assertThat(placed).isFalse();
        }

        @Test
        @DisplayName("катер вплотную к другому кораблю отклоняется")
        void boatAdjacentToAnotherShipIsRejected()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            game.getOwnFields().get(user.getChatId()).getShipsMap().put("3 3", new Boat());

            user.setState(State.BOAT_1_SETTING);
            Ship boat = game.getShips().get(user.getChatId()).get(4);
            boolean placed = game.setCage("3 4", user, boat);

            assertThat(placed).isFalse();
        }

        @Test
        @DisplayName("катер на удалённой пустой клетке принимается")
        void boatOnFarEmptyCellIsAccepted()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.BOAT_1_SETTING);
            Ship boat = game.getShips().get(user.getChatId()).get(4);

            boolean placed = game.setCage("5 5", user, boat);

            assertThat(placed).isTrue();
            assertThat(game.getOwnFields().get(user.getChatId()).getShipsMap()).containsKey("5 5");
        }
    }

    @Nested
    @DisplayName("Стрельба (attack)")
    class Attack
    {
        /** Раскладывает все 7 кораблей защищающегося (14 клеток) непересекающимся образом. */
        private void placeFullFleet(Game game, MyUser defender)
        {
            TelegramField field = game.getOwnFields().get(defender.getChatId());
            place(field, game.getShips().get(defender.getChatId()).get(0), "0 0", "0 1", "0 2", "0 3");
            place(field, game.getShips().get(defender.getChatId()).get(1), "2 0", "2 1", "2 2");
            place(field, game.getShips().get(defender.getChatId()).get(2), "4 0", "4 1");
            place(field, game.getShips().get(defender.getChatId()).get(3), "6 0", "6 1");
            place(field, game.getShips().get(defender.getChatId()).get(4), "0 5");
            place(field, game.getShips().get(defender.getChatId()).get(5), "2 5");
            place(field, game.getShips().get(defender.getChatId()).get(6), "4 5");
        }

        @Test
        @DisplayName("выстрел по пустой клетке — промах")
        void shotIntoEmptyCellIsMiss()
        {
            MyUser attacker = creator();
            MyUser defender = invited();
            Game game = new Game(attacker, defender);
            placeFullFleet(game, defender);

            MovingInformationForBothPlayers result = game.attack(attacker, "7 7");

            assertThat(result).isEqualTo(MovingInformationForBothPlayers.MISS_INFO);
        }

        @Test
        @DisplayName("первое попадание по эсминцу — ранение")
        void firstHitOnTwoCellShipIsHurt()
        {
            MyUser attacker = creator();
            MyUser defender = invited();
            Game game = new Game(attacker, defender);
            placeFullFleet(game, defender);

            MovingInformationForBothPlayers result = game.attack(attacker, "4 0");

            assertThat(result).isEqualTo(MovingInformationForBothPlayers.HURT_INFO);
        }

        @Test
        @DisplayName("второе попадание по эсминцу — потопление (но не победа)")
        void secondHitOnTwoCellShipIsKill()
        {
            MyUser attacker = creator();
            MyUser defender = invited();
            Game game = new Game(attacker, defender);
            placeFullFleet(game, defender);

            game.attack(attacker, "4 0");
            MovingInformationForBothPlayers result = game.attack(attacker, "4 1");

            assertThat(result).isEqualTo(MovingInformationForBothPlayers.KILL_INFO);
        }

        @Test
        @DisplayName("потопление последнего корабля — победа")
        void sinkingLastShipIsWin()
        {
            MyUser attacker = creator();
            MyUser defender = invited();
            Game game = new Game(attacker, defender);
            placeFullFleet(game, defender);

            String[] allCells = {
                    "0 0", "0 1", "0 2", "0 3",
                    "2 0", "2 1", "2 2",
                    "4 0", "4 1",
                    "6 0", "6 1",
                    "0 5", "2 5", "4 5"
            };

            MovingInformationForBothPlayers last = null;
            for (String cell : allCells)
                last = game.attack(attacker, cell);

            assertThat(last).isEqualTo(MovingInformationForBothPlayers.WIN_INFO);
        }
    }
}
