package com.coloryr.allmusic.client.core.render;

import java.util.ArrayList;
import java.util.List;

public abstract class TextFrameBuffer {

    public static class TextItem {
        public final int width;
        public final int height;
        public final int y;
        public final float scale;

        public TextItem(int width, int height, int y, float scale) {
            this.width = width;
            this.height = height;
            this.y = y;
            this.scale = scale;
        }
    }

    protected final List<TextItem> texts = new ArrayList<>();

    protected int nowWidth, nowHeight;

    public abstract void use();
    public abstract void unUse();
    public abstract void resize(int width, int height);
    public abstract void drawText(String text, int y, int color, boolean shadow);
    public abstract void draw(float alpha, int x, int y, int maxWidth);
    public abstract void drawWithState(float alpha, int x, int y, int maxWidth, float state);
    public abstract void tick();
}
