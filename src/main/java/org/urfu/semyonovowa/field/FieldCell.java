package org.urfu.semyonovowa.field;

/**
 * описывает свойства атомарной ячейку игрового поля
 */
public class FieldCell
{
    public final int x;
    public final int y;
    public final String emoji;
    public final String coordinate;
    public FieldCell(int y, int x, String emoji)
    {
        this.x = x;
        this.y = y;
        this.emoji = emoji;
        this.coordinate = this.y + " " + this.x;
    }
}
