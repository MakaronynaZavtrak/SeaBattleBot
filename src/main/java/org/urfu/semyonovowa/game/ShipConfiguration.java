package org.urfu.semyonovowa.game;

/**
 * Найденная раскладка корабля вокруг кликнутой клетки — результат
 * {@link Game#findWaysToConfigureTheShip}. Неизменяемое значение: собирается один
 * раз при обходе и только читается в {@link Game#configureTheShip}.
 *
 * @param startRow     строка стартовой (кликнутой) клетки
 * @param startCol     столбец стартовой (кликнутой) клетки
 * @param variableAxis индекс переменной оси раскладки (0 — строка, 1 — столбец)
 * @param step         направление раскладки вдоль оси (+1 или -1)
 * @param amountWays   сколько всего способов уложить корабль через эту клетку:
 *                     0 — никак (отказ), 1 — единственный (раскладываем целиком),
 *                     &gt;1 — неоднозначно (ставим одну клетку и ждём уточнения)
 */
public record ShipConfiguration(int startRow, int startCol, int variableAxis, int step, int amountWays) {}
