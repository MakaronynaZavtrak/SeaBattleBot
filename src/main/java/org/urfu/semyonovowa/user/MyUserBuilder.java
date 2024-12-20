package org.urfu.semyonovowa.user;

public class MyUserBuilder
{
    private Long chatId;
    private String userName;
    private String firstName;
    private int wins;
    private int loses;
    private int experience;
    private int currentRankIdx;
    private Integer lastMessageId;

    public MyUserBuilder chatId(Long chatId)
    {
        this.chatId = chatId;
        return this;
    }

    public MyUserBuilder userName(String userName)
    {
        this.userName = userName;
        return this;
    }

    public MyUserBuilder firstName(String firstName)
    {
        this.firstName = firstName;
        return this;
    }

    public MyUserBuilder wins(int wins)
    {
        this.wins = wins;
        return this;
    }

    public MyUserBuilder loses(int loses)
    {
        this.loses = loses;
        return this;
    }

    public MyUserBuilder experience(int experience)
    {
        this.experience = experience;
        return this;
    }

    public MyUserBuilder currentRankIdx(int currentRankIdx)
    {
        this.currentRankIdx = currentRankIdx;
        return this;
    }

    public MyUserBuilder lastMessageId(Integer lastMessageId)
    {
        this.lastMessageId = lastMessageId;
        return this;
    }

    public MyUser build()
    {
        return  new MyUser(chatId, userName, firstName, wins, loses, experience, currentRankIdx, lastMessageId);
    }
}
