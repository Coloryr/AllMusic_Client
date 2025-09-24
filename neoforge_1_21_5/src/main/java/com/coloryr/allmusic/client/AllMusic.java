package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
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
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Mod("allmusic_client")
public class AllMusic implements IPayloadHandler<PackData>, StreamCodec<RegistryFriendlyByteBuf, PackData>, AllMusicBridge {
    private static GuiGraphics gui;

    public static final ResourceLocation channel = ResourceLocation.fromNamespaceAndPath("allmusic", "channel");

    public static final Logger LOGGER = LogManager.getLogger("AllMusic Client");

    private static Function<ResourceLocation, RenderType> GUI_TEXTURED;

    public static class Tex extends GlTexture {
        protected Tex(int id, int width, int height) {
            super("allmusic", TextureFormat.RGBA8, width, height, 0, id);
        }
    }

    public static class Texture extends RenderStateShard.EmptyTextureStateShard {
        public Texture(Tex tex) {
            super(() -> RenderSystem.setShaderTexture(0, tex), () -> {
            });
        }
    }

    public AllMusic(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);
        modEventBus.addListener(this::onLoad);
        modEventBus.addListener(this::register);

        NeoForge.EVENT_BUS.register(this);
    }

    public void sendMessage(String data) {
        data = "[AllMusic Client]" + data;
        LOGGER.warn(data);
        String finalData = data;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null)
                return;
            Minecraft.getInstance().player.displayClientMessage(Component.literal(finalData), false);
        });
    }

    private void setup(final FMLClientSetupEvent event) {
        event.enqueueWork(AllMusicCore::glInit);
    }

    public void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0")
                .optional()
                .playToClient(PackData.TYPE, this,this);
    }

    @Override
    public PackData decode(RegistryFriendlyByteBuf pack) {
        handle(pack);
        pack.clear();
        return new PackData();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf pack, PackData data) {

    }

    @Override
    public void handle(@NotNull PackData payload, IPayloadContext context) {

    }

    @Override
    public void stopPlayMusic() {
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
    }

    public void handle(ByteBuf buffer) {
        try {
            AllMusicCore.packRead(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setup1(final FMLCommonSetupEvent event) {
        AllMusicCore.init(FMLPaths.CONFIGDIR.get(), this);
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

    public void onLoad(final SoundEngineLoadEvent e) {
        AllMusicCore.reload();
    }

    @SubscribeEvent
    public void onSound(final PlaySoundSourceEvent e) {
        if (!AllMusicCore.isPlay()) return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onSound(final PlayStreamingSourceEvent e) {
        if (!AllMusicCore.isPlay()) return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggingOut e) {
        AllMusicCore.onServerQuit();
    }

    public float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang) {
        PoseStack stack = gui.pose();
        stack.pushPose();
        Matrix4f matrix = stack.last().pose();

        int a = size / 2;

        if (ang > 0) {
            matrix.translationRotate(x + a, y + a, 0,
                    new Quaternionf().fromAxisAngleDeg(0, 0, 1, ang));
        } else {
            matrix.translation(x + a, y + a, 0);
        }

        gui.blit(GUI_TEXTURED, channel, -a, -a, 0, 0, size, size, size, size, size, size);
        stack.popPose();
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        var hud = Minecraft.getInstance().font;
        gui.drawString(hud, item, x, y, color, shadow);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiLayerEvent.Post e) {
        if (e.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) {
            gui = e.getGuiGraphics();
            AllMusicCore.hudUpdate();
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Post event) {
        AllMusicCore.tick();
    }

    @Override
    public Object genTexture(int size) {
        int textureID = AllMusicCore.genGLTexture(size);

        Tex tex = new Tex(textureID, size, size);
        tex.setTextureFilter(FilterMode.NEAREST, false);

        GUI_TEXTURED = Util.memoize((texture) -> RenderType.create("allmusic_gui_textured", 786432, RenderPipelines.GUI_TEXTURED, RenderType.CompositeState.builder().setTextureState(new Texture(tex)).createCompositeState(false)));

        return tex;
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        if (tex instanceof Tex tex1) {
            AllMusicCore.updateGLTexture(tex1.glId(), size, byteBuffer);
        }
    }
}
