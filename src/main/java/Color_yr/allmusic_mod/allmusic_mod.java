package Color_yr.allmusic_mod;

import javazoom.jl.player.Player;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
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
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Player nowPlaying = new Player();
    private static URL nowURL;
    private SimpleChannel channel;

    public allmusic_mod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation("allmusic", "channel"))
                .networkProtocolVersion(() -> "zzzz")
                .serverAcceptedVersions(NetworkRegistry.ACCEPTVANILLA::equals)
                .clientAcceptedVersions(NetworkRegistry.ACCEPTVANILLA::equals)
                .simpleChannel();
        channel.registerMessage(666, String.class, this::enc, this::dec, this::proc);
        set(100);
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
            if (message.equals("[Stop]")) {
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
            } else if (message.startsWith("[V]")) {
                try {
                    String a = message.replace("[V]", "");
                    set(Integer.parseInt(a));
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

    private void set(int a) {
        try {
            final float temp = (a == 0) ? -80.0f : ((float) (a * 0.2 - 20.0));
            nowPlaying.Set(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
