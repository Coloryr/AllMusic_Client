package coloryr.allmusic.hud;

public class SaveOBJ {
    public PosOBJ List;
    public PosOBJ Lyric;
    public PosOBJ Info;
    public PosOBJ Pic;
    public int PicSize;
    public int PicRotateSpeed;
    public boolean EnablePicRotate;
    public boolean EnableList;
    public boolean EnableLyric;
    public boolean EnableInfo;
    public boolean EnablePic;

    public SaveOBJ() {
        EnableList = true;
        EnableLyric = true;
        EnableInfo = true;
        EnablePic = true;
        EnablePicRotate = true;
        PicRotateSpeed = 10;
        List = new PosOBJ();
        Lyric = new PosOBJ();
        Info = new PosOBJ();
        Pic = new PosOBJ();
        PicSize = 70;
    }
}