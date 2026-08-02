package org.urfu.semyonovowa.dataBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты чистой арифметики {@link DataBaseHandler#getUserWinRate(int, int)}.
 * Метод не обращается к БД, поэтому обработчик создаётся с фиктивными параметрами.
 */
class DataBaseHandlerWinRateTest
{
    private final DataBaseHandler handler =
            new DataBaseHandler("driver", "url", new Properties());

    @Test
    @DisplayName("3 победы, 1 поражение — 75.00%")
    void threeWinsOneLoss()
    {
        assertThat(handler.getUserWinRate(3, 1)).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("2 победы, 1 поражение — округление до 66.67%")
    void twoWinsOneLoss()
    {
        assertThat(handler.getUserWinRate(2, 1)).isEqualByComparingTo("66.67");
    }

    @Test
    @DisplayName("1 победа, 2 поражения — округление до 33.33%")
    void oneWinTwoLosses()
    {
        assertThat(handler.getUserWinRate(1, 2)).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("без поражений — 100.00% (в т.ч. при нуле игр)")
    void noLossesYields100()
    {
        assertThat(handler.getUserWinRate(5, 0)).isEqualByComparingTo("100.00");
        assertThat(handler.getUserWinRate(0, 0)).isEqualByComparingTo("100.00");
    }
}
