package org.urfu.semyonovowa.dataBase;

import java.util.List;

/**
 * Сериализуемый слепок логического состояния партии для персистентности.
 * Содержит только то, из чего игру можно полностью восстановить; рендер полей
 * (TelegramField) не хранится — он пересобирается при загрузке.
 *
 * @param creatorChatId  chat_id создателя (он же game_id)
 * @param opponentChatId chat_id соперника
 * @param players        состояние обоих игроков
 */
public record GameSnapshot(long creatorChatId,
                           long opponentChatId,
                           List<PlayerState> players)  {
    /**
     * @param chatId        chat_id игрока
     * @param ships         корабли игрока в фиксированном порядке (7 штук); каждый —
     *                      список занятых клеток "y x" (пустой, если ещё не расставлен)
     * @param hits          доска попаданий по этому игроку, упакованная в long (BitBoard)
     * @param state         фаза игрока (имя MyUser.State) на момент снапшота
     * @param firstMove     флаг первого хода этого игрока
     */
    public record PlayerState(long chatId,
                              List<List<String>> ships,
                              long hits,
                              String state,
                              boolean firstMove) {}
}
