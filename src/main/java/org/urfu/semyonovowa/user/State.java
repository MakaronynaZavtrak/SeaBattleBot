package org.urfu.semyonovowa.user;

/**
 * Состояние пользователя в конечном автомате бота.
 * Пришло на смену набору строковых констант: теперь переходы типобезопасны,
 * а switch по состоянию проверяется компилятором на полноту.
 */
public enum State
{
    IN_LOBBY,
    LINCORE_SETTING,
    CRUISER_SETTING,
    ESMINEZ_1_SETTING,
    ESMINEZ_2_SETTING,
    BOAT_1_SETTING,
    BOAT_2_SETTING,
    BOAT_3_SETTING,
    MOVING,
    WAITING,
    FINISHED_GAME,
    WANT_TO_REPLAY,
    READY_TO_PLAY;

    /**
     * @return true, если пользователь сейчас расставляет корабль
     * (замена прежнему строковому хаку {@code state.contains("setting")}).
     */
    public boolean isPlacingShip()
    {
        return switch (this)
        {
            case LINCORE_SETTING, CRUISER_SETTING,
                 ESMINEZ_1_SETTING, ESMINEZ_2_SETTING,
                 BOAT_1_SETTING, BOAT_2_SETTING, BOAT_3_SETTING -> true;
            default -> false;
        };
    }
}
