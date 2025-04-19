package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.hud.AllMusicBridge;
import com.coloryr.allmusic.client.hud.AllMusicHelper;
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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod("allmusic_client")
public class AllMusic implements AllMusicBridge {
    private static final ResourceLocation channel = new ResourceLocation("allmusic", "channel");

    public AllMusic() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLClientSetupEvent event) {
        AllMusicHelper.hudInit(FMLPaths.CONFIGDIR.get());
        NetworkRegistry.ChannelBuilder.named(channel)
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(((status) -> true))
                .serverAcceptedVersions(((status) -> true))
                .eventNetworkChannel()
                .addListener(this::handle);
    }

    public void handle(NetworkEvent.ServerCustomPayloadEvent event) {
        try {
            PacketBuffer buffer = event.getPayload();
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
                Minecraft.getInstance().getSoundManager().stop(null, SoundCategory.MUSIC);
                Minecraft.getInstance().getSoundManager().stop(null, SoundCategory.RECORDS);
            }
            AllMusicHelper.hudState(type1, data, data1);
            event.getSource().get().setPacketHandled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setup1(final FMLLoadCompleteEvent event) {
        AllMusicHelper.init(this);
    }

    public int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getScreenHeight();
    }

    public int getTextWidth(String item) {
        return Minecraft.getInstance().font.width(item);
    }

    public int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @SubscribeEvent
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!AllMusicHelper.isPlay()) return;
        SoundCategory data = e.getSound().getSource();
        switch (data) {
            case MUSIC:
            case RECORDS:
                e.getSource().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        AllMusicHelper.onServerQuit();
    }

    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre e) {
        if (e.getType() == RenderGameOverlayEvent.ElementType.PORTAL) {
            AllMusicHelper.hudUpdate();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        AllMusicHelper.tick();
    }

    public float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundCategory.RECORDS);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang){
        GlStateManager._bindTexture((int)textureID);
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

    private static final MatrixStack stack = new MatrixStack();

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        FontRenderer hud = Minecraft.getInstance().font;
        if (shadow) {
            hud.drawShadow(stack, item, x, y, color);
        } else {
            hud.draw(stack, item, x, y, color);
        }
    }

    public void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent(data));
        });
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
