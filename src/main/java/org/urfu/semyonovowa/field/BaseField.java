package org.urfu.semyonovowa.field;

import java.util.ArrayList;
/**
 * Является базовым представлением игрового поля независимо от используемого API
 */
public class BaseField
{
    private final ArrayList<ArrayList<FieldCell>> fieldCellList;

    public BaseField()
    {
        fieldCellList = new ArrayList<>();
        for (int i = 0; i < 8; i++)
        {
            ArrayList<FieldCell> newLine = new ArrayList<>();
            for (int j = 0; j < 8; j++)
                newLine.add(new FieldCell(i, j, FieldEmoji.WATER_WAVE));
            fieldCellList.add(newLine);
        }
    }
    public ArrayList<ArrayList<FieldCell>> getFieldCellList(){return this.fieldCellList;}
}
