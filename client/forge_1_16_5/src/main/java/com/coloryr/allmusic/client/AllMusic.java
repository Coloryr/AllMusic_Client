package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.UUID;

@Mod("allmusic_client")
public class AllMusic implements AllMusicBridge {
    private static final ResourceLocation channel = new ResourceLocation("allmusic", "channel");
    private static final PoseStack stack = new PoseStack();

    public static final Logger LOGGER = LogManager.getLogger("AllMusic Client");

    public AllMusic() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLClientSetupEvent event) {
        AllMusicCore.init(FMLPaths.CONFIGDIR.get(), this);
        event.enqueueWork(AllMusicCore::glInit);
        NetworkRegistry.ChannelBuilder.named(channel)
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions((status) -> true)
                .serverAcceptedVersions((status) -> true)
                .eventNetworkChannel()
                .addListener(this::handle);
    }

    @Override
    public void stopPlayMusic() {
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
    }

    public void handle(NetworkEvent.ServerCustomPayloadEvent event) {
        try {
            AllMusicCore.packRead(event.getPayload());
            event.getSource().get().setPacketHandled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @SubscribeEvent
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!AllMusicCore.isPlay()) {
            return;
        }

        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC:
            case RECORDS:
                e.getSource().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        AllMusicCore.onServerQuit();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre e) {
        if (e.getType() == RenderGameOverlayEvent.ElementType.PORTAL) {
            AllMusicCore.hudUpdate();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        AllMusicCore.tick();
    }

    public float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang) {
        GlStateManager._bindTexture((int) textureID);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        Matrix4f matrix = Matrix4f.createTranslateMatrix(x + size / 2.0F, y + size / 2.0F, 0.0F);
        if (ang > 0) {
            matrix.multiply(new Quaternion(0, 0, ang, true));
        }

        int a = size / 2;
        int x0 = -a;
        int x1 = a;
        int y0 = -a;
        int y1 = a;
        int z = 0;
        int u0 = 0;
        float u1 = 1;
        float v0 = 0;
        float v1 = 1;

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(7, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).uv(u0, v0).endVertex();
        bufferBuilder.end();
        RenderSystem.enableAlphaTest();
        BufferUploader.end(bufferBuilder);
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        Font hud = Minecraft.getInstance().font;
        Component component = MiniMessage.parse(item);
        if (shadow) {
            hud.drawShadow(stack, component, x, y, color);
        } else {
            hud.draw(stack, component, x, y, color);
        }
    }

    public void sendMessage(String data) {
        data = "[AllMusic Client]" + data;
        LOGGER.warn(data);
        String finalData = data;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }
            Minecraft.getInstance().player.sendMessage(new TextComponent(finalData), UUID.randomUUID());
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
