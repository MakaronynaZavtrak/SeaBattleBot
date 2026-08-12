package org.urfu.semyonovowa.ship;

import java.util.HashSet;
import java.util.Set;

/**
 * Базовый корабль. Абстрактный: конкретное число жизней (= размер корабля)
 * задают наследники (Boat, Esminez, Cruiser, LinCore), поэтому «пустого»
 * корабля не существует. Поражения теперь отслеживаются битовой доской hits
 * в Game, а не счётчиком на корабле, поэтому lives неизменяемо.
 */
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

    public int getLives(){return this.lives;}
    public Set<String> getCoordinatesSet(){return this.coordinatesSet;}
    public Orientation getOrientation(){return this.orientation;}
    public void setOrientation(Orientation orientation) {this.orientation = orientation;}
    public int getFixedVal() {return fixedVal;}
    public void setFixedVal(int fixedVal) {this.fixedVal = fixedVal;}
}
