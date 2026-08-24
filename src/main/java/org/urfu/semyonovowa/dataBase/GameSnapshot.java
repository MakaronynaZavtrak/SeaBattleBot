package org.urfu.semyonovowa.dataBase;

import java.util.List;

/**
 * Сериализуемый слепок логического состояния партии для персистентности.
 * Содержит только то, из чего игру можно полностью восстановить; рендер полей
 * (TelegramField) не хранится — он пересобирается из этого слепка при загрузке.
 *
 * @param creatorChatId  chat_id создателя (он же game_id)
 * @param opponentChatId chat_id соперника
 * @param players        состояние обоих игроков
 */
public record GameSnapshot(long creatorChatId, long opponentChatId, List<PlayerState> players) {
    /**
     * @param chatId    chat_id игрока
     * @param ships     корабли игрока в фиксированном порядке (7 штук)
     * @param hits      доска ВХОДЯЩИХ попаданий по кораблям этого игрока (BitBoard в long)
     * @param usedCages доска выстрелов ЭТОГО игрока по сопернику (BitBoard в long) —
     *                  нужна для меток промахов и защиты от повторного выстрела
     * @param state     фаза игрока (имя MyUser.State) на момент снапшота
     * @param firstMove флаг первого хода этого игрока
     */
    public record PlayerState(long chatId, List<ShipState> ships, long hits, long usedCages,
                              String state, boolean firstMove) {}

    /**
     * Состояние одного корабля.
     * @param cells       занятые клетки "y x" (пустой список, если ещё не расставлен)
     * @param orientation ориентация ("VERTICAL"/"HORIZONTAL") или null, если не задана
     * @param fixedVal    зафиксированная координата вдоль неизменной оси (для достройки)
     */
    public record ShipState(List<String> cells, String orientation, int fixedVal) {}
}
