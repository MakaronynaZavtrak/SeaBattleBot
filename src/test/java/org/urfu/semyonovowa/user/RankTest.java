package org.urfu.semyonovowa.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты званий: пороги опыта и порядок в {@link RankList}.
 */
class RankTest
{
    @Test
    @DisplayName("крайние звания имеют ожидаемые пороги опыта")
    void boundaryRanksHaveExpectedExperience()
    {
        assertThat(Rank.SAILOR.rank).isEqualTo("Матрос");
        assertThat(Rank.SAILOR.experience).isEqualTo(20);
        assertThat(Rank.NEPTUNE_BROTHER.experience).isEqualTo(10000);
    }

    @Test
    @DisplayName("список содержит 11 званий в правильном порядке")
    void rankListHasElevenRanksInOrder()
    {
        assertThat(RankList.ranks).hasSize(11);
        assertThat(RankList.ranks.get(0)).isEqualTo(Rank.SAILOR);
        assertThat(RankList.ranks.get(10)).isEqualTo(Rank.NEPTUNE_BROTHER);
    }

    @Test
    @DisplayName("пороги опыта строго возрастают по списку")
    void experienceThresholdsAreStrictlyIncreasing()
    {
        for (int i = 1; i < RankList.ranks.size(); i++)
            assertThat(RankList.ranks.get(i).experience)
                    .isGreaterThan(RankList.ranks.get(i - 1).experience);
    }
}
