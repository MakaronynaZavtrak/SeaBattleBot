package org.urfu.semyonovowa.bot;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.urfu.semyonovowa.game.Game;
import org.urfu.semyonovowa.user.MyUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Хранилище игровой сессионной памяти в оперативной памяти: активные игры,
 * пары соперников, приглашения и стеки сообщений по чатам.
 *
 * Вынесено из god-класса, чтобы {@code TelegramBot} не владел этим состоянием
 * сам (принцип единственной ответственности). Пока отдаёт карты напрямую —
 * это инкапсуляция владения; семантический API и потокобезопасность придут
 * следующими шагами (потокобезопасность — задача этапа 5).
 */
public class SessionRegistry
{
    private final Map<Long, Game> games = new HashMap<>();
    private final Map<Long, Long> userPairs = new HashMap<>();
    private final Map<Long, Map<Long, Integer>> invitationMessages = new HashMap<>();
    private final Map<Long, MyUser> invitedUsers = new HashMap<>();
    private final Map<Long, Stack<Message>> messageStacks = new HashMap<>();

    public Map<Long, Game> games() { return games; }
    public Map<Long, Long> userPairs() { return userPairs; }
    public Map<Long, Map<Long, Integer>> invitationMessages() { return invitationMessages; }
    public Map<Long, MyUser> invitedUsers() { return invitedUsers; }
    public Map<Long, Stack<Message>> messageStacks() { return messageStacks; }
}
