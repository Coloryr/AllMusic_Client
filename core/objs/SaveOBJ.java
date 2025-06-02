package com.coloryr.allmusic.client.core.objs;

public class SaveOBJ {

    public PosOBJ list;
    public PosOBJ lyric;
    public PosOBJ info;
    public PosOBJ pic;
    public int picRotateSpeed;

    public SaveOBJ() {
        picRotateSpeed = 10;
        list = new PosOBJ();
        lyric = new PosOBJ();
        info = new PosOBJ();
        pic = new PosOBJ();
    }
}
