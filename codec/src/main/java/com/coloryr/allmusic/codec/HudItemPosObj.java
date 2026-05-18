package com.coloryr.allmusic.codec;

public class HudItemPosObj {
    public int x;
    public int y;
    public HudDirType dir;
    public int color;
    public float alpha;
    public boolean shadow;
    public boolean enable;

    public HudItemPosObj() {

    }

    public HudItemPosObj(int x, int y, HudDirType type, int color, boolean shadow, boolean enable, float alpha) {
        this.x = x;
        this.y = y;
        this.dir = type;
        this.color = color;
        this.shadow = shadow;
        this.enable = enable;
        this.alpha = alpha;
    }

    public HudItemPosObj copy() {
        return new HudItemPosObj(this.x, this.y, this.dir, this.color, this.shadow, this.enable, this.alpha);
    }
}
