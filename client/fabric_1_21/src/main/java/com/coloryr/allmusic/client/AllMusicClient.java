package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.comm.MusicCodec;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.nio.ByteBuffer;

public class AllMusicClient implements ClientModInitializer, AllMusicBridge {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("allmusic", "channel");
    public static final Logger LOGGER = LogManager.getLogger("AllMusic Client");
    private static GuiGraphics context;

    public static void update(GuiGraphics draw) {
        context = draw;
        AllMusicCore.hudUpdate();
    }

    @Override
    public Object genTexture(int size) {
        return AllMusicCore.genGLTexture(size);
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        AllMusicCore.updateGLTexture((int) tex, size, byteBuffer);
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

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        var hud = Minecraft.getInstance().font;
        Component component = MiniMessage.parse(item);
        context.drawString(hud, component, x, y, color, shadow);
    }

    public void drawPic(Object texture, int size, int x, int y, int ang) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, (int) texture);

        PoseStack stack = new PoseStack();
        Matrix4f matrix = stack.last().pose();

        int a = size / 2;

        if (ang > 0) {
            matrix = matrix.translationRotate(x + a, y + a, 0,
                    new Quaternionf().fromAxisAngleDeg(0, 0, 1, ang));
        } else {
            matrix = matrix.translation(x + a, y + a, 0);
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
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, (float) x0, (float) y1, (float) z).setUv(u0, v1);
        bufferBuilder.addVertex(matrix, (float) x1, (float) y1, (float) z).setUv(u1, v1);
        bufferBuilder.addVertex(matrix, (float) x1, (float) y0, (float) z).setUv(u1, v0);
        bufferBuilder.addVertex(matrix, (float) x0, (float) y0, (float) z).setUv(u0, v0);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    public void sendMessage(String data) {
        data = "[AllMusic Client]" + data;
        LOGGER.warn(data);
        String finalData = data;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null)
                return;
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(finalData));
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
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MusicCodec.ID, (pack, handler) -> {
            try {
                AllMusicCore.packDo(pack.pack().type, pack.pack().data, pack.pack().data1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AllMusicCore.init(FabricLoader.getInstance().getConfigDir(), this);
        RenderSystem.recordRenderCall(() -> AllMusicCore.glInit());
    }
}
