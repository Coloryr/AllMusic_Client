package com.coloryr.allmusic.client.hud;

import com.coloryr.allmusic.client.player.APlayer;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class AllMusicHelper {
    private static APlayer nowPlaying;
    private static HudUtils hudUtils;
    public static AllMusicBridge INSTANCE;

    public static void tick() {
        if (nowPlaying != null) {
            nowPlaying.tick();
        }
    }

    public static boolean isPlay() {
        if (nowPlaying == null) {
            return false;
        }
        return nowPlaying.isPlay();
    }

    public static void init(AllMusicBridge bridge) {
        INSTANCE = bridge;
        nowPlaying = new APlayer();
    }

    public static void hudInit(Path file) {
        hudUtils = new HudUtils(file);
    }

    public static void onServerQuit() {
        try {
            stopPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        hudUtils.save = null;
    }

    private static void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    public static void reload() {
        if (nowPlaying != null) {
            nowPlaying.setReload();
        }
    }

    public static void hudUpdate()
    {
        hudUtils.update();
    }

    public static void hudState(ComType type, String data, int data1)
    {
        switch (type) {
            case lyric:
                hudUtils.lyric = data;
                break;
            case info:
                hudUtils.info = data;
                break;
            case list:
                hudUtils.list = data;
                break;
            case play:
                stopPlaying();
                nowPlaying.setMusic(data);
                break;
            case img:
                hudUtils.setImg(data);
                break;
            case stop:
                stopPlaying();
                break;
            case clear:
                hudUtils.close();
                break;
            case pos:
                nowPlaying.set(data1);
                break;
            case hud:
                hudUtils.setPos(data);
                break;
        }
    }

    public static int gen(int size) {
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, size,
                size, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureID;
    }

    public static void update(int id, int size, ByteBuffer byteBuffer) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, size,
                size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
