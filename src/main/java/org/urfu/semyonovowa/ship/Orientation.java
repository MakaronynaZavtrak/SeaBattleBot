package org.urfu.semyonovowa.ship;

/**
 * Ориентация корабля на поле — ось, вдоль которой он вытянут.
 * Пришла на смену магическим {@code 0}/{@code 1} (и {@code 404} как «не задано»):
 * VERTICAL — корабль идёт вдоль строк (переменная ось — строка),
 * HORIZONTAL — вдоль столбцов (переменная ось — столбец).
 */
public enum Orientation
{
    VERTICAL(0),
    HORIZONTAL(1);

    private final int axisIndex;

    Orientation(int axisIndex)
    {
        this.axisIndex = axisIndex;
    }

    /**
     * @return индекс переменной оси: 0 — строка, 1 — столбец.
     */
    public int axisIndex()
    {
        return axisIndex;
    }
}
