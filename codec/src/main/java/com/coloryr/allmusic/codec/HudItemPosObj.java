package com.coloryr.allmusic.codec;

public class HudItemPosObj extends HudBasePosObj {
    public int color;
    public boolean shadow;
    public LoopType loop;
    public int maxWidth;

    public HudItemPosObj() {

    }

    public HudItemPosObj(int x, int y, HudPosType type, int color, boolean shadow, boolean enable, float alpha, LoopType loop, int maxWidth) {
        this.x = x;
        this.y = y;
        this.pos = type;
        this.color = color;
        this.shadow = shadow;
        this.enable = enable;
        this.alpha = alpha;
        this.loop = loop;
        this.maxWidth = maxWidth;
    }

    public HudItemPosObj copy() {
        return new HudItemPosObj(this.x, this.y, this.pos, this.color, this.shadow, this.enable, this.alpha, this.loop, this.maxWidth);
    }
}
