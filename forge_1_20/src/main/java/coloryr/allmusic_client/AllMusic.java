package coloryr.allmusic_client;

import coloryr.allmusic_client.hud.HudUtils;
import coloryr.allmusic_client.player.APlayer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Mod("allmusic_client")
public class AllMusic {
    private static APlayer nowPlaying;
    private static HudUtils hudUtils;
    private static GuiGraphics gui;

    public AllMusic() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);
        modEventBus.addListener(this::onLoad);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(data));
        });
    }

    private void setup(final FMLClientSetupEvent event) {
        hudUtils = new HudUtils(FMLPaths.CONFIGDIR.get());
        try {
            Class parcleClass = Class.forName("coloryr.allmusic.AllMusicForge");
            Field m = parcleClass.getField("channel");
            SimpleChannel channel = (SimpleChannel) m.get(null);
            channel.registerMessage(666, String.class, this::enc, this::dec, this::proc);
        } catch (Exception e) {
            e.printStackTrace();

            var channel = NetworkRegistry.newSimpleChannel(new ResourceLocation("allmusic", "channel"),
                    () -> "1.0", s -> true, s -> true);
            channel.registerMessage(666, String.class, this::enc, this::dec, this::proc);
        }
    }

    private void setup1(final FMLLoadCompleteEvent event) {
        nowPlaying = new APlayer();
    }

    private void enc(String str, FriendlyByteBuf buffer) {
        buffer.writeBytes(str.getBytes(StandardCharsets.UTF_8));
    }

    private String dec(FriendlyByteBuf buffer) {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private void proc(String str, Supplier<NetworkEvent.Context> supplier) {
        onClicentPacket(str);
        NetworkEvent.Context context = supplier.get();
        context.setPacketHandled(true);
    }

    public void onLoad(final SoundEngineLoadEvent e) {
        if (nowPlaying != null) {
            nowPlaying.setReload();
        }
    }

    @SubscribeEvent
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!nowPlaying.isPlay())
            return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggingOut e) {
        try {
            stopPlaying();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        hudUtils.close();
        hudUtils.save = null;
    }

    private void onClicentPacket(final String message) {
        try {
            if (message.equals("[Stop]")) {
                stopPlaying();
            } else if (message.startsWith("[Play]")) {
                Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
                Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
                stopPlaying();
                String url = message.replace("[Play]", "");
                nowPlaying.setMusic(url);
            } else if (message.startsWith("[Lyric]")) {
                hudUtils.lyric = message.substring(7);
            } else if (message.startsWith("[Info]")) {
                hudUtils.info = message.substring(6);
            } else if (message.startsWith("[Img]")) {
                hudUtils.setImg(message.substring(5));
            } else if (message.startsWith("[Pos]")) {
                nowPlaying.set(message.substring(5));
            } else if (message.startsWith("[List]")) {
                hudUtils.list = message.substring(6);
            } else if (message.equalsIgnoreCase("[clear]")) {
                hudUtils.close();
            } else if (message.startsWith("{")) {
                hudUtils.setPos(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    public static void drawPic(int textureID, int size, int x, int y, int ang) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureID);

        PoseStack stack = new PoseStack();
        Matrix4f matrix = stack.last().pose();

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

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).uv(u0, v0).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());

        //GuiComponent.blit(stack, x, y, 0, 0, 0, size, size, size, size);
    }

    public static void drawText(String item, float x, float y) {
        var hud = Minecraft.getInstance().font;
        gui.drawString(hud, item, (int) x, (int) y, 0xffffff);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post e) {
        if (e.getOverlay().id() == VanillaGuiOverlay.PORTAL.id()) {
            gui = e.getGuiGraphics();
            hudUtils.update();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        nowPlaying.tick();
    }

    private void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
    }

    public static void runMain(Runnable runnable){
        Minecraft.getInstance().execute(runnable);
    }
}
