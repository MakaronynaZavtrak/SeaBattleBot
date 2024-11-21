package org.university.ship;

import java.util.HashSet;
import java.util.Set;

import static java.lang.Math.abs;

public class Ship
{
    private int lives;
    private final Set<String> coordinatesSet;
    private char orientation;
    private final Set<String> damagedCages;
    public Ship()
    {
        this.lives = 404;
        this.coordinatesSet = new HashSet<>();
        this.damagedCages = new HashSet<>();
    }
    public int getLives(){return this.lives;}
    public void setLives(int lives){this.lives = lives;}
    public Set<String> getCoordinatesSet(){return this.coordinatesSet;}
    public Set<String> getDamagedCages(){return this.damagedCages;}
    public char getOrientation(){return this.orientation;}

    /**
     * Контролирует добавление клеток расставляемого корабля
     * @param coordinates координаты проверяемой ячейки жизни корабля
     * @return true - если, ячейку удалось занять, false - если нет
     */
    public boolean addCoordinates(String coordinates)
    {
        if (coordinatesSet.isEmpty())
        {
            coordinatesSet.add(coordinates);
            return true;
        }

        if (coordinatesSet.size() == 1)
        {
            String firstCoordinates = coordinatesSet.stream().findFirst().get();
            if (firstCoordinates.charAt(0) == coordinates.charAt(0) &&
                    abs((int)firstCoordinates.charAt(1) - (int)coordinates.charAt(1)) < lives)
            {
                orientation = firstCoordinates.charAt(0);
                coordinatesSet.add(coordinates);
                return true;
            }
            else if (firstCoordinates.charAt(1) == coordinates.charAt(1) &&
                    abs((int)firstCoordinates.charAt(0) - (int)coordinates.charAt(0)) < lives)
            {
                orientation = firstCoordinates.charAt(1);
                coordinatesSet.add(coordinates);
                return true;
            }
            return false;
        }

        if (coordinates.charAt(0) == orientation)
        {
            for (String elem: coordinatesSet)
                if (abs((int) elem.charAt(1) - (int) coordinates.charAt(1)) >= lives)
                    return false;

            coordinatesSet.add(coordinates);
            return true;
        }
        else if (coordinates.charAt(1) == orientation)
        {
            for (String elem: coordinatesSet)
                if (abs((int) elem.charAt(0) - (int) coordinates.charAt(0)) >= lives)
                    return false;

            coordinatesSet.add(coordinates);
            return true;
        }
        return false;
    }
}
