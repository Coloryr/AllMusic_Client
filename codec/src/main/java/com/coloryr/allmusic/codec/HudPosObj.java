package com.coloryr.allmusic.codec;

public class HudPosObj {
    public HudItemPosObj list;
    public HudItemPosObj lyric;
    public HudItemPosObj info;
    public HudItemPosObj pic;
    public int picRotateSpeed;

    public static HudPosObj make() {
        HudPosObj obj = new HudPosObj();
        obj.init();

        return obj;
    }

    public static HudItemPosObj makeDefault(HudType pos) {
        switch (pos) {
            case INFO:
                return new HudItemPosObj(74, 2, HudDirType.TOP_LEFT, 0xffffffff, true, true, 1.0f);
            case LIST:
                return new HudItemPosObj(2, 74, HudDirType.TOP_LEFT, 0xffffffff, true, true, 1.0f);
            case LYRIC:
                return new HudItemPosObj(74, 53, HudDirType.TOP_LEFT, 0xffffffff, true, true, 1.0f);
            case PIC:
                return new HudItemPosObj(2, 2, HudDirType.TOP_LEFT, 70, true, true, 1.0f);
        }

        return new HudItemPosObj(0, 0, HudDirType.TOP_LEFT, 0xffffff, false, true, 1.0f);
    }

    public HudPosObj copy() {
        HudPosObj obj1 = new HudPosObj();
        obj1.info = this.info.copy();
        obj1.list = this.list.copy();
        obj1.lyric = this.lyric.copy();
        obj1.pic = this.pic.copy();
        obj1.picRotateSpeed = this.picRotateSpeed;
        return obj1;
    }

    public boolean check() {
        boolean save = false;
        if (list == null) {
            save = true;
            list = makeDefault(HudType.LIST);
        }
        if (lyric == null) {
            save = true;
            lyric = makeDefault(HudType.LYRIC);
        }
        if (info == null) {
            save = true;
            info = makeDefault(HudType.INFO);
        }
        if (pic == null) {
            save = true;
            pic = makeDefault(HudType.PIC);
        }
        return save;
    }

    public void init() {
        picRotateSpeed = 30;
        list = new HudItemPosObj(2, 74, HudDirType.TOP_LEFT, 0xffffffff, true, true, 1.0f);
        lyric = new HudItemPosObj(74, 53, HudDirType.TOP_LEFT, 0xffffffff, true, true, 1.0f);
        info = new HudItemPosObj(74, 2, HudDirType.TOP_LEFT, 0xffffffff, true, true, 1.0f);
        pic = new HudItemPosObj(2, 2, HudDirType.TOP_LEFT, 70, true, true, 1.0f);
    }
}