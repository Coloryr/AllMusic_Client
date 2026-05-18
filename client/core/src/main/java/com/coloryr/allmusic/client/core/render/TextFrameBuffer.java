package com.coloryr.allmusic.client.core.render;

public abstract class TextFrameBuffer {
    public abstract void resize(int width, int height);
    public abstract void drawText(String text, int y, int color, boolean shadow);
    public abstract void draw(float alpha, int x, int y, int width, int height);
}
