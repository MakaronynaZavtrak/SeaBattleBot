package org.urfu.semyonovowa.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты {@link BitBoard}. Ключевой — дифференциальная проверка
 * {@code blockAround} (битовая версия) против наивной реализации по всем
 * 64 клеткам: доказывает, что сдвиги с масками столбцов не переползают через край.
 */
class BitBoardTest
{
    /** Наивный эталон блока 3×3 вокруг клетки, обрезанного по краям поля. */
    private long naiveBlock(int row, int col)
    {
        long mask = 0L;
        for (int i = -1; i < 2; i++)
            for (int j = -1; j < 2; j++)
            {
                int r = row + i, c = col + j;
                if (r >= 0 && r < 8 && c >= 0 && c < 8)
                    mask |= 1L << (r * 8 + c);
            }
        return mask;
    }

    @Test
    @DisplayName("blockAround совпадает с наивной реализацией на всех 64 клетках")
    void blockAroundMatchesNaiveEverywhere()
    {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                assertThat(BitBoard.blockAround(new Coord(row, col)).bits())
                        .as("клетка (%d,%d)", row, col)
                        .isEqualTo(naiveBlock(row, col));
    }

    @Test
    @DisplayName("размер блока: угол 4, край 6, центр 9")
    void blockSizesByPosition()
    {
        assertThat(BitBoard.blockAround(new Coord(0, 0)).count()).isEqualTo(4);
        assertThat(BitBoard.blockAround(new Coord(0, 3)).count()).isEqualTo(6);
        assertThat(BitBoard.blockAround(new Coord(3, 3)).count()).isEqualTo(9);
    }

    @Test
    @DisplayName("set / test / clear / count")
    void setTestClearCount()
    {
        BitBoard board = BitBoard.empty()
                .set(new Coord(0, 0))
                .set(new Coord(7, 7))
                .set(new Coord(3, 4));

        assertThat(board.count()).isEqualTo(3);
        assertThat(board.test(new Coord(3, 4))).isTrue();
        assertThat(board.test(new Coord(3, 3))).isFalse();

        BitBoard cleared = board.clear(new Coord(3, 4));
        assertThat(cleared.count()).isEqualTo(2);
        assertThat(cleared.test(new Coord(3, 4))).isFalse();
    }

    @Test
    @DisplayName("пустая доска пуста")
    void emptyBoardIsEmpty()
    {
        assertThat(BitBoard.empty().isEmpty()).isTrue();
        assertThat(BitBoard.empty().set(new Coord(0, 0)).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("or / and / andNot / intersects")
    void setAlgebra()
    {
        BitBoard x = BitBoard.empty().set(new Coord(1, 1)).set(new Coord(2, 2));
        BitBoard y = BitBoard.empty().set(new Coord(2, 2)).set(new Coord(3, 3));

        assertThat(x.or(y).count()).isEqualTo(3);
        assertThat(x.and(y).test(new Coord(2, 2))).isTrue();
        assertThat(x.and(y).count()).isEqualTo(1);
        assertThat(x.andNot(y).test(new Coord(1, 1))).isTrue();
        assertThat(x.andNot(y).count()).isEqualTo(1);
        assertThat(x.intersects(y)).isTrue();
        assertThat(BitBoard.empty().set(new Coord(0, 0)).intersects(x)).isFalse();
    }

    @Test
    @DisplayName("contains определяет «потоплен» (все клетки корабля поражены)")
    void containsDetectsSunkShip()
    {
        BitBoard ship = BitBoard.empty().set(new Coord(4, 0)).set(new Coord(4, 1));
        BitBoard hits = BitBoard.empty().set(new Coord(4, 0));

        assertThat(hits.contains(ship)).isFalse();
        assertThat(hits.set(new Coord(4, 1)).contains(ship)).isTrue();
    }
}
