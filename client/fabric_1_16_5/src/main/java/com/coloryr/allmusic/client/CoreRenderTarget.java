package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.render.TextFrameBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class CoreRenderTarget extends TextFrameBuffer {
    private final RenderTarget target;

    public CoreRenderTarget() {
        target = new RenderTarget(200, 20, false, false);
        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void resize(int width, int height) {
        if (width > target.width || height > target.height) {
            target.resize(width, height, false);
        }
    }

    @Override
    public void use() {
        target.bindWrite(true);
        target.clear(false);
    }

    @Override
    public void unUse() {
        target.unbindWrite();
    }

    @Override
    public void draw(float alpha, int x, int y, int width, int height) {
        RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
        rendertarget.bindWrite(true);

        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        GlStateManager._colorMask(true, true, true, false);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._matrixMode(5889);
        GlStateManager._loadIdentity();
        GlStateManager._ortho(0.0F, rendertarget.width, rendertarget.height, 0.0F, 1000.0F, 3000.0F);
        GlStateManager._matrixMode(5888);
        GlStateManager._loadIdentity();
        GlStateManager._translatef(0.0F, 0.0F, -2000.0F);
        GlStateManager._viewport(0, 0, rendertarget.width, rendertarget.height);
        GlStateManager._enableTexture();
        GlStateManager._disableLighting();
        GlStateManager._disableAlphaTest();

        // 混合
        if (alpha < 1.0F) {
            GlStateManager._enableBlend();
            GlStateManager._blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GlStateManager._disableBlend();
        }

        GlStateManager._color4f(1, 1, 1, alpha);

        // 绑定纹理并计算UV
        target.bindRead();
        float u0 = (float) 0;
        float v0 = (float) 0;
        float u1 = (float) width / target.width;
        float v1 = (float) height / target.height;

        // 绘制
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buffer.vertex(0, v0, 0).uv(u0, v0).color(255, 255, 255, 255).endVertex();
        buffer.vertex(u0, v0, 0).uv(u1, v0).color(255, 255, 255, 255).endVertex();
        buffer.vertex(u0, 0, 0).uv(u1, v1).color(255, 255, 255, 255).endVertex();
        buffer.vertex(0, 0, 0).uv(u0, v1).color(255, 255, 255, 255).endVertex();

        tessellator.end();
        target.unbindRead();

        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
    }
}
