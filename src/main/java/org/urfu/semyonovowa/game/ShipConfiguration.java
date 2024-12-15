package org.urfu.semyonovowa.game;

public class ShipConfiguration
{
    private int y;
    private int x;
    private int varUnitIdx;
    private int step;
    private int amountWays;
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
    public void setAmountWays(int amountWays) {this.amountWays = amountWays;}
    public boolean canBeConfigured(){return this.configFlag;}
    public int getVarUnitIdx() {return varUnitIdx;}
    public int getStep() {return step;}
    public int getAmountWays() {return amountWays;}
    public int getY() {return y;}
    public int getX() {return x;}
}
