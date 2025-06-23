package com.coloryr.allmusic.client.core.player;

import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.player.decoder.flac.FlacDecoder;
import com.coloryr.allmusic.client.core.player.decoder.mp3.Mp3Decoder;
import com.coloryr.allmusic.client.player.decoder.BuffPack;
import com.coloryr.allmusic.client.player.decoder.IDecoder;
import com.coloryr.allmusic.client.core.player.decoder.ogg.OggDecoder;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Queue;
import java.util.concurrent.*;

public class AllMusicPlayer extends InputStream {

    private final Queue<String> urls = new ConcurrentLinkedQueue<>();
    private final Semaphore semaphore = new Semaphore(0);
    private final Semaphore semaphore1 = new Semaphore(0);
    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private HttpClient client;
    private String url;
    private HttpGet get;
    private InputStream content;
    private boolean isClose = false;
    private boolean reload = false;
    private IDecoder decoder;
    private int time = 0;
    private long local = 0;
    private boolean isPlay = false;
    private boolean wait = false;
    private int index;
    private int frequency;
    private int channels;

    public AllMusicPlayer() {
        try {
            new Thread(this::run, "allmusic_run").start();
            client = HttpClientBuilder.create()
                    .useSystemProperties()
                    .build();
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

    public String Get(String url) {
        if (url.contains("https://music.163.com/song/media/outer/url?id=")
                || url.contains("http://music.163.com/song/media/outer/url?id=")) {
            try {
                HttpGet get = new HttpGet(url);
                get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52");
                get.setHeader("Host", "music.163.com");
                HttpResponse response = client.execute(get);
                StatusLine line = response.getStatusLine();
                if (line.getStatusCode() == 302) {
                    return response.getFirstHeader("Location").getValue();
                }
                return url;
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
        if (url == null) {
            return;
        }
        closePlayer();
        this.time = time;
        urls.add(url);
        semaphore.release();
    }

    public void connect() throws IOException {
        getClose();
        streamClose();
        get = new HttpGet(url);
        get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52");
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
                if (url == null || url.isEmpty()) continue;
                urls.clear();
                url = Get(url);
                if (url == null) continue;
                try {
                    local = 0;
                    connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    AllMusicCore.bridge.sendMessage("[AllMusic客户端]获取音乐失败");
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
                            AllMusicCore.bridge.sendMessage("[AllMusic客户端]不支持这样的文件播放");
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
                if (channels != 1 && channels != 2) continue;
                if (time != 0) {
                    decoder.set(time);
                }
                queue.clear();
                reload = false;
                isClose = false;
                while (true) {
                    try {
                        if (isClose) break;
                        while (AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED) < AllMusicCore.config.queueSize) {
                            BuffPack output = decoder.decodeFrame();
                            if (output == null) break;
                            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(output.len)
                                    .put(output.buff, 0, output.len);
                            ((Buffer) byteBuffer).flip();
                            queue.add(byteBuffer);

                            AL10.alSourcef(index, AL10.AL_GAIN, AllMusicCore.bridge.getVolume());
                        }

                        AL10.alSourcef(index, AL10.AL_GAIN, AllMusicCore.bridge.getVolume());

                        if (AL10.alGetSourcei(index, AL10.AL_BUFFERS_PROCESSED) > 0) {
                            int temp = AL10.alSourceUnqueueBuffers(index);
                            AL10.alDeleteBuffers(temp);
                        }

                        Thread.sleep(10);
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
                while (!isClose && AL10.alGetSourcei(index, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                    AL10.alSourcef(index, AL10.AL_GAIN, AllMusicCore.bridge.getVolume());
                    Thread.sleep(50);
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
        int count = 0;
        if (wait) {
            wait = false;
            semaphore1.release();
        }
        if (isClose) {
            queue.clear();
            return;
        }
        while (!queue.isEmpty()) {
            count++;
            if (count > AllMusicCore.config.exitSize) break;
            ByteBuffer byteBuffer = queue.poll();
            if (byteBuffer == null) continue;
            if (isClose) return;
            IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
            AL10.alGenBuffers(intBuffer);

            AL10.alBufferData(
                    intBuffer.get(0),
                    channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                    byteBuffer,
                    frequency);
            AL10.alSourcef(index, AL10.AL_GAIN, AllMusicCore.bridge.getVolume());

            AL10.alSourceQueueBuffers(index, intBuffer);
            if (AL10.alGetSourcei(index, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
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
    public synchronized int read(byte[] buf, int off, int len) throws IOException {
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
