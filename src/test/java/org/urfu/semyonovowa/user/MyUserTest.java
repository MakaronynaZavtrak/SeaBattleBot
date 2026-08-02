package org.urfu.semyonovowa.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты доменного объекта {@link MyUser}: значения по умолчанию и мутаторы.
 */
class MyUserTest
{
    private MyUser newUser()
    {
        return new MyUser(42L, "oleg", "Oleg", State.IN_LOBBY);
    }

    @Test
    @DisplayName("новый пользователь создаётся с нулевой статистикой")
    void freshUserHasZeroStats()
    {
        MyUser user = newUser();

        assertThat(user.getChatId()).isEqualTo(42L);
        assertThat(user.getWins()).isZero();
        assertThat(user.getLoses()).isZero();
        assertThat(user.getExperience()).isZero();
        assertThat(user.getCurrentRankIdx()).isZero();
        assertThat(user.getState()).isEqualTo(State.IN_LOBBY);
        assertThat(user.getLastMessageId()).isNull();
    }

    @Test
    @DisplayName("счётчики побед и поражений инкрементируются")
    void winsAndLossesIncrement()
    {
        MyUser user = newUser();

        user.incrementWins();
        user.incrementWins();
        user.incrementLoses();

        assertThat(user.getWins()).isEqualTo(2);
        assertThat(user.getLoses()).isEqualTo(1);
    }

    @Test
    @DisplayName("опыт накапливается")
    void experienceAccumulates()
    {
        MyUser user = newUser();

        user.increaseExperience(10);
        user.increaseExperience(5);

        assertThat(user.getExperience()).isEqualTo(15);
    }

    @Test
    @DisplayName("индекс звания и состояние меняются")
    void rankIndexAndStateChange()
    {
        MyUser user = newUser();

        user.incrementCurrentRankIdx();
        user.setState(State.MOVING);
        user.setUserName("new_name");

        assertThat(user.getCurrentRankIdx()).isEqualTo(1);
        assertThat(user.getState()).isEqualTo(State.MOVING);
        assertThat(user.getUserName()).isEqualTo("new_name");
    }
}
