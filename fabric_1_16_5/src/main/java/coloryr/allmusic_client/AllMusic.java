package coloryr.allmusic_client;

import coloryr.allmusic_client.hud.HudUtils;
import coloryr.allmusic_client.player.APlayer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AllMusic implements ModInitializer {
    public static final Identifier ID = new Identifier("allmusic", "channel");
    public static APlayer nowPlaying;
    public static String url;
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

    public static void onClicentPacket(final String message) {
        try {
            if (message.equals("[Stop]")) {
                stopPlaying();
            } else if (message.startsWith("[Play]")) {
                MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
                MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
                stopPlaying();
                url = message.replace("[Play]", "");
                nowPlaying.setMusic(url);
            } else if (message.startsWith("[Lyric]")) {
                hudUtils.lyric = message.substring(7);
            } else if (message.startsWith("[Info]")) {
                hudUtils.info = message.substring(6);
            } else if (message.startsWith("[List]")) {
                hudUtils.list = message.substring(6);
            } else if (message.startsWith("[Img]")) {
                hudUtils.setImg(message.substring(5));
            } else if (message.startsWith("[Pos]")) {
                nowPlaying.set(message.substring(5));
            } else if (message.equalsIgnoreCase("[clear]")) {
                hudUtils.close();
            } else if (message.startsWith("{")) {
                hudUtils.setPos(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    private static final MatrixStack stack = new MatrixStack();

    public static void drawText(String item, float x, float y){
        InGameHud hud = MinecraftClient.getInstance().inGameHud;
        TextRenderer textRenderer = hud.getFontRenderer();
        textRenderer.drawWithShadow(stack, item, x, y, 0xffffff);
    }

    public static void drawPic(int textureID, int size, int x, int y, int ang) {
        GlStateManager.bindTexture(textureID);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableAlphaTest();

        Matrix4f matrix;

        int a = size / 2;

        matrix = Matrix4f.translate(x + a, y + a, 0);
        if(hudUtils.save.EnablePicRotate && hudUtils.thisRoute) {
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

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, (float)x0, (float)y1, (float)z).texture(u0, v1).next();
        bufferBuilder.vertex(matrix, (float)x1, (float)y1, (float)z).texture(u1, v1).next();
        bufferBuilder.vertex(matrix, (float)x1, (float)y0, (float)z).texture(u1, v0).next();
        bufferBuilder.vertex(matrix, (float)x0, (float)y0, (float)z).texture(u0, v0).next();
        bufferBuilder.end();
        RenderSystem.enableAlphaTest();
        BufferRenderer.draw(bufferBuilder);

        //DrawableHelper.drawTexture(stack, x, y, 0, 0, 0, size, size, size, size);
        //RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        //RenderSystem.enableAlphaTest();
    }

    public static void sendMessage(String data){
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player == null)
                return;
            MinecraftClient.getInstance().player.sendChatMessage(data);
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
                byte[] buff = new byte[buffer.readableBytes()];
                buffer.readBytes(buff);
                buff[0] = 0;
                String data = new String(buff, StandardCharsets.UTF_8).substring(1);
                onClicentPacket(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        hudUtils = new HudUtils(FabricLoader.getInstance().getConfigDir());
        nowPlaying = new APlayer();
    }
}