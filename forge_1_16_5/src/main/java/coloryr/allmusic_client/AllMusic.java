package coloryr.allmusic_client;

import coloryr.allmusic_client.hud.HudUtils;
import coloryr.allmusic_client.player.APlayer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Mod("allmusic")
public class AllMusic {
    private static APlayer nowPlaying;
    private static HudUtils hudUtils;
    private String url;

    private static int ang = 0;
    private static int count = 0;

    private static ScheduledExecutorService service;

    public AllMusic() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLClientSetupEvent event) {
        try {
            Class parcleClass = Class.forName("coloryr.allmusic.AllMusicForge");
            Field m = parcleClass.getField("channel");
            SimpleChannel channel = (SimpleChannel) m.get(null);
            channel.registerMessage(666, String.class, this::enc, this::dec, this::proc);
        } catch (Exception e) {
            e.printStackTrace();

            SimpleChannel channel = NetworkRegistry.newSimpleChannel(new ResourceLocation("allmusic", "channel"),
                    () -> "1.0", s -> true, s -> true);
            channel.registerMessage(666, String.class, this::enc, this::dec, this::proc);
        }
    }

    private void setup1(final FMLLoadCompleteEvent event) {
        nowPlaying = new APlayer();
        hudUtils = new HudUtils();

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(AllMusic::time1, 0, 1, TimeUnit.MILLISECONDS);
    }

    private void enc(String str, PacketBuffer buffer) {
        buffer.writeBytes(str.getBytes(StandardCharsets.UTF_8));
    }

    private String dec(PacketBuffer buffer) {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private void proc(String str, Supplier<NetworkEvent.Context> supplier) {
        onClicentPacket(str);
        NetworkEvent.Context context = supplier.get();
        context.setPacketHandled(true);
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
        hudUtils.Lyric = hudUtils.Info = hudUtils.List = "";
        hudUtils.haveImg = false;
        hudUtils.save = null;
    }

    private void onClicentPacket(final String message) {
        try {
            if (message.equals("[Stop]")) {
                stopPlaying();
            } else if (message.startsWith("[Play]")) {
                Minecraft.getInstance().getSoundManager().stop(null, SoundCategory.RECORDS);
                Minecraft.getInstance().getSoundManager().stop(null, SoundCategory.RECORDS);
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

    public static void drawPic(int textureID, int size, int x, int y) {
        GlStateManager._bindTexture(textureID);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        MatrixStack stack = new MatrixStack();
        Matrix4f matrix = stack.last().pose();

        int a = size / 2;

        matrix.setTranslation(x + a, y + a, 0);
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

    public static void drawText(String item, float x, float y) {
        FontRenderer hud = Minecraft.getInstance().font;
        hud.draw(stack, item, x, y, 0xffffff);
    }

    private void stopPlaying() {
        nowPlaying.closePlayer();
        hudUtils.close();
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

    public static void runMain(Runnable runnable){
        Minecraft.getInstance().execute(runnable);
    }

    public static void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent(data));
        });
    }
}
