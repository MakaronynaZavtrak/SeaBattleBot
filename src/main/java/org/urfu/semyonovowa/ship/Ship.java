package org.urfu.semyonovowa.ship;

import java.util.HashSet;
import java.util.Set;

/**
 * Базовый корабль. Абстрактный: конкретное число жизней задают наследники
 * (Boat, Esminez, Cruiser, LinCore), поэтому «пустого» корабля не существует.
 */
public abstract class Ship
{
    private int lives;
    private final Set<String> coordinatesSet;
    private Orientation orientation;
    private final Set<String> damagedCages;
    private int fixedVal;

    protected Ship(int lives)
    {
        this.lives = lives;
        this.coordinatesSet = new HashSet<>();
        this.damagedCages = new HashSet<>();
        this.orientation = null;
    }

    public int getLives(){return this.lives;}
    public void decreaseLivesByOne(){this.lives--;}
    public Set<String> getCoordinatesSet(){return this.coordinatesSet;}
    public Set<String> getDamagedCages(){return this.damagedCages;}
    public Orientation getOrientation(){return this.orientation;}
    public void setOrientation(Orientation orientation) {this.orientation = orientation;}
    public int getFixedVal() {return fixedVal;}
    public void setFixedVal(int fixedVal) {this.fixedVal = fixedVal;}
}
