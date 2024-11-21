package org.university.field;

/**
 * описывает свойства атомарной ячейку игрового поля
 */
public class FieldCell
{
    private final int x;
    private final int y;
    private String emodji;
    public FieldCell(int x, int y, String emodzi)
    {
        this.x = x;
        this.y = y;
        this.emodji = emodzi;
    }
    public int getX() {return x;}
    public int getY() {return y;}
    public String getEmodji()
    {
        return emodji;
    }
    public void setEmodji(String emodji)
    {
        this.emodji = emodji;
    }
}
