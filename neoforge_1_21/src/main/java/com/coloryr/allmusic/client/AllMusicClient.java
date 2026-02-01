package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.codec.MusicPack;
import com.coloryr.allmusic.comm.AllMusicInit;
import com.coloryr.allmusic.comm.MusicCodec;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.nio.ByteBuffer;

@EventBusSubscriber(modid = AllMusicInit.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class AllMusicClient implements IPayloadHandler<MusicCodec>, AllMusicBridge {
    private static GuiGraphics gui;

    public static final ResourceLocation channel = ResourceLocation.fromNamespaceAndPath("allmusic", "channel");

    public static final Logger LOGGER = LogManager.getLogger("AllMusic Client");

    @SubscribeEvent
    public static void setup(final FMLClientSetupEvent event) {
        AllMusicClient client = new AllMusicClient();
        AllMusicInit.handler = client;
        AllMusicCore.init(FMLPaths.CONFIGDIR.get(), client);
        event.enqueueWork(AllMusicCore::glInit);
    }

    @SubscribeEvent
    public static void onLoad(final SoundEngineLoadEvent e) {
        AllMusicCore.reload();
    }

    @EventBusSubscriber(modid = AllMusicInit.MODID, value = Dist.CLIENT)
    public static class MusicEvent
    {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            AllMusicCore.tick();
        }

        @SubscribeEvent
        public static void onSound(final PlaySoundSourceEvent e) {
            if (!AllMusicCore.isPlay()) return;
            SoundSource data = e.getSound().getSource();
            switch (data) {
                case MUSIC, RECORDS -> e.getChannel().stop();
            }
        }

        @SubscribeEvent
        public static void onSound(final PlayStreamingSourceEvent e) {
            if (!AllMusicCore.isPlay()) return;
            SoundSource data = e.getSound().getSource();
            switch (data) {
                case MUSIC, RECORDS -> e.getChannel().stop();
            }
        }

        @SubscribeEvent
        public static void onServerQuit(final ClientPlayerNetworkEvent.LoggingOut e) {
            AllMusicCore.onServerQuit();
        }

        @SubscribeEvent
        public static void onRenderOverlay(RenderGuiLayerEvent.Post e) {
            if (e.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) {
                gui = e.getGuiGraphics();
                AllMusicCore.hudUpdate();
            }
        }
    }

    public void sendMessage(String data) {
        data = "[AllMusic Client]" + data;
        LOGGER.warn(data);
        String finalData = data;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null)
                return;
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(finalData));
        });
    }

    @Override
    public void handle(MusicCodec pack, IPayloadContext iPayloadContext)  {
        try {
            AllMusicCore.packDo(pack.pack().type, pack.pack().data, pack.pack().data1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPlayMusic() {
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
    }

    public int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public int getTextWidth(String item) {
        return Minecraft.getInstance().font.width(item);
    }

    public int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, (int)textureID);

        PoseStack stack = new PoseStack();
        Matrix4f matrix = stack.last().pose();

        int a = size / 2;

        if (ang > 0) {
            matrix = matrix.translationRotate(x + a, y + a, 0,
                    new Quaternionf().fromAxisAngleDeg(0, 0, 1, ang));
        } else {
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
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, (float) x0, (float) y1, (float) z).setUv(u0, v1);
        bufferBuilder.addVertex(matrix, (float) x1, (float) y1, (float) z).setUv(u1, v1);
        bufferBuilder.addVertex(matrix, (float) x1, (float) y0, (float) z).setUv(u1, v0);
        bufferBuilder.addVertex(matrix, (float) x0, (float) y0, (float) z).setUv(u0, v0);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        var hud = Minecraft.getInstance().font;
        gui.drawString(hud, item, x, y, color, shadow);
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
