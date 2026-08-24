package org.urfu.semyonovowa.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.urfu.semyonovowa.dataBase.GameSnapshot;
import org.urfu.semyonovowa.user.MyUser;
import org.urfu.semyonovowa.user.State;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что {@link Game#toSnapshot()} и {@link Game#restore} — взаимно
 * обратны: слепок, восстановленный в игру и снятый заново, совпадает с исходным.
 * Это гарантирует, что логическое состояние (корабли, попадания, выстрелы, фаза,
 * очередь хода) переживает сериализацию в БД без потерь. Рендер полей проверяется
 * тем, что restore не падает на пересборке.
 */
@DisplayName("Маппинг Game <-> GameSnapshot")
class GameSnapshotMappingTest
{
    private GameSnapshot.ShipState emptyShip()
    {
        return new GameSnapshot.ShipState(List.of(), null, 0);
    }

    @Test
    @DisplayName("restore -> toSnapshot возвращает исходный слепок без потерь")
    void restoreThenSnapshotRoundTrips()
    {
        MyUser creator = new MyUser(1L, "alice", "Alice", State.IN_LOBBY);
        MyUser invited = new MyUser(2L, "bob", "Bob", State.IN_LOBBY);

        // Согласованный сценарий боя:
        //  creator стрелял по "5 5" (потопил бот соперника) и "5 6" (мимо);
        //  invited стрелял по "0 0" (ранил линкор creator) и "1 1" (мимо).
        long creatorShots = (1L << (5 * 8 + 5)) | (1L << (5 * 8 + 6)); // 45, 46
        long invitedShots = (1L << 0) | (1L << (1 * 8 + 1));           // 0, 9
        long hitsOnCreator = 1L << 0;                                  // "0 0"
        long hitsOnInvited = 1L << (5 * 8 + 5);                        // "5 5"

        GameSnapshot original = new GameSnapshot(1L, 2L, List.of(
                new GameSnapshot.PlayerState(1L, List.of(
                        new GameSnapshot.ShipState(List.of("0 0", "0 1", "0 2", "0 3"), "VERTICAL", 0),
                        new GameSnapshot.ShipState(List.of("2 0", "2 1", "2 2"), "VERTICAL", 2),
                        emptyShip(), emptyShip(), emptyShip(), emptyShip(), emptyShip()),
                        hitsOnCreator, creatorShots, "MOVING", true),
                new GameSnapshot.PlayerState(2L, List.of(
                        emptyShip(), emptyShip(), emptyShip(), emptyShip(),
                        new GameSnapshot.ShipState(List.of("5 5"), null, 5),
                        emptyShip(), emptyShip()),
                        hitsOnInvited, invitedShots, "WAITING", false)));

        Game restored = Game.restore(original, creator, invited);
        GameSnapshot again = restored.toSnapshot();

        assertThat(again).isEqualTo(original);
    }
}
