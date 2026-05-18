package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.render.TextFrameBuffer;
import com.coloryr.allmusic.client.core.GLTexture;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public class AllMusicClient implements ClientModInitializer, AllMusicBridge {
    public static final ResourceLocation ID = new ResourceLocation("allmusic", "channel");

    public static final Logger LOGGER = LogManager.getLogger("AllMusic Client");

    private static final PoseStack stack = new PoseStack();

    @Override
    public Object genTexture(int size) {
        return GLTexture.genGLTexture(size);
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        GLTexture.updateGLTexture((int) tex, size, byteBuffer);
    }

    public int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public int getTextWidth(String item) {
        return Minecraft.getInstance().font.width(item);
    }

    public int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public void drawText(String item, int color, boolean shadow) {
        Gui hud = Minecraft.getInstance().gui;
        Font textRenderer = hud.getFont();
        Component component = MiniMessage.parse(item);
        if (shadow) {
            textRenderer.drawShadow(stack, component, 0, 0, color);
        } else {
            textRenderer.draw(stack, component, 0, 0, color);
        }
    }

    public void drawPic(Object texture, int size, int x, int y, int ang, float alpha) {
        GlStateManager._bindTexture((int) texture);
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_LESS);
        RenderSystem.enableBlend();
        RenderSystem.depthFunc(GL30.GL_ALWAYS);

        Matrix4f matrix;

        int a = size / 2;

        matrix = Matrix4f.createTranslateMatrix(x + a, y + a, 0);
        if (ang > 0) {
            matrix.multiply(new Quaternion(0, 0, ang, true));
        }

        int x0 = -a;
        int x1 = a;
        int y0 = -a;
        int y1 = a;
        int z = 0;
        int u0 = 0;
        float u1 = 1;
        float v0 = 0;
        float v1 = 1;

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).color(1.0f, 1.0f, 1.0f, alpha).uv(u0, v0).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    public void sendMessage(String data) {
        data = "[AllMusic Client]" + data;
        LOGGER.warn(data);
        String finalData = data;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null)
                return;
            Minecraft.getInstance().player.chat(finalData);
        });
    }

    public float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    @Override
    public void stopPlayMusic() {
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
    }

    @Override
    public TextFrameBuffer makeTextRender() {
        return new CoreRenderTarget();
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buffer, responseSender) -> {
            try {
                AllMusicCore.packRead(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AllMusicCore.init(FabricLoader.getInstance().getConfigDir(), this);
        RenderSystem.recordRenderCall(AllMusicCore::glInit);
    }
}