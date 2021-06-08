package Color_yr.AllMusic;

import Color_yr.AllMusic.Hud.Hud;
import Color_yr.AllMusic.player.APlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Mod("allmusic")
public class AllMusic {
    private static final APlayer nowPlaying = new APlayer();
    private static URL nowURL;
    public static boolean isPlay = false;
    public static int v = -1;

    public final Thread thread = new Thread(() -> {
        while (true) {
            try {
                int nowV = (int) (Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.RECORDS) *
                        Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.MASTER) * 100);
                if (v != nowV) {
                    nowPlaying.Set(nowV);
                }
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    public AllMusic() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLClientSetupEvent event) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(new ResourceLocation("allmusic", "channel"),
                () -> "1.0", s -> true, s -> true);
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
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!isPlay)
            return;
        SoundCategory data = e.getSound().getCategory();
        switch (data) {
            case MUSIC:
            case RECORDS:
                e.getSource().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggedOutEvent e) {
        stopPlaying();
        Hud.Lyric = Hud.Info = Hud.List = "";
        Hud.save = null;
    }

    public static URL Get(URL url) {
        if (url.toString().contains("https://music.163.com/song/media/outer/url?id=")
                || url.toString().contains("http://music.163.com/song/media/outer/url?id=")) {
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(4 * 1000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52");
                connection.setRequestProperty("Host", "music.163.com");
                connection.connect();
                if (connection.getResponseCode() == 302) {
                    return new URL(connection.getHeaderField("Location"));
                }
                return connection.getURL();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    private void onClicentPacket(final String message) {
        final Thread asyncThread = new Thread(() -> {
            try {
                if (message.equals("[Stop]")) {
                    stopPlaying();
                } else if (message.startsWith("[Play]")) {
                    Minecraft.getInstance().getSoundHandler().stop(null, SoundCategory.MUSIC);
                    Minecraft.getInstance().getSoundHandler().stop(null, SoundCategory.RECORDS);
                    stopPlaying();
                    nowURL = new URL(message.replace("[Play]", ""));
                    nowURL = Get(nowURL);
                    if (nowURL == null)
                        return;
                    nowPlaying.SetMusic(nowURL);
                    nowPlaying.play();
                } else if (message.startsWith("[Lyric]")) {
                    Hud.Lyric = message.substring(7);
                } else if (message.startsWith("[Info]")) {
                    Hud.Info = message.substring(6);
                } else if (message.startsWith("[Img]")) {
                    Hud.SetImg(message.substring(5));
                } else if (message.startsWith("[List]")) {
                    Hud.List = message.substring(6);
                } else if (message.equalsIgnoreCase("[clear]")) {
                    Hud.Lyric = Hud.Info = Hud.List = "";
                } else if (message.startsWith("{")) {
                    Hud.Set(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        asyncThread.start();
    }

    @SubscribeEvent
    public void onRed(final TickEvent.RenderTickEvent e) {
        Hud.update();
    }

    private void stopPlaying() {
        try {
            nowPlaying.close();
            Hud.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
