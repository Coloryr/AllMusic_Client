package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.render.TextFrameBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public class CoreRenderTarget extends TextFrameBuffer {
    private static final RenderBuffers renderBuffers = new RenderBuffers();

    private final RenderTarget target;
    private Matrix4f matrix4f;

    private int nowWidth, nowHeight;

    public CoreRenderTarget() {
        target = new TextureTarget(800, 200, false, false);
        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        matrix4f = new Matrix4f().setOrtho(0.0F, target.width, target.height, 0.0F, 1000.0F, 3000.0F);
    }

    @Override
    public void resize(int width, int height) {
        Window window = Minecraft.getInstance().getWindow();

        nowWidth = (int) (width * window.getGuiScale());
        nowHeight = (int) (height * window.getGuiScale());

        if (nowWidth > target.width || nowHeight > target.height) {
            target.resize(nowWidth, nowHeight, false);
            matrix4f = new Matrix4f().setOrtho(0.0F, (float) (nowWidth / window.getGuiScale()), (float)(nowHeight / window.getGuiScale()), 0.0F, 1000.0F, 21000.0F);
        }
    }

    @Override
    public void drawText(String text, int y, int color, boolean shadow) {
        target.clear(false);
        target.bindWrite(true);
        Matrix4f prj = RenderSystem.getProjectionMatrix();

        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

        var font = Minecraft.getInstance().font;
        Component component = MiniMessage.parse(text);
        font.drawInBatch(component, 0, y, color, shadow, new Matrix4f(), renderBuffers.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        RenderSystem.disableDepthTest();

        renderBuffers.bufferSource().endBatch();
        RenderSystem.enableDepthTest();

        RenderSystem.setProjectionMatrix(prj, VertexSorting.ORTHOGRAPHIC_Z);

        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
    }

    @Override
    public void draw(float alpha, int x, int y, int width, int height) {
//        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
//        main.bindWrite(true);
//
//        RenderSystem.assertOnRenderThread();
//        GlStateManager._colorMask(true, true, true, false);
//        GlStateManager._disableDepthTest();
//        GlStateManager._depthMask(false);
//        GlStateManager._viewport(0, 0, main.width, main.height);
//
//        Minecraft minecraft = Minecraft.getInstance();
//        ShaderInstance shaderInstance = minecraft.gameRenderer.blitShader;
//        shaderInstance.setSampler("DiffuseSampler", target.getColorTextureId());
//        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, main.width, main.height, 0.0F, 1000.0F, 3000.0F);
//        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
//        if (shaderInstance.MODEL_VIEW_MATRIX != null) {
//            shaderInstance.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0F, 0.0F, -2000.0F));
//        }
//
//        if (shaderInstance.PROJECTION_MATRIX != null) {
//            shaderInstance.PROJECTION_MATRIX.set(matrix4f);
//        }
//
//        shaderInstance.apply();
//        float f = x;
//        float g = y;
//        float h = (float)width / target.width;
//        float k = (float)height / target.height;
//        Tesselator tesselator = RenderSystem.renderThreadTesselator();
//        BufferBuilder bufferBuilder = tesselator.getBuilder();
//        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
//        bufferBuilder.vertex(0.0, g, 0.0).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
//        bufferBuilder.vertex(f, g, 0.0).uv(h, 0.0F).color(255, 255, 255, 255).endVertex();
//        bufferBuilder.vertex(f, 0.0, 0.0).uv(h, k).color(255, 255, 255, 255).endVertex();
//        bufferBuilder.vertex(0.0, 0.0, 0.0).uv(0.0F, k).color(255, 255, 255, 255).endVertex();
//        BufferUploader.draw(bufferBuilder.end());
//        shaderInstance.clear();
//        GlStateManager._depthMask(true);
//        GlStateManager._colorMask(true, true, true, true);
    }
}
