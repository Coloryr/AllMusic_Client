package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AllMusic implements ClientModInitializer, AllMusicBridge {
    public static final Identifier ID = Identifier.of("allmusic", "channel");

    private static DrawContext context;

    public static class Tex extends AbstractTexture {
        public Tex(GpuTexture tex, GpuTextureView view) {
            this.glTexture = tex;
            this.glTextureView = view;
        }
    }

    public Object genTexture(int size) {
        var device = RenderSystem.getDevice();
        var tex = device.createTexture("allmusic:gui_textured", 5, TextureFormat.RGBA8, size, size, 1, 1);
        tex.setTextureFilter(FilterMode.NEAREST, false);

        var view = device.createTextureView(tex);

        Tex tex1 = new Tex(tex, view);

        MinecraftClient.getInstance().getTextureManager().registerTexture(ID, tex1);

        return tex;
    }

    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        if (tex instanceof GlTexture tex1) {
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
        Matrix3x2fStack stack = context.getMatrices();
        Matrix3x2fStack matrix = stack.pushMatrix();

        int a = size / 2;

        if (ang > 0) {
            matrix.translation(x + a, y + a);
            matrix.pushMatrix().rotate((float) Math.toRadians(ang));
        } else {
            matrix.translation(x + a, y + a);
        }

        context.drawTexture(RenderPipelines.GUI_TEXTURED, ID, -a, -a, 0, 0, size, size, size, size, size, size);
        stack.popMatrix();
        if (ang > 0) {
            stack.popMatrix();
        }
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

    public record PackPayload() implements CustomPayload {
        public static final Id<PackPayload> ID = new CustomPayload.Id<>(AllMusic.ID);
        public static final PacketCodec<PacketByteBuf, PackPayload> CODEC = PacketCodec.of((value, buf) -> {
        }, buffer -> {
            AllMusicCore.packRead(buffer);
            return new PackPayload();
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

        });

        AllMusicCore.init(FabricLoader.getInstance().getConfigDir(), this);
    }
}
