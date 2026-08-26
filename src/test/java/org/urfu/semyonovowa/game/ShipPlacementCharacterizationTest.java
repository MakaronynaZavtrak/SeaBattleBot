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
 * Характеризационная сеть под этап 18 (рефакторинг алгоритмов расстановки).
 *
 * Фиксирует ТЕКУЩЕЕ поведение {@link Game#setCage} на всех нетривиальных ветках
 * полуавтоматической расстановки, которые прежний GameCharacterizationTest
 * намеренно оставил без точных ассертов:
 *  - авто-конфигурация корабля по одному клику ({@code findWaysToConfigureTheShip}
 *    -> amountWays == 1 / >= 2);
 *  - достройка в несколько кликов со сканом min/max по переменной оси;
 *  - выбор направления краевой заливки (ветки beforeMin/afterMax у крейсера,
 *    автозавершение линкора у края и по достижении длины);
 *  - отказы: диагональ, второй клик дальше длины корабля.
 *
 * Значения выверены вручную по коду алгоритма и служат «золотыми»: любой дрейф
 * поведения при рефакторинге 18b/18c обязан здесь покраснеть.
 *
 * Геометрическое напоминание: {@code isInCorrectPosition} запрещает клетке
 * соседствовать (ход короля) с ЧУЖИМ кораблём, но не мешает кораблю тянуться
 * вдоль себя. Поэтому на пустом поле форму задаёт только логика самого корабля.
 */
class ShipPlacementCharacterizationTest
{
    private MyUser creator()
    {
        return new MyUser(1L, "creator", "Creator", State.IN_LOBBY);
    }

    private MyUser invited()
    {
        return new MyUser(2L, "invited", "Invited", State.IN_LOBBY);
    }

    private Ship ship(Game game, MyUser user, int index)
    {
        return game.getShips().get(user.getChatId()).get(index);
    }

    private TelegramField field(Game game, MyUser user)
    {
        return game.getOwnFields().get(user.getChatId());
    }

    /** Регистрирует ЧУЖОЙ корабль-блокиратор на клетках (в shipsMap и его набор). */
    private void blocker(TelegramField field, String... cells)
    {
        Ship other = new Boat();
        for (String cell : cells)
        {
            field.getShipsMap().put(cell, other);
            other.getCoordinatesSet().add(cell);
        }
    }

    // ==================== Линкор (4 клетки) ====================

    @Nested
    @DisplayName("Линкор (LINCORE_SETTING)")
    class LinCorePlacement
    {
        @Test
        @DisplayName("два клика у верхнего края (min==0) авто-достраивают линкор вниз")
        void twoClicksAtTopEdgeAutoCompleteDownward()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.LINCORE_SETTING);
            Ship linCore = ship(game, user, 0);

            assertThat(game.setCage("0 3", user, linCore)).isTrue();
            assertThat(game.setCage("1 3", user, linCore)).isTrue();

            assertThat(linCore.getCoordinatesSet())
                    .containsExactlyInAnyOrder("0 3", "1 3", "2 3", "3 3");
        }

        @Test
        @DisplayName("четыре клика в середине достраивают линкор по достижении длины")
        void fourClicksInMiddleCompleteBySpan()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.LINCORE_SETTING);
            Ship linCore = ship(game, user, 0);

            assertThat(game.setCage("2 3", user, linCore)).isTrue(); // single
            assertThat(game.setCage("3 3", user, linCore)).isTrue(); // single (span 1)
            assertThat(game.setCage("4 3", user, linCore)).isTrue(); // single (span 2)
            assertThat(game.setCage("5 3", user, linCore)).isTrue(); // span==3 -> заливка

            assertThat(linCore.getCoordinatesSet())
                    .containsExactlyInAnyOrder("2 3", "3 3", "4 3", "5 3");
        }

        @Test
        @DisplayName("диагональный второй клик отклоняется")
        void diagonalSecondClickRejected()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.LINCORE_SETTING);
            Ship linCore = ship(game, user, 0);

            assertThat(game.setCage("2 2", user, linCore)).isTrue();
            assertThat(game.setCage("3 3", user, linCore)).isFalse();

            assertThat(linCore.getCoordinatesSet()).containsExactly("2 2");
        }

        @Test
        @DisplayName("второй клик дальше длины корабля отклоняется")
        void secondClickBeyondLengthRejected()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.LINCORE_SETTING);
            Ship linCore = ship(game, user, 0);

            assertThat(game.setCage("0 0", user, linCore)).isTrue();
            assertThat(game.setCage("4 0", user, linCore)).isFalse(); // |4-0| == lives

            assertThat(linCore.getCoordinatesSet()).containsExactly("0 0");
        }
    }

    // ==================== Крейсер (3 клетки) ====================

    @Nested
    @DisplayName("Крейсер (CRUISER_SETTING)")
    class CruiserPlacement
    {
        @Test
        @DisplayName("первый клик в открытой середине (много вариантов) ставит одну клетку")
        void firstClickInOpenMiddlePlacesSingle()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.CRUISER_SETTING);
            Ship cruiser = ship(game, user, 1);

            assertThat(game.setCage("3 3", user, cruiser)).isTrue();

            assertThat(cruiser.getCoordinatesSet()).containsExactly("3 3");
        }

        @Test
        @DisplayName("единственный вариант (amountWays==1) авто-конфигурирует весь крейсер по клику")
        void singleWayAutoConfiguresWholeShip()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.CRUISER_SETTING);
            Ship cruiser = ship(game, user, 1);

            // Блокиратор "2 1" глушит вертикаль от "0 0" (клетка "1 0" становится
            // соседней с чужим кораблём), оставляя единственный путь — вправо по строке 0.
            blocker(field(game, user), "2 1");

            assertThat(game.setCage("0 0", user, cruiser)).isTrue();

            assertThat(cruiser.getCoordinatesSet())
                    .containsExactlyInAnyOrder("0 0", "0 1", "0 2");
        }

        @Test
        @DisplayName("три клика в середине достраивают крейсер по достижении длины")
        void threeClicksCompleteBySpan()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.CRUISER_SETTING);
            Ship cruiser = ship(game, user, 1);

            assertThat(game.setCage("3 3", user, cruiser)).isTrue(); // single
            assertThat(game.setCage("3 4", user, cruiser)).isTrue(); // single (span 1)
            assertThat(game.setCage("3 5", user, cruiser)).isTrue(); // span==2 -> заливка

            assertThat(cruiser.getCoordinatesSet())
                    .containsExactlyInAnyOrder("3 3", "3 4", "3 5");
        }

        @Test
        @DisplayName("у правого края второй клик достраивает крейсер влево")
        void rightEdgeFillsLeft()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.CRUISER_SETTING);
            Ship cruiser = ship(game, user, 1);

            assertThat(game.setCage("3 6", user, cruiser)).isTrue();
            assertThat(game.setCage("3 7", user, cruiser)).isTrue(); // max==7 -> fill влево

            assertThat(cruiser.getCoordinatesSet())
                    .containsExactlyInAnyOrder("3 5", "3 6", "3 7");
        }

        @Test
        @DisplayName("у левого края второй клик достраивает крейсер вправо")
        void leftEdgeFillsRight()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.CRUISER_SETTING);
            Ship cruiser = ship(game, user, 1);

            assertThat(game.setCage("3 1", user, cruiser)).isTrue();
            assertThat(game.setCage("3 0", user, cruiser)).isTrue(); // min==0 -> fill вправо

            assertThat(cruiser.getCoordinatesSet())
                    .containsExactlyInAnyOrder("3 0", "3 1", "3 2");
        }
    }

    // ==================== Эсминец (2 клетки) ====================

    @Nested
    @DisplayName("Эсминец (ESMINEZ_1_SETTING)")
    class EsminezPlacement
    {
        @Test
        @DisplayName("первый клик в открытой середине (много вариантов) ставит одну клетку")
        void firstClickInOpenMiddlePlacesSingle()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.ESMINEZ_1_SETTING);
            Ship esminez = ship(game, user, 2);

            assertThat(game.setCage("3 3", user, esminez)).isTrue();

            assertThat(esminez.getCoordinatesSet()).containsExactly("3 3");
        }

        @Test
        @DisplayName("единственный вариант (amountWays==1) авто-конфигурирует эсминец по клику")
        void singleWayAutoConfigures()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.ESMINEZ_1_SETTING);
            Ship esminez = ship(game, user, 2);

            // Блокиратор "1 2" глушит ход вправо от "0 0" (клетка "0 1" соседствует
            // с чужим), оставляя единственный путь — вниз по столбцу 0.
            blocker(field(game, user), "1 2");

            assertThat(game.setCage("0 0", user, esminez)).isTrue();

            assertThat(esminez.getCoordinatesSet())
                    .containsExactlyInAnyOrder("0 0", "1 0");
        }

        @Test
        @DisplayName("два клика в ряд достраивают эсминец")
        void twoClicksComplete()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.ESMINEZ_1_SETTING);
            Ship esminez = ship(game, user, 2);

            assertThat(game.setCage("3 3", user, esminez)).isTrue();
            assertThat(game.setCage("3 4", user, esminez)).isTrue();

            assertThat(esminez.getCoordinatesSet())
                    .containsExactlyInAnyOrder("3 3", "3 4");
        }

        @Test
        @DisplayName("диагональный второй клик отклоняется")
        void diagonalSecondClickRejected()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.ESMINEZ_1_SETTING);
            Ship esminez = ship(game, user, 2);

            assertThat(game.setCage("3 3", user, esminez)).isTrue();
            assertThat(game.setCage("4 4", user, esminez)).isFalse();

            assertThat(esminez.getCoordinatesSet()).containsExactly("3 3");
        }

        @Test
        @DisplayName("второй клик дальше длины корабля отклоняется")
        void secondClickBeyondLengthRejected()
        {
            MyUser user = creator();
            Game game = new Game(user, invited());
            user.setState(State.ESMINEZ_1_SETTING);
            Ship esminez = ship(game, user, 2);

            assertThat(game.setCage("3 3", user, esminez)).isTrue();
            assertThat(game.setCage("3 5", user, esminez)).isFalse(); // |5-3| == lives

            assertThat(esminez.getCoordinatesSet()).containsExactly("3 3");
        }
    }
}
