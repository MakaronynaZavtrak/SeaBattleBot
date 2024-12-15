package org.urfu.semyonovowa.user;

public final class State
{
    private State(){}
    public static final String IN_LOBBY = "in_lobby";
    public static final String LINCORE_SETTING = "is_setting_linCore";
    public static final String CRUISER_SETTING = "is_setting_cruiser";
    public static final String ESMINEZ_1_SETTTING = "is_setting_esminez1";
    public static final String ESMINEZ_2_SETTTING = "is_setting_esminez2";
    public static final String BOAT_1_SETTING = "is_setting_boat1";
    public static final String BOAT_2_SETTING = "is_setting_boat2";
    public static final String BOAT_3_SETTING = "is_setting_boat3";
    public static final String MOVING = "is_moving";
    public static final String WAITING = "is_waiting";
    public static final String FINISHED_GAME = "has_finished_the_game";
    public static final String WANT_TO_REPLAY = "wanna_to_replay";
    public static final String READY_TO_PLAY = "is_ready_to_play";
}
