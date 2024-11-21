package org.university.user;
/**
 * класс myUser предназначен для хранения необходимых данных о пользователе
 * для возможности более корректной обработки целостной логики программы
 */
public class MyUser {
    private final Long chatId;
    private final String userName;
    private final String firstName;
    private String state;
    private boolean invitedFlag;
    public MyUser(Long chatId, String userName, String firstName, String state)
    {
        this.chatId = chatId;
        this.userName = userName;
        this.firstName = firstName;
        this.state = state;
        this.invitedFlag = false;
    }

    public Long getChatId()
    {
        return chatId;
    }
    public String getUsername()
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

    public boolean isInvited() {return this.invitedFlag;}
    public void setInvitedFlag(boolean flag){this.invitedFlag = flag;}
}
