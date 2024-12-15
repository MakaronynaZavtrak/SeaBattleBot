package org.urfu.semyonovowa.game;

public enum MovingInformationForBothPlayers {
    KILL_INFO(MovingInformation.CURRENT_USER_KILL,
            MovingInformation.PAIR_USER_KILL),
    WIN_INFO(MovingInformation.CURRENT_USER_WIN,
                MovingInformation.PAIR_USER_WIN),
    HURT_INFO(MovingInformation.CURRENT_USER_HURT,
            MovingInformation.PAIR_USER_HURT),
    MISS_INFO(MovingInformation.CURRENT_USER_MISS,
                MovingInformation.PAIR_USER_MISS);
    public final String currentUserInformation;
    public final String pairUserInformation;

    MovingInformationForBothPlayers(String currentUserInformation, String pairUserInformation)
    {
        this.currentUserInformation = currentUserInformation;
        this.pairUserInformation = pairUserInformation;
    }
}
