package com.coloryr.allmusic.client;

import java.io.File;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.coloryr.allmusic.client.hud.ComType;
import com.coloryr.allmusic.client.hud.HudUtils;
import com.coloryr.allmusic.client.player.APlayer;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

@Mod(modid = "allmusic_client", version = "3.0.0", name = "AllMusic_Client", acceptedMinecraftVersions = "[1.7.10]")
@SideOnly(Side.CLIENT)
public class AllMusic {

    private static APlayer nowPlaying;
    private static HudUtils hudUtils;

    public static void sendMessage(String data) {
        FMLClientHandler.instance()
            .getClient()
            .func_152344_a(
                () -> {
                    FMLClientHandler.instance()
                        .getClient().ingameGUI.getChatGUI()
                            .addToSentMessages(data);
                });
    }

    @Mod.EventHandler
    public void test(final FMLLoadCompleteEvent event) {
        hudUtils = new HudUtils(new File("config").toPath());
        nowPlaying = new APlayer();
    }

    @Mod.EventHandler
    public void preload(final FMLPreInitializationEvent evt) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        NetworkRegistry.INSTANCE.newEventDrivenChannel("allmusic:channel")
            .register(this);
    }

    @SubscribeEvent
    public void onSound(final PlaySoundEvent17 e) {
        if (!nowPlaying.isPlay()) return;
        SoundCategory data = e.category;
        if (data == null) return;
        switch (data) {
            case MUSIC:
            case RECORDS:
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    FMLClientHandler.instance()
                        .getClient()
                        .func_152344_a(() -> { e.manager.stopSound(e.sound); });
                }).start();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        try {
            stopPlaying();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        hudUtils.close();
        hudUtils.save = null;
    }

    public static int getScreenWidth() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public static int getScreenHeight() {
        return Minecraft.getMinecraft().displayHeight;
    }

    public static int getTextWidth(String item) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(item);
    }

    public static int getFontHeight() {
        return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
    }

    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientPacket(final FMLNetworkEvent.ClientCustomPacketEvent evt) {
        final ByteBuf buffer = evt.packet.payload();
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
                    Minecraft.getMinecraft()
                            .getSoundHandler()
                            .stopSounds();
                    Minecraft.getMinecraft()
                            .getSoundHandler()
                            .stopSounds();
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

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderOverlay(final RenderGameOverlayEvent.Pre e) {
        if (e.type == RenderGameOverlayEvent.ElementType.PORTAL) {
            hudUtils.update();
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) nowPlaying.tick();
    }

    public static float getVolume() {
        return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS);
    }

    public static void drawPic(int textureID, int size, int x, int y, int ang) {
        int a = size / 2;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        // GL11.glEnable(GL11.GL_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + a, (float) y + a, 0.0f);

        if (ang > 0) {
            GL11.glRotatef(ang, 0, 0, 1f);
        }

        int x0 = -a;
        int x1 = a;
        int y0 = -a;
        int y1 = a;
        GL11.glBegin(GL11.GL_QUADS);
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
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawText(String item, int x, int y, int color, boolean shadow) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawString(item, x, y, color, shadow);
    }

    private void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    public static void runMain(Runnable runnable) {
        FMLClientHandler.instance()
            .getClient()
            .func_152344_a(runnable);
    }
}
