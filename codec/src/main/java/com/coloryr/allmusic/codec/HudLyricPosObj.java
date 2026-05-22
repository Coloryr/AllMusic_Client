package com.coloryr.allmusic.codec;

public class HudLyricPosObj extends HudBasePosObj {
    public int color;
    public int gap;
    public boolean shadow;
    public int maxWidth;

    public HudLyricPosObj copy() {
        HudLyricPosObj pos = new HudLyricPosObj();
        pos.x = x;
        pos.y = y;
        pos.alpha = alpha;
        pos.color = color;
        pos.enable = enable;
        pos.maxWidth = maxWidth;
        pos.shadow = shadow;
        pos.gap = gap;

        return pos;
    }
}
