package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.render.TextFrameBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL30;

public class CoreRenderTarget extends TextFrameBuffer {
    private static final RenderBuffers renderBuffers = new RenderBuffers();

    private final RenderTarget target;
    private Matrix4f matrix4f = new Matrix4f();

    private int nowWidth, nowHeight;

    public CoreRenderTarget() {
        target = new TextureTarget(800, 200, false, false);
        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void resize(int width, int height) {
        Window window = Minecraft.getInstance().getWindow();

        nowWidth = (int) (width * window.getGuiScale());
        nowHeight = (int) (height * window.getGuiScale());

        if (nowWidth > target.width || nowHeight > target.height) {
            target.resize(nowWidth, nowHeight, false);
        }

        matrix4f = new Matrix4f().setOrtho(0.0F, (float) (target.width / window.getGuiScale()),
                (float)(target.height / window.getGuiScale()), 0.0F, 1000.0F, 21000.0F);
    }

    @Override
    public void use() {
        target.clear(false);
        target.bindWrite(true);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
    }

    @Override
    public void unUse() {
        RenderSystem.restoreProjectionMatrix();

        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
    }

    @Override
    public void drawText(String text, int y, int color, boolean shadow) {
        var font = Minecraft.getInstance().font;
        Component component = MiniMessage.parse(text);
        font.drawInBatch(component, 0, y, color, shadow, new Matrix4f(), renderBuffers.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        RenderSystem.disableDepthTest();
        renderBuffers.bufferSource().endBatch();
        RenderSystem.enableDepthTest();
    }

    @Override
    public void draw(float alpha, int x, int y, int width, int height) {
        alpha = 1.0f;

        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.depthFunc(GL30.GL_ALWAYS);

        Matrix4f matrix = new Matrix4f().translation(x, y, 0);

        var w = target.width / 2;
        var h = target.height / 2;

        int x0 = -w;
        int x1 = w;
        int y0 = -h;
        int y1 = h;
        int z = 0;
        int u0 = 0;
        float u1 = 1;
        float v0 = 0;
        float v1 = 1;

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u0, v0).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
        target.unbindRead();
    }
}
