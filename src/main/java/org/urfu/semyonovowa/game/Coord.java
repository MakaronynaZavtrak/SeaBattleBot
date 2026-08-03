package org.urfu.semyonovowa.game;

/**
 * Координата клетки игрового поля (строка, столбец).
 *
 * Введена взамен разбросанных по геометрии {@code split(" ")} и склеек
 * {@code y + " " + x}: разбор и форматирование теперь в одном месте, а
 * арифметика ведётся над типизированным значением.
 *
 * Строковое представление ("строка столбец") намеренно совпадает с прежним
 * форматом callbackData и ключей карт — это позволяет держать хранилище
 * строковым до этапа 6 (битборд), не ломая границы.
 */
public record Coord(int row, int col)
{
    /** Размер поля (8x8): допустимые индексы — 0..7. */
    public static final int BOARD_SIZE = 8;

    /**
     * Разбирает строку формата "строка столбец".
     */
    public static Coord parse(String coordinates)
    {
        String[] parts = coordinates.split(" ");
        return new Coord(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * Собирает координату из переменной и фиксированной осей.
     * @param variable     значение переменной оси
     * @param fixed        значение фиксированной оси
     * @param variableAxis индекс переменной оси: 0 — строка, 1 — столбец
     */
    public static Coord of(int variable, int fixed, int variableAxis)
    {
        return variableAxis == 0 ? new Coord(variable, fixed) : new Coord(fixed, variable);
    }

    /**
     * Значение оси по индексу: 0 — строка, 1 — столбец.
     */
    public int axis(int index)
    {
        return index == 0 ? row : col;
    }

    /**
     * @return true, если координата лежит в пределах поля.
     */
    public boolean isOnBoard()
    {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    @Override
    public String toString()
    {
        return row + " " + col;
    }
}
