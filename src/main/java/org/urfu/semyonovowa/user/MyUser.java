package org.urfu.semyonovowa.user;
/**
 * класс myUser предназначен для хранения необходимых данных о пользователе
 * для возможности более корректной обработки целостной логики программы
 */
public class MyUser {
    private final Long chatId;
    private final String userName;
    private final String firstName;
    private String state;
    private int wins;
    private int loses;
    private int experience;
    private int currentRankIdx;
    private final Integer lastMessageId;
    public MyUser(Long chatId, String userName, String firstName, String state)
    {
        this.chatId = chatId;
        this.userName = userName;
        this.firstName = firstName;
        this.state = state;
        this.wins = 0;
        this.loses = 0;
        this.experience = 0;
        this.currentRankIdx = 0;
        this.lastMessageId = null;
    }
    public MyUser(Long chatId, String userName, String firstName, int wins, int loses, int experience,
                  int currentRankIdx, Integer lastMessageId)
    {
        this.chatId = chatId;
        this.userName = userName;
        this.firstName = firstName;
        this.wins = wins;
        this.loses = loses;
        this.experience = experience;
        this.currentRankIdx = currentRankIdx;
        this.lastMessageId = lastMessageId;
        this.state = State.IN_LOBBY;
    }

    public static MyUserBuilder builder() { return new MyUserBuilder(); }

    public Long getChatId()
    {
        return chatId;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public int getWins() { return wins; }

    public void incrementWins() { this.wins++; }

    public int getLoses() { return loses; }

    public void incrementLoses() { this.loses++; }

    public int getCurrentRankIdx(){ return this.currentRankIdx; }

    public void incrementCurrentRankIdx(){ this.currentRankIdx++; }

    public int getExperience() { return experience; }

    public void increaseExperience(int addend) { this.experience += addend;}

    public Integer getLastMessageId() { return lastMessageId; }
}
