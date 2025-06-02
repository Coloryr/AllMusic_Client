package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;

import java.nio.ByteBuffer;

public class AllMusic implements ClientModInitializer, AllMusicBridge {
    public static final Identifier ID = new Identifier("allmusic", "channel");

    private static final MatrixStack stack = new MatrixStack();

    @Override
    public Object genTexture(int size) {
        return AllMusicCore.genGLTexture(size);
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        AllMusicCore.updateGLTexture((int) tex, size, byteBuffer);
    }

    public int getScreenWidth() {
        return MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    public int getScreenHeight() {
        return MinecraftClient.getInstance().getWindow().getScaledHeight();
    }

    public int getTextWidth(String item) {
        return MinecraftClient.getInstance().textRenderer.getWidth(item);
    }

    public int getFontHeight() {
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        var hud = MinecraftClient.getInstance().inGameHud;
        var textRenderer = hud.getTextRenderer();
        if (shadow) {
            textRenderer.drawWithShadow(stack, item, x, y, color);
        } else {
            textRenderer.draw(stack, item, x, y, color);
        }
    }

    public void drawPic(Object texture, int size, int x, int y, int ang) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, (int) texture);

        MatrixStack stack = new MatrixStack();
        Matrix4f matrix = stack.peek().getModel();

        int a = size / 2;

        matrix.multiplyByTranslation(x + a, y + a, 0);
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

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).texture(u0, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).texture(u1, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).texture(u1, v0).next();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).texture(u0, v0).next();
        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);
    }

    public void sendMessage(String data) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(data));
        });
    }

    public float getVolume() {
        return MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS);
    }

    @Override
    public void stopPlayMusic() {
        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
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