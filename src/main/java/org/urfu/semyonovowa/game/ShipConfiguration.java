package org.urfu.semyonovowa.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ShipConfiguration
{
    private int y;
    private int x;
    private int varUnitIdx;
    private int step;
    @Setter
    private int amountWays;
    @Getter(AccessLevel.NONE)
    private boolean configFlag;
    public ShipConfiguration()
    {
        this.configFlag = false;
        this.amountWays = 0;
    }
    public void initializeConfiguration(int y, int x, int varUnitIdx, int step)
    {
        this.y = y;
        this.x = x;
        this.varUnitIdx = varUnitIdx;
        this.step = step;
        this.configFlag = true;
    }
    public boolean canBeConfigured(){return this.configFlag;}
}
