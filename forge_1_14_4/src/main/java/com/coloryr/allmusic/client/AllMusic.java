package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.hud.AllMusicBridge;
import com.coloryr.allmusic.client.hud.AllMusicHelper;
import com.coloryr.allmusic.client.hud.ComType;
import com.coloryr.allmusic.client.hud.HudUtils;
import com.coloryr.allmusic.client.player.APlayer;
import com.mojang.blaze3d.platform.GlStateManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.lwjgl.opengl.GL11;

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
                Minecraft.getInstance().getSoundHandler().stop(null, SoundCategory.MUSIC);
                Minecraft.getInstance().getSoundHandler().stop(null, SoundCategory.RECORDS);
            }
            AllMusicHelper.hudState(type1, data, data1);
            event.getSource().get().setPacketHandled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readString(PacketBuffer buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    private void setup1(final FMLLoadCompleteEvent event) {
        AllMusicHelper.init(this);
    }

    public int getScreenWidth() {
        return Minecraft.getInstance().mainWindow.getScaledWidth();
    }

    public int getScreenHeight() {
        return Minecraft.getInstance().mainWindow.getScaledHeight();
    }

    public int getTextWidth(String item) {
        return Minecraft.getInstance().fontRenderer.getStringWidth(item);
    }

    public int getFontHeight() {
        return Minecraft.getInstance().fontRenderer.FONT_HEIGHT;
    }

    @SubscribeEvent
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!AllMusicHelper.isPlay()) return;
        SoundCategory data = e.getSound().getCategory();
        switch (data) {
            case MUSIC:
            case RECORDS:
                e.getSource().func_216418_f();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        AllMusicHelper.onServerQuit();
    }

    @SubscribeEvent
    public void onRed(final RenderGameOverlayEvent.Pre e) {
        if (e.getType() == RenderGameOverlayEvent.ElementType.PORTAL) {
            AllMusicHelper.hudUpdate();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        if (event.phase == TickEvent.Phase.END) {
            AllMusicHelper.tick();
        }
    }

    public float getVolume() {
        return Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.RECORDS);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang) {
        int a = size / 2;

        GlStateManager.bindTexture((int)textureID);
        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableAlphaTest();
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + a, (float) y + a, 0.0f);

        if(ang > 0) {
            GL11.glRotatef(ang, 0, 0, 1f);
        }

        int x0 = -a;
        int x1 = a;
        int y0 = -a;
        int y1 = a;

        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(x0, y0, 0.0f);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(x0, y1, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x1, y1, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x1, y0, 0.0f);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableAlphaTest();
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        FontRenderer hud = Minecraft.getInstance().fontRenderer;

        if (shadow) {
            hud.drawStringWithShadow(item, x, y, color);
        } else {
            hud.drawString(item, x, y, color);
        }
    }
    public void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().ingameGUI.getChatGUI().addToSentMessages(data);
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
