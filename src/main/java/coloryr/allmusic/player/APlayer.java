package coloryr.allmusic.player;

import coloryr.allmusic.AllMusic;
import coloryr.allmusic.player.decoder.BuffPack;
import coloryr.allmusic.player.decoder.IDecoder;
import coloryr.allmusic.player.decoder.flac.DataFormatException;
import coloryr.allmusic.player.decoder.flac.FlacDecoder;
import coloryr.allmusic.player.decoder.mp3.Mp3Decoder;
import coloryr.allmusic.player.decoder.ogg.OggDecoder;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class APlayer extends InputStream {

    private HttpClient client;
    private String url;
    private HttpGet get;
    private InputStream content;

    private boolean isClose = false;
    private boolean reload = false;
    private IDecoder decoder;
    private final Queue<String> urls = new ConcurrentLinkedQueue<>();
    private int time = 0;
    private long local = 0;
    private final Semaphore semaphore = new Semaphore(0);
    private final Semaphore semaphore1 = new Semaphore(0);
    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private boolean isPlay = false;
    private boolean wait = false;
    private int index;
    private int frequency;
    private int channels;

    public APlayer() {
        try {
            new Thread(this::run, "allmusic_run").start();
            client = HttpClientBuilder.create().useSystemProperties().build();
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.scheduleAtFixedRate(this::run1, 0, 10, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void run1() {
        if (isPlay) {
            time += 10;
        }
    }

    public boolean isPlay() {
        return isPlay;
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
        closePlayer();
        this.time = time;
        urls.add(url);
        semaphore.release();
    }

    public void connect() throws IOException {
        getClose();
        streamClose();
        get = new HttpGet(url);
        get.setHeader("Range", "bytes=" + local + "-");
        HttpResponse response = this.client.execute(get);
        HttpEntity entity = response.getEntity();
        content = entity.getContent();
    }

    private void run() {
        while (true) {
            try {
                semaphore.acquire();
                url = urls.poll();
                if (url == null || url.isEmpty())
                    continue;
                urls.clear();
                URL nowURL = new URL(url);
                nowURL = Get(nowURL);
                if (nowURL == null)
                    continue;
                try {
                    local = 0;
                    connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    AllMusic.sendMessage("[AllMusic客户端]获取音乐失败");
                    continue;
                }

                decoder = new FlacDecoder(this);
                if (!decoder.set()) {
                    local = 0;
                    connect();
                    decoder = new OggDecoder(this);
                    if (!decoder.set()) {
                        local = 0;
                        connect();
                        decoder = new Mp3Decoder(this);
                        if (!decoder.set()) {
                            AllMusic.sendMessage("[AllMusic客户端]不支持这样的文件播放");
                            continue;
                        }
                    }
                }

                isPlay = true;
                index = AL10.alGenSources();
                int m_numqueued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);
                while (m_numqueued > 0) {
                    int temp = AL10.alSourceUnqueueBuffers(index);
                    AL10.alDeleteBuffers(temp);
                    m_numqueued--;
                }
                frequency = decoder.getOutputFrequency();
                channels = decoder.getOutputChannels();
                if (channels != 1 && channels != 2)
                    continue;
                if (time != 0) {
                    decoder.set(time);
                }
                queue.clear();
                reload = false;
                isClose = false;
                while (true) {
                    try {
                        if (isClose) break;
                        BuffPack output = decoder.decodeFrame();
                        if (output == null) break;
                        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(
                                output.len).put(output.buff, 0, output.len);
                        ((Buffer) byteBuffer).flip();
                        queue.add(byteBuffer);
                    } catch (Exception e) {
                        if (!isClose) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
                getClose();
                streamClose();
                decodeClose();
                while (!isClose && AL10.alGetSourcei(index,
                        AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                    AL10.alSourcef(index, AL10.AL_GAIN, AllMusic.getVolume());
                    Thread.sleep(100);
                }
                if (!reload) {
                    wait = true;
                    if (semaphore1.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                        if (reload) {
                            urls.add(url);
                            semaphore.release();
                            continue;
                        }
                    }
                    isPlay = false;
                    AL10.alSourceStop(index);
                    m_numqueued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);
                    while (m_numqueued > 0) {
                        int temp = AL10.alSourceUnqueueBuffers(index);
                        AL10.alDeleteBuffers(temp);
                        m_numqueued--;
                    }
                    AL10.alDeleteSources(index);
                } else {
                    urls.add(url);
                    semaphore.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void tick() {
        if (wait) {
            wait = false;
            semaphore1.release();
        }
        if (isClose) {
            queue.clear();
            return;
        }
        while (!queue.isEmpty()) {
            ByteBuffer byteBuffer = queue.poll();
            if (byteBuffer == null)
                continue;
            if (isClose)
                return;
            IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
            AL10.alGenBuffers(intBuffer);

            AL10.alBufferData(intBuffer.get(0), channels == 1
                    ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, byteBuffer, frequency);
            AL10.alSourcef(index, AL10.AL_GAIN, AllMusic.getVolume());

            AL10.alSourceQueueBuffers(index, intBuffer);
            if (AL10.alGetSourcei(index,
                    AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alSourcePlay(index);
            }
        }
    }

    public void closePlayer() {
        isClose = true;
    }

    public void setMusic(String url) {
        time = 0;
        closePlayer();
        urls.add(url);
        semaphore.release();
    }

    private void getClose() {
        if (get != null && !get.isAborted()) {
            get.abort();
            get = null;
        }
    }

    private void streamClose() throws IOException {
        if (content != null) {
            content.close();
            content = null;
        }
    }

    private void decodeClose() throws Exception {
        if (decoder != null) {
            decoder.close();
            decoder = null;
        }
    }


    @Override
    public int read() throws IOException {
        return content.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return content.read(buf);
    }

    @Override
    public synchronized int read(byte[] buf, int off, int len)
            throws IOException {
        try {
            int temp = content.read(buf, off, len);
            local += temp;
            return temp;
        } catch (ConnectionClosedException | SocketException ex) {
            connect();
            return read(buf, off, len);
        }
    }

    @Override
    public synchronized int available() throws IOException {
        return content.available();
    }

    @Override
    public void close() throws IOException {
        streamClose();
    }

    public void setLocal(long local) throws IOException {
        getClose();
        streamClose();
        this.local = local;
        connect();
    }

    public void setReload() {
        if (isPlay) {
            reload = true;
            isClose = true;
        }
    }
}
