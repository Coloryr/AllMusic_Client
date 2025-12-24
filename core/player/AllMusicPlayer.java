package com.coloryr.allmusic.client.core.player;

import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.player.decoder.BuffPack;
import com.coloryr.allmusic.client.core.player.decoder.IDecoder;
import com.coloryr.allmusic.client.core.player.decoder.flac.FlacDecoder;
import com.coloryr.allmusic.client.core.player.decoder.mp3.Mp3Decoder;
import com.coloryr.allmusic.client.core.player.decoder.ogg.OggDecoder;
import okhttp3.*;

import java.net.SocketTimeoutException;
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
    private OkHttpClient client;
    private String url;
    private Request request;
    private Response response;
    private InputStream content;
    private boolean isClose = false;
    private boolean reload = false;
    private IDecoder decoder;
    private int time = 0;
    private long local = 0;
    private boolean isPlay = false;
    private boolean wait = false;
    private int index = -1;
    private int frequency;
    private int channels;
    private IntBuffer source;

    public AllMusicPlayer(IntBuffer source) {
        try {
            this.source = source;
            new Thread(this::run, "allmusic_run").start();
            client = new OkHttpClient.Builder()
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
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52")
                        .header("Host", "music.163.com")
                        .build();
                Call call = client.newCall(request);
                Response response = call.execute();
                if (response.code() == 302) {
                    String location = response.header("Location");
                    if (location != null) {
                        response.close();
                        return location;
                    }
                }
                response.close();
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
        request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52")
                .header("Range", "bytes=" + local + "-")
                .build();
        response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("Response body is null");
        }
        content = body.byteStream();
    }

    private void run() {
        while (true) {
            try {
                semaphore.acquire();

                if (index == -1) {
                    index = AL10.alGenSources();
                    if (index == 0 && source != null) {
                        index = source.get(0);
                        if (index == 0) {
                            AllMusicCore.bridge.sendMessage("音频源创建失败");
                            return;
                        }
                    }
                }

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
                    AllMusicCore.bridge.sendMessage("获取音乐失败");
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
                            AllMusicCore.bridge.sendMessage("不支持这样的文件播放");
                            continue;
                        }
                    }
                }

                isPlay = true;
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
        // OkHttp doesn't have an abort method for requests, but we can cancel the call
        // Since we store the response, we can close it
        if (response != null) {
            response.close();
            response = null;
        }
        request = null;
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
        } catch (SocketTimeoutException | SocketException ex) {
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
