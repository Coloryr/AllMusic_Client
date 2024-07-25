package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.hud.ComType;
import com.coloryr.allmusic.client.hud.HudUtils;
import com.coloryr.allmusic.client.player.APlayer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AllMusic implements ModInitializer {
    public static final Identifier ID = new Identifier("allmusic", "channel");
    public static APlayer nowPlaying;
    public static HudUtils hudUtils;

    public static void onServerQuit() {
        try {
            stopPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        hudUtils.close();
        hudUtils.save = null;
    }

    private static void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    private static final MatrixStack stack = new MatrixStack();

    public static int getScreenWidth() {
        return MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    public static int getScreenHeight() {
        return MinecraftClient.getInstance().getWindow().getScaledHeight();
    }

    public static int getTextWidth(String item) {
        return MinecraftClient.getInstance().textRenderer.getWidth(item);
    }

    public static int getFontHeight() {
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    public static void drawText(String item, int x, int y, int color, boolean shadow) {
        InGameHud hud = MinecraftClient.getInstance().inGameHud;
        TextRenderer textRenderer = hud.getFontRenderer();
        if (shadow) {
            textRenderer.drawWithShadow(stack, item, x, y, color);
        } else {
            textRenderer.draw(stack, item, x, y, color);
        }
    }

    public static void drawPic(int textureID, int size, int x, int y, int ang) {
        GlStateManager.bindTexture(textureID);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableAlphaTest();

        Matrix4f matrix;

        int a = size / 2;

        matrix = Matrix4f.translate(x + a, y + a, 0);
        if(ang > 0) {
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

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, (float)x0, (float)y1, (float)z).texture(u0, v1).next();
        bufferBuilder.vertex(matrix, (float)x1, (float)y1, (float)z).texture(u1, v1).next();
        bufferBuilder.vertex(matrix, (float)x1, (float)y0, (float)z).texture(u1, v0).next();
        bufferBuilder.vertex(matrix, (float)x0, (float)y0, (float)z).texture(u0, v0).next();
        bufferBuilder.end();
        RenderSystem.enableAlphaTest();
        BufferRenderer.draw(bufferBuilder);
    }

    public static void sendMessage(String data){
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player == null)
                return;
            MinecraftClient.getInstance().player.sendChatMessage(data);
        });
    }

    public static void runMain(Runnable runnable) {
        RenderSystem.recordRenderCall(runnable::run);
    }

    public static float getVolume(){
        return MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS);
    }

    @Override
    public void onInitialize() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buffer, responseSender) -> {
            try {
                byte type = buffer.readByte();
                if (type >= HudUtils.types.length || type < 0) {
                    return;
                }
                ComType type1 = ComType.values()[type];
                switch (type1) {
                    case lyric -> hudUtils.lyric = readString(buffer);
                    case info -> hudUtils.info = readString(buffer);
                    case list -> hudUtils.list = readString(buffer);
                    case play -> {
                        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
                        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
                        stopPlaying();
                        nowPlaying.setMusic(readString(buffer));
                    }
                    case img -> hudUtils.setImg(readString(buffer));
                    case stop -> stopPlaying();
                    case clear -> hudUtils.close();
                    case pos -> nowPlaying.set(buffer.readInt());
                    case hud -> hudUtils.setPos(readString(buffer));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        hudUtils = new HudUtils(FabricLoader.getInstance().getConfigDir());
        nowPlaying = new APlayer();
    }

    private static String readString(PacketByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }
}