package com.coloryr.allmusic.client.core;

import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class GLTexture {
    /**
     * 创建GL材质
     *
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
     *
     * @param id         材质id
     * @param size       大小
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
