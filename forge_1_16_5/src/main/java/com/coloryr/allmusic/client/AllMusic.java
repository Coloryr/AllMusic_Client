package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.hud.ComType;
import com.coloryr.allmusic.client.hud.HudUtils;
import com.coloryr.allmusic.client.player.APlayer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod("allmusic")
public class AllMusic {
    private static APlayer nowPlaying;
    private static HudUtils hudUtils;
    private static final ResourceLocation channel = new ResourceLocation("allmusic", "channel");

    public AllMusic() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLClientSetupEvent event) {
        hudUtils = new HudUtils(FMLPaths.CONFIGDIR.get());
        try {
            Class parcleClass = Class.forName("coloryr.allmusic.AllMusicForge");
            Field m = parcleClass.getField("channel");
            SimpleChannel channel = (SimpleChannel) m.get(null);
            channel.registerMessage(1, PacketBuffer.class, this::encode, this::decode, this::handle);
        } catch (Exception e) {
            NetworkRegistry.ChannelBuilder.named(channel)
                    .networkProtocolVersion(() -> "1.0")
                    .clientAcceptedVersions(((status) -> true))
                    .serverAcceptedVersions(((status) -> true))
                    .simpleChannel()
                    .registerMessage(0, PacketBuffer.class, this::encode, this::decode, this::handle);
        }
    }

    public void encode(PacketBuffer msg, PacketBuffer buf) {

    }

    public PacketBuffer decode(PacketBuffer buf) {
        return buf;
    }

    public void handle(PacketBuffer buffer, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
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
                        Minecraft.getInstance().getSoundManager().stop(null, SoundCategory.MUSIC);
                        Minecraft.getInstance().getSoundManager().stop(null, SoundCategory.RECORDS);
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
        });
        ctx.get().setPacketHandled(true);
    }

    private void setup1(final FMLLoadCompleteEvent event) {
        nowPlaying = new APlayer();
    }

    public static int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getScreenHeight();
    }

    public static int getTextWidth(String item) {
        return Minecraft.getInstance().font.width(item);
    }

    public static int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @SubscribeEvent
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!nowPlaying.isPlay())
            return;
        SoundCategory data = e.getSound().getSource();
        switch (data) {
            case MUSIC:
            case RECORDS:
                e.getSource().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        try {
            stopPlaying();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        hudUtils.close();
        hudUtils.save = null;
    }

    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post e) {
        if (e.getType() == RenderGameOverlayEvent.ElementType.PORTAL) {
            hudUtils.update();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        nowPlaying.tick();
    }

    public static float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundCategory.RECORDS);
    }

    public static void drawPic(int textureID, int size, int x, int y, int ang) {
        GlStateManager._bindTexture(textureID);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        MatrixStack stack = new MatrixStack();
        Matrix4f matrix = stack.last().pose();

        int a = size / 2;

        matrix.setTranslation(x + a, y + a, 0);
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

        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.vertex(matrix, (float) x0, (float) y1, (float) z).uv(u0, v1).endVertex();
        bufferbuilder.vertex(matrix, (float) x1, (float) y1, (float) z).uv(u1, v1).endVertex();
        bufferbuilder.vertex(matrix, (float) x1, (float) y0, (float) z).uv(u1, v0).endVertex();
        bufferbuilder.vertex(matrix, (float) x0, (float) y0, (float) z).uv(u0, v0).endVertex();
        bufferbuilder.end();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.end(bufferbuilder);
    }

    private static MatrixStack stack = new MatrixStack();

    public static void drawText(String item, int x, int y, int color, boolean shadow) {
        FontRenderer hud = Minecraft.getInstance().font;
        if (shadow) {
            hud.drawShadow(stack, item, x, y, color);
        } else {
            hud.draw(stack, item, x, y, color);
        }
    }

    private void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    public static void runMain(Runnable runnable){
        Minecraft.getInstance().execute(runnable);
    }

    public static void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent(data));
        });
    }
}
