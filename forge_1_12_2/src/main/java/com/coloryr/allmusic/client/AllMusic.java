package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Mod(modid = "allmusic_client", version = "3.1.0", acceptedMinecraftVersions = "[1.12,)")
@SideOnly(Side.CLIENT)
public class AllMusic implements AllMusicBridge {
    @SubscribeEvent
    public void onMessage(FMLNetworkEvent.ClientCustomPacketEvent message) {
        try {
            AllMusicCore.packRead(message.getPacket().payload());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Mod.EventHandler
    public void preload(final FMLPreInitializationEvent evt) {
        if (!evt.getModConfigurationDirectory().exists()) {
            evt.getModConfigurationDirectory().mkdirs();
        }
        AllMusicCore.init(evt.getModConfigurationDirectory().toPath(), this);
        AllMusicCore.glInit();
        MinecraftForge.EVENT_BUS.register(this);
        try {
            Class<?> server = Class.forName("com.coloryr.allmusic.server.AllMusicForge");
            Field m = server.getField("channel");
            FMLEventChannel channel = (FMLEventChannel) m.get(null);
            channel.register(this);
        } catch (Exception e) {
            NetworkRegistry.INSTANCE.newEventDrivenChannel("allmusic:channel")
                    .register(this);
        }
    }

    @SubscribeEvent
    public void onSound(final PlaySoundEvent e) {
        if (!AllMusicCore.isPlay()) return;
        SoundCategory data = e.getSound().getCategory();
        switch (data) {
            case MUSIC:
            case RECORDS:
                e.setResultSound(null);
        }
    }

    @SubscribeEvent
    public void onServerQuit(final FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        AllMusicCore.onServerQuit();
    }

    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderOverlay(RenderGameOverlayEvent.Pre e) {
        if (e.getType() == RenderGameOverlayEvent.ElementType.PORTAL) {
            AllMusicCore.hudUpdate();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AllMusicCore.tick();
        }
    }

    public int getScreenWidth() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public int getScreenHeight() {
        return Minecraft.getMinecraft().displayHeight;
    }

    public int getTextWidth(String item) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(item);
    }

    public int getFontHeight() {
        return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
    }

    public float getVolume() {
        return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS);
    }

    @Override
    public void stopPlayMusic() {
        Minecraft.getMinecraft().getSoundHandler().stop("", SoundCategory.MUSIC);
        Minecraft.getMinecraft().getSoundHandler().stop("", SoundCategory.RECORDS);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang) {
        GlStateManager.bindTexture((int)textureID);
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        int a = size / 2;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + a, (float) y + a, 0.0f);

        if (ang > 0) {
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
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableAlpha();
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawString(item, x, y, color, shadow);
    }

    public void sendMessage(String data) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(data);
        });
    }

    @Override
    public Object genTexture(int size) {
        return AllMusicCore.genGLTexture(size);
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        AllMusicCore.updateGLTexture((int) tex, size, byteBuffer);
    }
}
