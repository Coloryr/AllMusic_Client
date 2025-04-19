package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.hud.AllMusicBridge;
import com.coloryr.allmusic.client.hud.AllMusicHelper;
import com.coloryr.allmusic.client.hud.ComType;
import com.coloryr.allmusic.client.hud.HudUtils;
import com.coloryr.allmusic.client.mixin.IGuiGetter;
import com.coloryr.allmusic.client.player.APlayer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@OnlyIn(Dist.CLIENT)
@Mod("allmusic_client")
public class AllMusic implements LayeredDraw.Layer, AllMusicBridge {
    private static GuiGraphics gui;
    private static final ResourceLocation channel = ResourceLocation.fromNamespaceAndPath("allmusic", "channel");

    public AllMusic() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);
        modEventBus.addListener(this::onLoad);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.getChat().addMessage(Component.literal(data)));
    }

    private void setup(final FMLClientSetupEvent event) {
        AllMusicHelper.hudInit(FMLPaths.CONFIGDIR.get());
        try {
            Class parcleClass = Class.forName("com.coloryr.allmusic.server.AllMusicForge");
            Field m = parcleClass.getField("channel1");
            SimpleChannel channel = (SimpleChannel) m.get(null);
            channel.messageBuilder(FriendlyByteBuf.class)
                    .decoder(this::decode)
                    .encoder(this::encode)
                    .consumerNetworkThread(this::handle1)
                    .add();
        } catch (Exception e) {
            ChannelBuilder.named(channel)
                    .networkProtocolVersion(0)
                    .optional()
                    .clientAcceptedVersions(((status, i) -> true))
                    .serverAcceptedVersions(((status, i) -> true))
                    .eventNetworkChannel()
                    .addListener((data) -> handle(data.getPayload()));
        }
        var get = (IGuiGetter) (Minecraft.getInstance().gui);
        get.getLayers().add(this);
    }

    public void encode(FriendlyByteBuf msg, FriendlyByteBuf buf) {

    }

    public FriendlyByteBuf decode(FriendlyByteBuf buf) {
        return buf;
    }

    public void handle1(FriendlyByteBuf buffer, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> handle(buffer));
        ctx.setPacketHandled(true);
    }

    public void handle(FriendlyByteBuf buffer) {
        try {
            byte type = buffer.readByte();
            if (type >= HudUtils.types.length || type < 0) {
                return;
            }
            ComType type1 = ComType.values()[type];
            String data = null;
            int data1 = 0;
            switch (type1) {
                case lyric:
                case info:
                case list:
                case play:
                case img:
                case hud:
                    data = readString(buffer);
                    break;
                case pos:
                    data1 = buffer.readInt();
                    break;
            }
            if (type1 == ComType.play) {
                Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
                Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
            }
            AllMusicHelper.hudState(type1, data, data1);
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

    private void setup1(final FMLLoadCompleteEvent event) {
        AllMusicHelper.init(this);
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

    public void onLoad(final SoundEngineLoadEvent e) {
        AllMusicHelper.reload();
    }

    @SubscribeEvent
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!AllMusicHelper.isPlay()) return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggingOut e) {
        AllMusicHelper.onServerQuit();
    }

    public float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, (int)textureID);

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

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        var hud = Minecraft.getInstance().font;
        gui.drawString(hud, item, x, y, color, shadow);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        AllMusicHelper.tick();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        if (!Minecraft.getInstance().options.hideGui) {
            gui = guiGraphics;
            AllMusicHelper.hudUpdate();
        }
    }

    @Override
    public Object genTexture(int size) {
        return AllMusicHelper.gen(size);
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        AllMusicHelper.update((int) tex, size, byteBuffer);
    }
}
