package coloryr.allmusic_client;

import coloryr.allmusic_client.hud.ComType;
import coloryr.allmusic_client.hud.HudUtils;
import coloryr.allmusic_client.player.APlayer;
import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.SelectorText;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import org.lwjgl.opengl.GL11;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AllMusic implements ModInitializer {
    public static final Identifier ID = new Identifier("allmusic", "channel");
    public static APlayer nowPlaying;
    public static HudUtils hudUtils;

    public static void onServerQuit() {
        try {
            stopPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        hudUtils.close();
        hudUtils.save = null;
    }

    private static void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    public static int getScreenWidth() {
        return MinecraftClient.getInstance().window.getScaledWidth();
    }

    public static int getScreenHeight() {
        return MinecraftClient.getInstance().window.getScaledHeight();
    }

    public static int getTextWidth(String item) {
        return MinecraftClient.getInstance().textRenderer.getStringWidth(item);
    }

    public static int getFontHeight() {
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }

    public static void drawText(String item, int x, int y, int color, boolean shadow) {
        InGameHud hud = MinecraftClient.getInstance().inGameHud;
        TextRenderer textRenderer = hud.getFontRenderer();
        if (shadow) {
            textRenderer.drawWithShadow(item, x, y, color);
        } else {
            textRenderer.draw(item, x, y, color);
        }
    }

    public static void drawPic(int textureID, int size, int x, int y, int ang) {
        int a = size / 2;

        GlStateManager.bindTexture(textureID);
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
        GlStateManager.disableLighting();
        GlStateManager.enableAlphaTest();
    }

    public static void sendMessage(String data){
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new SelectorText(data));
        });
    }

    public static void runMain(Runnable runnable){
        MinecraftClient.getInstance().execute(runnable);
    }

    public static float getVolume(){
        return MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS);
    }

    @Override
    public void onInitialize() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buffer, responseSender) -> {
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
                        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
                        MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
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
        hudUtils = new HudUtils(FabricLoader.getInstance().getConfigDir());
        nowPlaying = new APlayer();
    }

    private static String readString(PacketByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }
}