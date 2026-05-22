package com.coloryr.allmusic.client.core.render;

public abstract class PictureFrameBuffer {
    /**
     * 更新图片
     * @param source 原始
     * @param rotate 圆形
     */
    public abstract void updatePic(byte[] source, byte[] rotate);
    /**
     * 绘制图片
     *
     * @param rotate 是否为旋转模式
     * @param size    大小
     * @param x       X坐标
     * @param y       Y坐标
     * @param ang     旋转角度
     * @param alpha   透明度
     */
    public abstract void drawPic(boolean rotate, int size, float x, float y, int ang, float alpha);
}
