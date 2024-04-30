package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.hud.ComType;
import com.coloryr.allmusic.client.hud.HudUtils;
import com.coloryr.allmusic.client.player.APlayer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.client.event.sound.SoundEvent;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.nio.charset.StandardCharsets;

@Mod("allmusic_client")
public class AllMusic implements IPlayPayloadHandler<PackData> {
    private static APlayer nowPlaying;
    private static HudUtils hudUtils;
    private static GuiGraphics gui;

    public static final ResourceLocation channel =
            new ResourceLocation("allmusic", "channel");

    public AllMusic(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);
        modEventBus.addListener(this::onLoad);
        modEventBus.addListener(this::register);

        NeoForge.EVENT_BUS.register(this);
    }

    public static void sendMessage(String data) {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal(data)));
    }

    private void setup(final FMLClientSetupEvent event) {
        hudUtils = new HudUtils(FMLPaths.CONFIGDIR.get());
    }

    public void register(final RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar("allmusic");
        registrar.optional().play(channel, new DataReader(), handler -> handler
                .client(this));
    }

    @Override
    public void handle(@NotNull PackData payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> handle(payload.buffer()));
    }

    public static class DataReader implements FriendlyByteBuf.Reader<PackData> {
        @Override
        public PackData apply(FriendlyByteBuf buf) {
            return new PackData(buf);
        }
    }

    public void handle(ByteBuf buffer) {
        try {
            byte type = buffer.readByte();
            if (type >= HudUtils.types.length || type < 0) {
                return;
            }
            ComType type1 = ComType.values()[type];
            switch (type1) {
                case lyric:
                    hudUtils.lyric = readString(buffer);
                    break;
                case info:
                    hudUtils.info = readString(buffer);
                    break;
                case list:
                    hudUtils.list = readString(buffer);
                    break;
                case play:
                    Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
                    Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
                    stopPlaying();
                    nowPlaying.setMusic(readString(buffer));
                    break;
                case img:
                    hudUtils.setImg(readString(buffer));
                    break;
                case stop:
                    stopPlaying();
                    break;
                case clear:
                    hudUtils.close();
                    break;
                case pos:
                    nowPlaying.set(buffer.readInt());
                    break;
                case hud:
                    hudUtils.setPos(readString(buffer));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    private void setup1(final FMLCommonSetupEvent event) {
        nowPlaying = new APlayer();
    }

    public static int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static int getTextWidth(String item) {
        return Minecraft.getInstance().font.width(item);
    }

    public static int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public void onLoad(final SoundEngineLoadEvent e) {
        if (nowPlaying != null) {
            nowPlaying.setReload();
        }
    }

    @SubscribeEvent
    public void onSound(final PlayStreamingSourceEvent e) {
        if (!nowPlaying.isPlay())
            return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onSound(final PlaySoundSourceEvent e) {
        if (!nowPlaying.isPlay())
            return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggingOut e) {
        try {
            stopPlaying();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        hudUtils.close();
        hudUtils.save = null;
    }

    public static float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    public static void drawPic(int textureID, int size, int x, int y, int ang) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureID);

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
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).uv(u0, v0).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        //GuiComponent.blit(stack, x, y, 0, 0, 0, size, size, size, size);
    }

    public static void drawText(String item, int x, int y, int color, boolean shadow) {
        var hud = Minecraft.getInstance().font;
        gui.drawString(hud, item, x, y, color, shadow);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post e) {
        if (e.getOverlay().id() == VanillaGuiOverlay.PORTAL.id()) {
            gui = e.getGuiGraphics();
            hudUtils.update();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        nowPlaying.tick();
    }

    private void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    public static void runMain(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }
}
