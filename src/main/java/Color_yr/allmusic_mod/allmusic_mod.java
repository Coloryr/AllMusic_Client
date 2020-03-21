package Color_yr.allmusic_mod;

import io.netty.buffer.Unpooled;
import javazoom.jl.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Mod("allmusic_mod")
public class allmusic_mod {
    private static final Player nowPlaying = new Player();
    private static URL nowURL;
    private SimpleChannel channel;

    public final Thread thread = new Thread(() -> {
        while (true) {
            try {
                int nowV = (int) (Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.RECORDS) *
                        Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.MASTER) * 100);
                nowPlaying.Set(nowV);
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    public allmusic_mod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLClientSetupEvent event) {
        channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation("allmusic", "channel"))
                .networkProtocolVersion(() -> "zzzz")
                .serverAcceptedVersions(NetworkRegistry.ACCEPTVANILLA::equals)
                .clientAcceptedVersions(NetworkRegistry.ACCEPTVANILLA::equals)
                .simpleChannel();
        channel.registerMessage(666, String.class, this::enc, this::dec, this::proc);
        thread.start();
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
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        stopPlaying();
    }

    private void onClicentPacket(final String message) {
        final Thread asyncThread = new Thread(() -> {
            if (message.contains("[Check]")) {
                try {
                    Thread.sleep(1000);
                    channel.sendToServer("666");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (message.equals("[Stop]")) {
                allmusic_mod.this.stopPlaying();
            } else if (message.startsWith("[Play]")) {
                try {
                    stopPlaying();
                    allmusic_mod.nowURL = new URL(message.replace("[Play]", ""));
                    nowPlaying.SetMusic(nowURL.openStream());
                    nowPlaying.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        asyncThread.start();
    }

    private void stopPlaying() {
        nowPlaying.close();
    }
}
