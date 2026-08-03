package org.urfu.semyonovowa.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты значения {@link Coord}. Проверяют в том числе семантические
 * эквивалентности, на которые опирается рефакторинг геометрии Game:
 * {@code Coord.of} воспроизводит прежнюю склейку "y x".
 */
class CoordTest
{
    @Test
    @DisplayName("разбор строки в координату")
    void parseSplitsRowAndColumn()
    {
        Coord coord = Coord.parse("3 4");
        assertThat(coord.row()).isEqualTo(3);
        assertThat(coord.col()).isEqualTo(4);
    }

    @Test
    @DisplayName("строковое представление совпадает с форматом y x")
    void toStringMatchesWireFormat()
    {
        assertThat(new Coord(3, 4)).hasToString("3 4");
        assertThat(new Coord(0, 0)).hasToString("0 0");
    }

    @Test
    @DisplayName("разбор и печать обратимы")
    void parseAndToStringRoundTrip()
    {
        for (String s : new String[]{"0 0", "7 7", "2 5", "5 2"})
            assertThat(Coord.parse(s)).hasToString(s);
    }

    @Test
    @DisplayName("of воспроизводит прежнюю склейку по оси")
    void ofReproducesLegacyConcatenation()
    {
        // varUnitIdx == 0: переменная — строка → "variable fixed"
        assertThat(Coord.of(5, 2, 0)).hasToString("5 2");
        // varUnitIdx == 1: переменная — столбец → "fixed variable"
        assertThat(Coord.of(5, 2, 1)).hasToString("2 5");
    }

    @Test
    @DisplayName("axis возвращает нужную ось")
    void axisReturnsRequestedComponent()
    {
        Coord coord = new Coord(3, 4);
        assertThat(coord.axis(0)).isEqualTo(3);
        assertThat(coord.axis(1)).isEqualTo(4);
    }

    @Test
    @DisplayName("isOnBoard проверяет обе границы 0..7")
    void isOnBoardChecksBothBounds()
    {
        assertThat(new Coord(0, 0).isOnBoard()).isTrue();
        assertThat(new Coord(7, 7).isOnBoard()).isTrue();
        assertThat(new Coord(8, 0).isOnBoard()).isFalse();
        assertThat(new Coord(0, 8).isOnBoard()).isFalse();
        assertThat(new Coord(-1, 0).isOnBoard()).isFalse();
        assertThat(new Coord(0, -1).isOnBoard()).isFalse();
    }
}
