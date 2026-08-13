package org.urfu.semyonovowa.bot;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.urfu.semyonovowa.game.Game;
import org.urfu.semyonovowa.user.MyUser;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище игровой сессионной памяти в оперативной памяти: активные игры,
 * пары соперников, приглашения и стеки сообщений по чатам.
 *
 * Вынесено из god-класса, чтобы {@code TelegramBot} не владел этим состоянием
 * сам (принцип единственной ответственности). Карты — {@link ConcurrentHashMap}:
 * отдельные операции над картой потокобезопасны. Многошаговая игровая логика
 * (get-then-modify над одной игрой) атомарной этим НЕ становится — за это
 * отвечает сериализация по игре на этапе виртуальных потоков.
 */
public class SessionRegistry
{
    private final Map<Long, Game> games = new ConcurrentHashMap<>();
    private final Map<Long, Long> userPairs = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Integer>> invitationMessages = new ConcurrentHashMap<>();
    private final Map<Long, MyUser> invitedUsers = new ConcurrentHashMap<>();
    private final Map<Long, Stack<Message>> messageStacks = new ConcurrentHashMap<>();

    public Map<Long, Game> games() { return games; }
    public Map<Long, Long> userPairs() { return userPairs; }
    public Map<Long, Map<Long, Integer>> invitationMessages() { return invitationMessages; }
    public Map<Long, MyUser> invitedUsers() { return invitedUsers; }
    public Map<Long, Stack<Message>> messageStacks() { return messageStacks; }
}
