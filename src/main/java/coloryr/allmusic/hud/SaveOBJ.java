package coloryr.allmusic.hud;

public class SaveOBJ {
    private PosOBJ List;
    private PosOBJ Lyric;
    private PosOBJ Info;
    private PosOBJ Pic;
    private boolean EnableList;
    private boolean EnableLyric;
    private boolean EnableInfo;
    private boolean EnablePic;
    private int PicSize;

    public SaveOBJ() {
        EnableList = true;
        EnableLyric = true;
        EnableInfo = true;
        EnablePic = true;
        List = new PosOBJ(2, 80);
        Lyric = new PosOBJ(2, 55);
        Info = new PosOBJ(2, 2);
        Pic = new PosOBJ(2, 2);
        PicSize = 70;
    }

    public void setPicSize(int picSize) {
        PicSize = picSize;
    }

    public int getPicSize() {
        return PicSize;
    }

    public boolean isEnablePic() {
        return EnablePic;
    }

    public PosOBJ getPic() {
        return Pic;
    }

    public void setEnablePic(boolean enablePic) {
        EnablePic = enablePic;
    }

    public void setPic(PosOBJ pic) {
        Pic = pic;
    }

    public boolean isEnableInfo() {
        return EnableInfo;
    }

    public void setEnableInfo(boolean enableInfo) {
        EnableInfo = enableInfo;
    }

    public boolean isEnableLyric() {
        return EnableLyric;
    }

    public void setEnableLyric(boolean enableLyric) {
        EnableLyric = enableLyric;
    }

    public boolean isEnableList() {
        return EnableList;
    }

    public void setEnableList(boolean enableList) {
        EnableList = enableList;
    }

    public PosOBJ getInfo() {
        return Info;
    }

    public void setInfo(PosOBJ info) {
        Info = info;
    }

    public PosOBJ getList() {
        return List;
    }

    public void setList(PosOBJ list) {
        List = list;
    }

    public PosOBJ getLyric() {
        return Lyric;
    }

    public void setLyric(PosOBJ lyric) {
        Lyric = lyric;
    }
}