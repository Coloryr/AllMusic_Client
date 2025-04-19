package com.coloryr.allmusic.client.hud;

import java.nio.ByteBuffer;

public interface AllMusicBridge {
    Object genTexture(int size);
    int getScreenWidth();
    int getScreenHeight();
    int getTextWidth(String item);
    int getFontHeight();
    void updateTexture(Object tex, int size, ByteBuffer byteBuffer);
    void drawText(String item, int x, int y, int color, boolean shadow);
    void drawPic(Object texture, int size, int x, int y, int ang);
    void sendMessage(String data);
    float getVolume();
}
