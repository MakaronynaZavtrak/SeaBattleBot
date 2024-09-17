package org.University.user;
/**
 * класс myUser предназначен для хранения необходимых данных о пользователе
 * для возможности более корректной обработки целостной логики программы
 */
public class myUser {
    private final Long chatId;
    private final String userName;
    private final String firstName;
    private String state;
    public myUser(Long chatId, String userName, String firstName, String state)
    {
        this.chatId = chatId;
        this.userName = userName;
        this.firstName = firstName;
        this.state = state;
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
}
