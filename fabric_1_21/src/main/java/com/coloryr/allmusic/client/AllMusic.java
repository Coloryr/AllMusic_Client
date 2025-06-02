package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.CommandType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AllMusic implements ClientModInitializer, AllMusicBridge {
    public static final Identifier ID = Identifier.of("allmusic", "channel");
    private static DrawContext context;

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
        var hud = MinecraftClient.getInstance().textRenderer;
        context.drawText(hud, item, x, y, color, shadow);
    }

    public void drawPic(Object texture, int size, int x, int y, int ang) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, (int) texture);

        MatrixStack stack = new MatrixStack();
        Matrix4f matrix = stack.peek().getPositionMatrix();

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

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).texture(u0, v1);
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).texture(u1, v1);
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).texture(u1, v0);
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).texture(u0, v0);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public void sendMessage(String data) {
        MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(data)));
    }

    public float getVolume() {
        return MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS);
    }

    @Override
    public void stopPlayMusic() {
        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
    }

    public static void update(DrawContext draw) {
        context = draw;
        AllMusicCore.hudUpdate();
    }

    private static String readString(PacketByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    public record PackPayload(CommandType type, String data, int data1) implements CustomPayload {
        public static final Id<PackPayload> ID = new CustomPayload.Id<>(AllMusic.ID);
        public static final PacketCodec<PacketByteBuf, PackPayload> CODEC = PacketCodec.of((value, buf) -> {}, buffer -> {
            byte type = buffer.readByte();
            if (type >= AllMusicCore.types.length || type < 0) {
                return null;
            }
            CommandType type1 = CommandType.values()[type];
            PackPayload payload = new PackPayload(CommandType.CLEAR, null, 0);
            switch (type1) {
                case LYRIC, INFO, LIST, PLAY, IMG, HUD_DATA -> payload = new PackPayload(type1, readString(buffer), 0);
                case STOP, CLEAR -> payload = new PackPayload(type1, null, 0);
                case POS -> payload = new PackPayload(type1, null, buffer.readInt());
            }
            buffer.clear();
            return payload;
        });

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(PackPayload.ID, PackPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(PackPayload.ID, (pack, handler) -> {
            try {
                AllMusicCore.hudState(pack.type, pack.data, pack.data1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AllMusicCore.init(FabricLoader.getInstance().getConfigDir(), this);
        RenderSystem.recordRenderCall(()-> AllMusicCore.glInit());
    }
}
