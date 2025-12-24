package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AllMusic implements ClientModInitializer, AllMusicBridge {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("allmusic", "channel");

    private static GuiGraphics context;

    public static final Logger LOGGER = LogManager.getLogger("AllMusic Client");

    public static class Tex extends AbstractTexture {
        public Tex(GpuTexture tex, GpuTextureView view) {
            this.texture = tex;
            this.textureView = view;
        }
    }

    public Object genTexture(int size) {
        var device = RenderSystem.getDevice();
        var tex = device.createTexture("allmusic:gui_textured", 5, TextureFormat.RGBA8, size, size, 1, 1);
//        tex.setTextureFilter(FilterMode.NEAREST, false);

        var view = device.createTextureView(tex);

        Tex tex1 = new Tex(tex, view);

        Minecraft.getInstance().getTextureManager().register(ID, tex1);

        return tex;
    }

    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        if (tex instanceof GlTexture tex1) {
            AllMusicCore.updateGLTexture(tex1.glId(), size, byteBuffer);
        }
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
        context.drawString(hud, item, x, y, color, shadow);
    }

    public void drawPic(Object texture, int size, int x, int y, int ang) {
        Matrix3x2fStack stack = context.pose();
        Matrix3x2fStack matrix = stack.pushMatrix();

        int a = size / 2;

        if (ang > 0) {
            matrix.translation(x + a, y + a);
            matrix.pushMatrix().rotate((float) Math.toRadians(ang));
        } else {
            matrix.translation(x + a, y + a);
        }

        context.blit(RenderPipelines.GUI_TEXTURED, ID, -a, -a, 0, 0, size, size, size, size, size, size);
        stack.popMatrix();
        if (ang > 0) {
            stack.popMatrix();
        }
    }

    public void sendMessage(String data) {
        data = "[AllMusic Client]" + data;
        LOGGER.warn(data);
        String finalData = data;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null)
                return;
            Minecraft.getInstance().player.displayClientMessage(Component.literal(finalData), false);
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

    public static void update(GuiGraphics draw) {
        context = draw;
        AllMusicCore.hudUpdate();
    }

    private static String readString(FriendlyByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    public record PackPayload() implements CustomPacketPayload {
        public static final Type<PackPayload> ID = new CustomPacketPayload.Type<>(AllMusic.ID);
        public static final StreamCodec<FriendlyByteBuf, PackPayload> CODEC = StreamCodec.of((value, buf) -> {
        }, buffer -> {
            AllMusicCore.packRead(buffer);
            return new PackPayload();
        });

        @Override
        public @NonNull Type<? extends CustomPacketPayload> type() {
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
