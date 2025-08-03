package com.coloryr.allmusic.client.core;

import com.coloryr.allmusic.client.core.hud.AllMusicHud;
import com.coloryr.allmusic.client.core.objs.SaveOBJ;
import com.coloryr.allmusic.client.core.objs.ConfigObj;
import com.coloryr.allmusic.client.core.player.AllMusicPlayer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * AllMusic核心
 */
public class AllMusicCore {
    public static final CommandType[] types = CommandType.values();

    private static final Gson gson = new Gson();

    /**
     * 音频解码器与播放器
     */
    private static AllMusicPlayer player;
    /**
     * 界面显示内容
     */
    private static AllMusicHud hud;
    /**
     * 与游戏链接的桥
     */
    public static AllMusicBridge bridge;

    /**
     * 配置文件
     */
    public static ConfigObj config;

    /**
     * 更新音频缓存
     */
    public static void tick() {
        if (player != null) {
            player.tick();
        }
    }

    /**
     * 是否正在播放音乐
     * @return 是否在播放
     */
    public static boolean isPlay() {
        if (player == null) {
            return false;
        }
        return player.isPlay();
    }

    /**
     * 初始化核心
     * @param file 配置文件
     * @param bridge 游戏桥
     */
    public static void init(Path file, AllMusicBridge bridge) {
//        File configFile = new File(file.toFile(), "allmusic.json");
//        if (configFile.exists()) {
//            try {
//                InputStreamReader reader = new InputStreamReader(
//                        Files.newInputStream(configFile.toPath()),
//                        StandardCharsets.UTF_8);
//                BufferedReader bf = new BufferedReader(reader);
//                config = new Gson().fromJson(bf, ConfigObj.class);
//                bf.close();
//                reader.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        if (config == null) {
//            config = new ConfigObj();
//            config.picSize = 200;
//            config.queueSize = 100;
//            config.exitSize = 50;
//            try {
//                String data = new GsonBuilder().setPrettyPrinting()
//                        .create()
//                        .toJson(config);
//                FileOutputStream out = new FileOutputStream(configFile);
//                OutputStreamWriter write = new OutputStreamWriter(out, StandardCharsets.UTF_8);
//                write.write(data);
//                write.close();
//                out.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

        AllMusicCore.bridge = bridge;
        player = new AllMusicPlayer();
    }

    /**
     * 贴图初始化
     */
    public static void glInit() {
        hud = new AllMusicHud(200);
    }

    /**
     * 退出服务器时
     */
    public static void onServerQuit() {
        try {
            stopPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        hud.save = null;
    }

    /**
     * 停止播放
     */
    private static void stopPlaying() {
        player.closePlayer();
        hud.close();
    }

    /**
     * 重载音频
     */
    public static void reload() {
        if (player != null) {
            player.setReload();
        }
    }

    /**
     * 更新显示内容
     */
    public static void hudUpdate() {
        hud.update();
    }

    /**
     * 从数据包中读文字
     * @param buf 数据包
     * @return 文字
     */
    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    /**
     * 读取数据包
     * @param buffer 数据包
     */
    public static void packRead(ByteBuf buffer) {
        byte type = buffer.readByte();
        if (type >= types.length || type < 0) {
            return;
        }
        CommandType type1 = CommandType.values()[type];
        String data = null;
        int data1 = 0;
        switch (type1) {
            case LYRIC:
            case INFO:
            case LIST:
            case PLAY:
            case IMG:
            case HUD_DATA:
                data = readString(buffer);
                break;
            case POS:
                data1 = buffer.readInt();
                break;
        }
        if (type1 == CommandType.PLAY) {
            bridge.stopPlayMusic();
        }
        hudState(type1, data, data1);
        buffer.clear();
    }

    /**
     * 解析数据包
     * @param type 命令
     * @param data 数据
     * @param data1 数据
     */
    public static void hudState(CommandType type, String data, int data1) {
        switch (type) {
            case LYRIC:
                hud.lyric = data;
                break;
            case INFO:
                hud.info = data;
                break;
            case LIST:
                hud.list = data;
                break;
            case PLAY:
                stopPlaying();
                player.setMusic(data);
                break;
            case IMG:
                hud.setImg(data);
                break;
            case STOP:
                stopPlaying();
                break;
            case CLEAR:
                hud.close();
                break;
            case POS:
                player.set(data1);
                break;
            case HUD_DATA:
                hud.setPos(gson.fromJson(data, SaveOBJ.class));
                break;
        }
    }

    /**
     * 创建GL材质
     * @param size 大小
     * @return 材质号
     */
    public static int genGLTexture(int size) {
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

    /**
     * 更新GL材质
     * @param id 材质id
     * @param size 大小
     * @param byteBuffer 数据
     */
    public static void updateGLTexture(int id, int size, ByteBuffer byteBuffer) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, size,
                size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
