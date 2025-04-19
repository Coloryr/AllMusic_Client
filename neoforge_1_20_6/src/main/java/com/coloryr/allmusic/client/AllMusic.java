package com.coloryr.allmusic.client;

import com.coloryr.allmusic.client.hud.AllMusicBridge;
import com.coloryr.allmusic.client.hud.AllMusicHelper;
import com.coloryr.allmusic.client.hud.ComType;
import com.coloryr.allmusic.client.hud.HudUtils;
import com.coloryr.allmusic.client.player.APlayer;
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
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Mod("allmusic_client")
public class AllMusic implements IPayloadHandler<PackData>, StreamCodec<RegistryFriendlyByteBuf, PackData>, AllMusicBridge {
    private static GuiGraphics gui;

    public static final ResourceLocation channel =
            new ResourceLocation("allmusic", "channel");

    public AllMusic(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::setup1);
        modEventBus.addListener(this::onLoad);
        modEventBus.addListener(this::register);

        NeoForge.EVENT_BUS.register(this);
    }

    public void sendMessage(String data) {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal(data)));
    }

    private void setup(final FMLClientSetupEvent event) {
        AllMusicHelper.hudInit(FMLPaths.CONFIGDIR.get());
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
        //context.handle(payload);
    }

    public void handle(ByteBuf buffer) {
        try {
            byte type = buffer.readByte();
            if (type >= HudUtils.types.length || type < 0) {
                return;
            }
            ComType type1 = ComType.values()[type];
            String data = null;
            int data1 = 0;
            switch (type1) {
                case lyric:
                case info:
                case list:
                case play:
                case img:
                case hud:
                    data = readString(buffer);
                    break;
                case pos:
                    data1 = buffer.readInt();
                    break;
            }
            if (type1 == ComType.play) {
                Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
                Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
            }
            AllMusicHelper.hudState(type1, data, data1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    private void setup1(final FMLCommonSetupEvent event) {
        AllMusicHelper.init(this);
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
        AllMusicHelper.reload();
    }

    @SubscribeEvent
    public void onSound(final PlaySoundSourceEvent e) {
        if (!AllMusicHelper.isPlay()) return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onSound(final PlayStreamingSourceEvent e) {
        if (!AllMusicHelper.isPlay()) return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggingOut e) {
        AllMusicHelper.onServerQuit();
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
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).uv(u0, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).uv(u1, v1).endVertex();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).uv(u1, v0).endVertex();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).uv(u0, v0).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        var hud = Minecraft.getInstance().font;
        gui.drawString(hud, item, x, y, color, shadow);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiLayerEvent.Pre e) {
        if (e.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) {
            gui = e.getGuiGraphics();
            AllMusicHelper.hudUpdate();
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Post event) {
        AllMusicHelper.tick();
    }

    @Override
    public Object genTexture(int size) {
        return AllMusicHelper.gen(size);
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        AllMusicHelper.update((int) tex, size, byteBuffer);
    }
}
