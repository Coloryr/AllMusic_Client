package Color_yr.All_music;

import io.netty.buffer.ByteBuf;
import javazoom.jl.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import java.net.URL;

@Mod(modid = "all_music", version = "1.0.0", acceptedMinecraftVersions = "[1.9,)")
public class ALL_MUSIC {
    static final String MODID = "all_music";
    static final String VERSION = "1.0.0";
    private static FMLEventChannel channel;
    private static Player nowPlaying;
    private static URL nowURL;

    @Mod.EventHandler
    public void preload(final FMLPreInitializationEvent evt) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        (ALL_MUSIC.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("allmusic")).register((Object) this);
        set(100);
    }

    @SubscribeEvent
    public void onServerQuit(final FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        if (ALL_MUSIC.nowPlaying != null && !ALL_MUSIC.nowPlaying.isComplete()) {
            ALL_MUSIC.nowPlaying.close();
        }
    }

    @SubscribeEvent
    public void onClicentPacket(final FMLNetworkEvent.ClientCustomPacketEvent evt) {
        final Thread asyncThread = new Thread(() -> {
            final ByteBuf directBuf = evt.getPacket().payload();
            final int length = directBuf.readableBytes();
            final byte[] array = new byte[length];
            directBuf.getBytes(directBuf.readerIndex(), array);
            String message = new String(array);
            if (message.equals("[Stop]")) {
                ALL_MUSIC.this.stopPlaying();
            } else if (message.startsWith("[Play]")) {
                try {
                    if (nowPlaying != null && !nowPlaying.isComplete()) {
                        nowPlaying.close();
                    }
                    ALL_MUSIC.nowURL = new URL(message.replace("[Play]", ""));
                    nowPlaying = new Player(nowURL.openStream());
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
        if (nowPlaying != null) {
            nowPlaying.close();
            nowPlaying = null;
        }
    }

    private void set(int a) {
        try {
            final Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
            for (final Mixer.Info info : mixerInfo) {
                final Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(Port.Info.SPEAKER)) {
                    final Port port = (Port) mixer.getLine(Port.Info.SPEAKER);
                    port.open();
                    if (port.isControlSupported(FloatControl.Type.VOLUME)) {
                        final FloatControl volume = (FloatControl) port.getControl(FloatControl.Type.VOLUME);
                        volume.setValue((float) a / 1000);
                    }
                    port.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
