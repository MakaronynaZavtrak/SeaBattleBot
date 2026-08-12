package org.urfu.semyonovowa.game;

/**
 * Игровое поле 8×8, упакованное в один {@code long}: бит с индексом
 * {@code row * 8 + col} соответствует клетке (row, col).
 *
 * Неизменяемое значение (как {@link Coord}): модифицирующие операции возвращают
 * новый {@code BitBoard}. Занятость, пересечения и «потоплен» считаются битовыми
 * операциями за O(1).
 */
public record BitBoard(long bits)
{
    /** Столбец 0 (клетки 0,8,…,56). */
    private static final long FILE_FIRST = 0x0101010101010101L;
    /** Столбец 7 (клетки 7,15,…,63). */
    private static final long FILE_LAST = 0x8080808080808080L;

    public static BitBoard empty()
    {
        return new BitBoard(0L);
    }

    private static int index(Coord coord)
    {
        return coord.row() * Coord.BOARD_SIZE + coord.col();
    }

    /** @return копия доски с установленной клеткой coord. */
    public BitBoard set(Coord coord)
    {
        return new BitBoard(bits | (1L << index(coord)));
    }

    /** @return копия доски со снятой клеткой coord. */
    public BitBoard clear(Coord coord)
    {
        return new BitBoard(bits & ~(1L << index(coord)));
    }

    /** @return true, если клетка coord занята. */
    public boolean test(Coord coord)
    {
        return (bits & (1L << index(coord))) != 0;
    }

    /** @return количество занятых клеток. */
    public int count()
    {
        return Long.bitCount(bits);
    }

    public boolean isEmpty()
    {
        return bits == 0L;
    }

    public BitBoard or(BitBoard other)
    {
        return new BitBoard(bits | other.bits);
    }

    public BitBoard and(BitBoard other)
    {
        return new BitBoard(bits & other.bits);
    }

    /** @return клетки этой доски, которых нет в other (this AND NOT other). */
    public BitBoard andNot(BitBoard other)
    {
        return new BitBoard(bits & ~other.bits);
    }

    /** @return true, если у досок есть хотя бы одна общая клетка. */
    public boolean intersects(BitBoard other)
    {
        return (bits & other.bits) != 0L;
    }

    /** @return true, если все клетки other присутствуют на этой доске (для проверки «потоплен»). */
    public boolean contains(BitBoard other)
    {
        return (bits & other.bits) == other.bits;
    }

    /**
     * Блок 3×3 вокруг клетки (включая её саму), обрезанный по краям поля —
     * «ход короля». Считается сдвигами с масками столбцов, чтобы клетки не
     * переползали с края на край.
     */
    public static BitBoard blockAround(Coord coord)
    {
        long cell = 1L << index(coord);
        long horizontalBand = cell | ((cell << 1) & ~FILE_FIRST) | ((cell >>> 1) & ~FILE_LAST);
        long block = horizontalBand | (horizontalBand << Coord.BOARD_SIZE) | (horizontalBand >>> Coord.BOARD_SIZE);
        return new BitBoard(block);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < Coord.BOARD_SIZE; row++)
        {
            for (int col = 0; col < Coord.BOARD_SIZE; col++)
                builder.append(test(new Coord(row, col)) ? '#' : '.');
            builder.append('\n');
        }
        return builder.toString();
    }
}
