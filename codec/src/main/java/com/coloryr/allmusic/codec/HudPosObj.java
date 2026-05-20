package com.coloryr.allmusic.codec;

public class HudPosObj {
    public HudItemPosObj list;
    public HudItemPosObj lyric;
    public HudItemPosObj info;
    public HudPicPosObj pic;

    public static HudPosObj make() {
        HudPosObj obj = new HudPosObj();
        obj.init();

        return obj;
    }

    public static HudItemPosObj makeDefault(HudType pos) {
        switch (pos) {
            case INFO:
                return new HudItemPosObj(74, 2, HudPosType.TOP_LEFT, 0xffffff, true, true, 1.0f, LoopType.NO_LOOP, 1000);
            case LIST:
                return new HudItemPosObj(2, 74, HudPosType.TOP_LEFT, 0xffffff, true, true, 1.0f, LoopType.NO_LOOP, 1000);
            case LYRIC:
                return new HudItemPosObj(74, 53, HudPosType.TOP_LEFT, 0xffffff, true, true, 1.0f, LoopType.NO_LOOP, 1000);
            case PIC:
                return new HudItemPosObj(2, 2, HudPosType.TOP_LEFT, 70, true, true, 1.0f, LoopType.NO_LOOP, 1000);
        }

        return new HudItemPosObj(0, 0, HudPosType.TOP_LEFT, 0xffffff, false, true, 1.0f, LoopType.NO_LOOP, 1000);
    }

    public static HudPicPosObj makePicDefault() {
        return new HudPicPosObj(2, 2, HudPosType.TOP_LEFT, 70, true, true, 1.0f, 30);
    }

    public HudPosObj copy() {
        HudPosObj obj1 = new HudPosObj();
        obj1.info = this.info.copy();
        obj1.list = this.list.copy();
        obj1.lyric = this.lyric.copy();
        obj1.pic = this.pic.copy();
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
            pic = makePicDefault();
        }
        return save;
    }

    public void init() {
        list = makeDefault(HudType.LIST);
        lyric = makeDefault(HudType.LYRIC);
        info = makeDefault(HudType.INFO);
        pic = makePicDefault();
    }
}