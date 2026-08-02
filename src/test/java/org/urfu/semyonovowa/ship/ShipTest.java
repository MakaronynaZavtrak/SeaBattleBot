package org.urfu.semyonovowa.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты иерархии кораблей: количество жизней и стартовые значения.
 * Значение-заглушка 404 у базового {@link Ship} зафиксировано намеренно —
 * на этапе 2 оно уйдёт вместе с магическими константами, и тест будет обновлён.
 */
class ShipTest
{
    @Test
    @DisplayName("у каждого типа корабля своё число жизней")
    void shipTypesHaveExpectedLives()
    {
        assertThat(new Boat().getLives()).isEqualTo(1);
        assertThat(new Esminez().getLives()).isEqualTo(2);
        assertThat(new Cruiser().getLives()).isEqualTo(3);
        assertThat(new LinCore().getLives()).isEqualTo(4);
    }

    @Test
    @DisplayName("новый корабль без координат и повреждений")
    void freshShipHasNoCoordinatesAndDamage()
    {
        Ship ship = new Boat();

        assertThat(ship.getCoordinatesSet()).isEmpty();
        assertThat(ship.getDamagedCages()).isEmpty();
    }

    @Test
    @DisplayName("ориентация и жизни базового корабля — заглушка 404")
    void baseShipUsesSentinelDefaults()
    {
        Ship ship = new Ship();

        assertThat(ship.getLives()).isEqualTo(404);
        assertThat(ship.getOrientation()).isEqualTo(404);
    }

    @Test
    @DisplayName("уменьшение жизней на единицу")
    void decreaseLivesByOne()
    {
        Ship esminez = new Esminez();

        esminez.decreaseLivesByOne();

        assertThat(esminez.getLives()).isEqualTo(1);
    }
}
