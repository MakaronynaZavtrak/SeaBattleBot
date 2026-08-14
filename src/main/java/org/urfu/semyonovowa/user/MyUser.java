package org.urfu.semyonovowa.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Класс MyUser предназначен для хранения необходимых данных о пользователе.
 * Геттеры и билдер генерирует Lombok. getChatId оставлен вручную, чтобы
 * возвращать примитив long — иначе сравнения chatId через == сломались бы
 * (сравнение ссылок вместо значений). @Builder висит на 8-арг конструкторе,
 * чтобы сохранить дефолт state = IN_LOBBY, как в прежнем самописном билдере.
 */
@Getter
public class MyUser
{
    private final Long chatId;
    @Setter private String userName;
    private final String firstName;
    @Setter private State state;
    private int wins;
    private int loses;
    private int experience;
    private int currentRankIdx;
    private final Integer lastMessageId;

    public MyUser(Long chatId, String userName, String firstName, State state)
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

    @Builder
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

    public long getChatId() { return chatId; }

    public void incrementWins() { this.wins++; }
    public void incrementLoses() { this.loses++; }
    public void incrementCurrentRankIdx() { this.currentRankIdx++; }
    public void increaseExperience(int addend) { this.experience += addend; }
}
