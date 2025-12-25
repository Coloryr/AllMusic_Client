package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.CommandType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class AllMusic implements ClientModInitializer, AllMusicBridge {
    public static final Identifier ID = Identifier.of("allmusic", "channel");

    private static DrawContext context;

    private static Function<Identifier, RenderLayer> GUI_TEXTURED;

    public static final Logger LOGGER = LogManager.getLogger("AllMusic Client");

    public static class Tex extends GlTexture {
        protected Tex(int id, int width, int height) {
            super("allmusic", TextureFormat.RGBA8, width, height, 0, id);
        }
    }

    public static class Texture extends RenderPhase.TextureBase {
        public Texture(Tex tex) {
            super(() -> RenderSystem.setShaderTexture(0, tex), () -> {
            });
        }
    }

    public Object genTexture(int size) {
        int textureID = AllMusicCore.genGLTexture(size);

        Tex tex = new Tex(textureID, size, size);
        tex.setTextureFilter(FilterMode.NEAREST, false);

        GUI_TEXTURED = Util.memoize((texture) -> RenderLayer.of("allmusic_gui_textured", 786432, RenderPipelines.GUI_TEXTURED, RenderLayer.MultiPhaseParameters.builder().texture(new Texture(tex)).build(false)));

        return tex;
    }

    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        if (tex instanceof Tex tex1) {
            AllMusicCore.updateGLTexture(tex1.getGlId(), size, byteBuffer);
        }
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
        MatrixStack stack = context.getMatrices();
        stack.push();
        Matrix4f matrix = stack.peek().getPositionMatrix();

        int a = size / 2;

        if (ang > 0) {
            matrix.translationRotate(x + a, y + a, 0,
                    new Quaternionf().fromAxisAngleDeg(0, 0, 1, ang));
        } else {
            matrix.translation(x + a, y + a, 0);
        }

        context.drawTexture(GUI_TEXTURED, ID, -a, -a, 0, 0, size, size, size, size, size, size);
        stack.pop();
    }

    public void sendMessage(String data) {
        data = "[AllMusic Client]" + data;
        LOGGER.warn(data);
        String finalData = data;
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player == null)
                return;
            MinecraftClient.getInstance().player.sendMessage(Text.of(finalData), false);
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
        public static final PacketCodec<PacketByteBuf, PackPayload> CODEC = PacketCodec.of((value, buf) -> {
        }, buffer -> {
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
                AllMusicCore.packDo(pack.type, pack.data, pack.data1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AllMusicCore.init(FabricLoader.getInstance().getConfigDir(), this);
    }
}
