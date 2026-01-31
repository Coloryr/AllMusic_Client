package com.coloryr.allmusic.client.core;

import java.nio.ByteBuffer;

/**
 * AllMusic 核心桥
 */
public interface AllMusicBridge {
    /**
     * 获取一个渲染材质
     *
     * @param size 材质大小
     * @return 材质
     */
    Object genTexture(int size);

    /**
     * 获取屏幕宽度
     *
     * @return 屏幕宽度
     */
    int getScreenWidth();

    /**
     * 获取屏幕高度
     *
     * @return 屏幕高度
     */
    int getScreenHeight();

    /**
     * 获取文字宽度
     *
     * @param item 文字内容
     * @return 宽度
     */
    int getTextWidth(String item);

    /**
     * 获取字体高度
     *
     * @return 字体高度
     */
    int getFontHeight();

    /**
     * 更新材质内容
     *
     * @param tex        材质
     * @param size       贴图大小
     * @param byteBuffer 贴图内容
     */
    void updateTexture(Object tex, int size, ByteBuffer byteBuffer);

    /**
     * 绘制文字
     *
     * @param item   文字内容
     * @param x      X坐标
     * @param y      Y坐标
     * @param color  显示颜色
     * @param shadow 是否有阴影
     */
    void drawText(String item, int x, int y, int color, boolean shadow);

    /**
     * 绘制图片
     *
     * @param texture 材质
     * @param size    大小
     * @param x       X坐标
     * @param y       Y坐标
     * @param ang     旋转角度
     */
    void drawPic(Object texture, int size, int x, int y, int ang);

    /**
     * 显示消息
     *
     * @param data 显示内容
     */
    void sendMessage(String data);

    /**
     * 获取当前音量
     *
     * @return 音量
     */
    float getVolume();

    /**
     * 停止播放其他音频
     */
    void stopPlayMusic();
}
