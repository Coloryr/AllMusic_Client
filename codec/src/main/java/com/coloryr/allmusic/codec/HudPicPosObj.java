package com.coloryr.allmusic.codec;

public class HudPicPosObj extends HudBasePosObj {
    public int size;
    public boolean rotate;
    public int speed;

    public HudPicPosObj() {

    }

    public HudPicPosObj(int x, int y, HudPosType type, int size, boolean rotate, boolean enable, float alpha, int speed) {
        this.x = x;
        this.y = y;
        this.pos = type;
        this.size = size;
        this.rotate = rotate;
        this.enable = enable;
        this.alpha = alpha;

        this.speed = speed;
    }

    public HudPicPosObj copy() {
        return new HudPicPosObj(this.x, this.y, this.pos, this.size, this.rotate, this.enable, this.alpha, this.speed);
    }
}
