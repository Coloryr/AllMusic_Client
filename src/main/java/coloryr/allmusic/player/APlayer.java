package coloryr.allmusic.player;

import coloryr.allmusic.AllMusic;
import coloryr.allmusic.player.decoder.BuffPack;
import coloryr.allmusic.player.decoder.IDecoder;
import coloryr.allmusic.player.decoder.flac.DataFormatException;
import coloryr.allmusic.player.decoder.flac.FlacDecoder;
import coloryr.allmusic.player.decoder.mp3.Mp3Decoder;
import coloryr.allmusic.player.decoder.ogg.OggDecoder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class APlayer {

    private HttpClient client;
    private boolean isClose;
    private IDecoder decoder;
    private final List<String> urls = new ArrayList<>();
    private int time;
    private String url;

    private final Semaphore semaphore = new Semaphore(0);
    private int index = -1;

    public APlayer() {
        try {
            new Thread(this::run, "allmusic_run").start();
            client = HttpClientBuilder.create().useSystemProperties().build();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void set(String time) {
        try {
            int time1 = Integer.parseInt(time);
            set(time1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void set(int time) {
        close();
        this.time = time;
        urls.add(url);
        semaphore.release();
    }

    private void run() {
        while (true) {
            try {
                semaphore.acquire();
                if (urls.size() > 0) {
                    url = urls.remove(urls.size() - 1);
                    urls.clear();
                    URL nowURL = new URL(url);
                    nowURL = Get(nowURL);
                    if (nowURL == null)
                        continue;
                    try {
                        decoder = new FlacDecoder();
                        decoder.set(client, nowURL);
                    } catch (DataFormatException e) {
                        try {
                            decoder = new OggDecoder();
                            decoder.set(client, nowURL);
                        } catch (DataFormatException e1) {
                            try {
                                decoder = new Mp3Decoder();
                                decoder.set(client, nowURL);
                            } catch (DataFormatException e2) {
                                AllMusic.sendMessage("[AllMusic客户端]不支持这样的文件播放");
                                continue;
                            }
                        }
                    }

                    AllMusic.isPlay = true;
                    index = AL10.alGenSources();
                    int frequency = decoder.getOutputFrequency();
                    int channels = decoder.getOutputChannels();
                    if (time != 0) {
                        decoder.set(time);
                    }
                    isClose = false;
                    while (true) {
                        try {
                            if (isClose)
                                break;
                            BuffPack output = decoder.decodeFrame();
                            if (output == null)
                                break;

                            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(
                                    output.len).put(output.buff, 0, output.len);
                            ((Buffer) byteBuffer).flip();
                            IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
                            AL10.alGenBuffers(intBuffer);

                            if (channels != 1 && channels != 2)
                                break;

                            AL10.alBufferData(intBuffer.get(0), channels == 1
                                    ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, byteBuffer, frequency);
                            AL10.alSourcef(index, AL10.AL_GAIN, AllMusic.getVolume());

                            AL10.alSourceQueueBuffers(index, intBuffer);
                            if (AL10.alGetSourcei(index,
                                    AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                                AL10.alSourcePlay(index);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    try {
                        while (!isClose && AL10.alGetSourcei(index,
                                AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                            AL10.alSourcef(index, AL10.AL_GAIN, AllMusic.getVolume());
                            Thread.sleep(100);
                        }
                        AL10.alSourceStop(index);
                        int m_numqueued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);
                        while (m_numqueued > 0) {
                            int temp = AL10.alSourceUnqueueBuffers(index);
                            AL10.alDeleteBuffers(temp);
                            m_numqueued--;
                        }
                        AL10.alDeleteSources(index);
                        decoder.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    AllMusic.isPlay = false;
                } else {
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setMusic(String url) {
        time = 0;
        close();
        urls.add(url);
        semaphore.release();
    }

    public void close() {
        urls.clear();
        isClose = true;
        if (decoder != null) {
            try {
                decoder.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
