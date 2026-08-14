package org.urfu.semyonovowa.ship;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Базовый корабль. Абстрактный: конкретное число жизней (= размер корабля)
 * задают наследники (Boat, Esminez, Cruiser, LinCore), поэтому «пустого»
 * корабля не существует. Поражения отслеживаются битовой доской hits в Game,
 * а не счётчиком на корабле, поэтому lives неизменяемо.
 * Геттеры/сеттеры генерирует Lombok: у final-полей (lives, coordinatesSet)
 * сеттеров не будет, у orientation/fixedVal — будут.
 */
@Getter
@Setter
public abstract class Ship
{
    private final int lives;
    private final Set<String> coordinatesSet;
    private Orientation orientation;
    private int fixedVal;

    protected Ship(int lives)
    {
        this.lives = lives;
        this.coordinatesSet = new HashSet<>();
        this.orientation = null;
    }
}
