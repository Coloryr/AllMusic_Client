package coloryr.allmusic_client;

import coloryr.allmusic_client.hud.HudUtils;
import coloryr.allmusic_client.player.APlayer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AllMusic implements ModInitializer {
    public static final Identifier ID = new Identifier("allmusic", "channel");
    public static APlayer nowPlaying;
    public static HudUtils hudUtils;
    private static int ang = 0;
    private static int count = 0;
    private static DrawContext context;

    private static ScheduledExecutorService service;

    public static void onServerQuit() {
        try {
            nowPlaying.close();
            hudUtils.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        hudUtils.Lyric = hudUtils.Info = hudUtils.List = "";
        hudUtils.haveImg = false;
        hudUtils.save = null;
    }

    public static void onClientPacket(final String message) {
        new Thread(() -> {
            try {
                if (message.equals("[Stop]")) {
                    stopPlaying();
                } else if (message.startsWith("[Play]")) {
                    MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
                    MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
                    stopPlaying();
                    nowPlaying.setMusic(message.replace("[Play]", ""));
                } else if (message.startsWith("[Lyric]")) {
                    hudUtils.Lyric = message.substring(7);
                } else if (message.startsWith("[Info]")) {
                    hudUtils.Info = message.substring(6);
                } else if (message.startsWith("[List]")) {
                    hudUtils.List = message.substring(6);
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
        }, "allmusic").start();
    }

    private static void stopPlaying() {
        try {
            nowPlaying.closePlayer();
            hudUtils.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void drawText(String item, float x, float y) {
        var hud = MinecraftClient.getInstance().textRenderer;
        context.drawText(hud, item, (int) x, (int) y, 0xffffff, false);
    }

    public static void drawPic(int textureID, int size, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureID);

        MatrixStack stack = new MatrixStack();
        Matrix4f matrix = stack.peek().getPositionMatrix();

        int a = size / 2;


        if(hudUtils.save.EnablePicRotate && hudUtils.thisRoute) {
            matrix = matrix.translationRotate(x + a, y + a, 0,
                    new Quaternionf().fromAxisAngleDeg(0,0,1, ang));
        }
        else {
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

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).texture(u0, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).texture(u1, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).texture(u1, v0).next();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).texture(u0, v0).next();

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

//        DrawableHelper.drawTexture();
    }

    public static void sendMessage(String data) {
        MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(data)));
    }

    public static void runMain(Runnable runnable) {
        MinecraftClient.getInstance().execute(runnable);
    }

    public static float getVolume() {
        return MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS);
    }

    public static void reload() {
        if (nowPlaying != null) {
            nowPlaying.setReload();
        }
    }

    private static void time1() {
        if (hudUtils.save == null)
            return;
        if (count < hudUtils.save.PicRotateSpeed) {
            count++;
            return;
        }
        count = 0;
        ang++;
        ang = ang % 360;
    }

    public static void update(DrawContext draw) {
        context = draw;
        hudUtils.update();
    }

    @Override
    public void onInitialize() {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buffer, responseSender) -> {
            try {
                byte[] buff = new byte[buffer.readableBytes()];
                buffer.readBytes(buff);
                buff[0] = 0;
                String data = new String(buff, StandardCharsets.UTF_8).substring(1);
                onClientPacket(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        nowPlaying = new APlayer();
        hudUtils = new HudUtils();

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(AllMusic::time1, 0, 1, TimeUnit.MILLISECONDS);
    }
}
