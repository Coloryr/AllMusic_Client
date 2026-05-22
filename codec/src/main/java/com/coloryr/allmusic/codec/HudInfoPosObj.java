package com.coloryr.allmusic.codec;

public class HudInfoPosObj extends HudBasePosObj {
    public int color;
    public boolean shadow;
    public int maxWidth;
    public int gap;
    public LoopType loop;

    public HudInfoPosObj copy() {
        HudInfoPosObj pos = new HudInfoPosObj();
        pos.x = x;
        pos.y = y;
        pos.alpha = alpha;
        pos.loop = loop;
        pos.color = color;
        pos.enable = enable;
        pos.gap = gap;
        pos.maxWidth = maxWidth;
        pos.shadow = shadow;

        return pos;
    }
}
