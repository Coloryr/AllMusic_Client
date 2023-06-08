package coloryr.allmusic_client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import coloryr.allmusic_client.hud.HudUtils;
import coloryr.allmusic_client.player.APlayer;
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

@Mod(modid = "allmusic_client", version = "2.6.8", name = "AllMusic_Client", acceptedMinecraftVersions = "[1.7.10]")
public class AllMusic {

    private static APlayer nowPlaying;
    private static HudUtils hudUtils;
    private String url;
    private static int ang = 0;
    private static int count = 0;

    private static ScheduledExecutorService service;

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
        nowPlaying = new APlayer();
        hudUtils = new HudUtils();

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(AllMusic::time1, 0, 1, TimeUnit.MILLISECONDS);
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
        hudUtils.Lyric = hudUtils.Info = hudUtils.List = "";
        hudUtils.haveImg = false;
        hudUtils.save = null;
    }

    @SubscribeEvent
    public void onClicentPacket(final FMLNetworkEvent.ClientCustomPacketEvent evt) {
        try {
            final ByteBuf directBuf = evt.packet.payload();
            byte[] array = new byte[directBuf.readableBytes()];
            directBuf.getBytes(directBuf.readerIndex(), array);
            array[0] = 0;
            String message = new String(array, StandardCharsets.UTF_8).substring(1);
            if (message.equals("[Stop]")) {
                stopPlaying();
            } else if (message.startsWith("[Play]")) {
                Minecraft.getMinecraft()
                    .getSoundHandler()
                    .stopSounds();
                Minecraft.getMinecraft()
                    .getSoundHandler()
                    .stopSounds();
                stopPlaying();
                url = message.replace("[Play]", "");
                nowPlaying.setMusic(url);
            } else if (message.startsWith("[Lyric]")) {
                hudUtils.Lyric = message.substring(7);
            } else if (message.startsWith("[Info]")) {
                hudUtils.Info = message.substring(6);
            } else if (message.startsWith("[Img]")) {
                hudUtils.setImg(message.substring(5));
            } else if (message.startsWith("[Pos]")) {
                nowPlaying.set(message.substring(5));
            } else if (message.startsWith("[List]")) {
                hudUtils.List = message.substring(6);
            } else if (message.equalsIgnoreCase("[clear]")) {
                hudUtils.Lyric = hudUtils.Info = hudUtils.List = "";
                hudUtils.haveImg = false;
            } else if (message.startsWith("{")) {
                hudUtils.setPos(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderOverlay(final RenderGameOverlayEvent.Post e) {
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

    public static void drawPic(int textureID, int size, int x, int y) {
        int a = size / 2;

        GL11.glBindTexture(3553, textureID);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        // GL11.glEnable(GL11.GL_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + a, (float) y + a, 0.0f);

        if (hudUtils.save.EnablePicRotate && hudUtils.thisRoute) {
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
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawText(String item, float x, float y) {
        GuiIngame gui = Minecraft.getMinecraft().ingameGUI;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        gui.drawString(font, item, (int) x, (int) y, 0xffffff);
    }

    private static void time1() {
        if (hudUtils.save == null) return;
        if (count < hudUtils.save.PicRotateSpeed) {
            count++;
            return;
        }
        count = 0;
        ang++;
        ang = ang % 360;
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
