package org.urfu.semyonovowa.ship;

import java.util.HashSet;
import java.util.Set;

public class Ship
{
    private int lives;
    private final Set<String> coordinatesSet;
    private int orientation;
    private final Set<String> damagedCages;
    private int fixedVal;
    public Ship()
    {
        this.lives = 404;
        this.coordinatesSet = new HashSet<>();
        this.damagedCages = new HashSet<>();
        this.orientation = 404;
    }
    public int getLives(){return this.lives;}
    public void setLives(int lives){this.lives = lives;}
    public void decreaseLivesByOne(){this.lives--;}
    public Set<String> getCoordinatesSet(){return this.coordinatesSet;}
    public Set<String> getDamagedCages(){return this.damagedCages;}
    public int getOrientation(){return this.orientation;}
    public void setOrientation(int orientation) {this.orientation = orientation;}
    public int getFixedVal() {return fixedVal;}
    public void setFixedVal(int fixedVal) {this.fixedVal = fixedVal;}
}
