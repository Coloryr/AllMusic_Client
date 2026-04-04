package com.coloryr.allmusic.client.core.player;

import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.objs.PlayTaskObj;
import com.coloryr.allmusic.client.core.player.decoder.BuffPack;
import com.coloryr.allmusic.client.core.player.decoder.IDecoder;
import com.coloryr.allmusic.client.core.player.decoder.m4a.M4ADecoder;
import com.coloryr.allmusic.client.core.player.decoder.mp3.Mp3Decoder;
import com.coloryr.allmusic.client.core.player.decoder.ogg.OggDecoder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.io.CloseMode;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AllMusicPlayer extends InputStream {

    private final Stack<PlayTaskObj> tasks = new Stack<>();
    private final Semaphore semaphore = new Semaphore(0);
    private final Semaphore semaphore1 = new Semaphore(0);
    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();

    private PlayTaskObj nowTask;
    private CloseableHttpResponse response;
    private BufferedInputStream content;
    private volatile boolean isClose = false;
    private volatile boolean reload = false;
    private IDecoder decoder;
    private volatile boolean isPlay = false;
    private boolean wait = false;
    private int index = -1;
    private int frequency;
    private int channels;
    private IntBuffer source;
    private long local;
    private volatile boolean isRun;
    private ScheduledExecutorService scheduler;

    public AllMusicPlayer(IntBuffer source) {
        try {
            this.source = source;
            new Thread(this::run, "allmusic_run").start();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::run1, 0, 10, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        closePlayer();
        // 完全清理 OpenAL 资源
        fullCleanup();
    }

    private void fullCleanup() {
        if (index != -1) {
            // 1. 停止源
            AL10.alSourceStop(index);

            // 2. 【关键】解绑所有缓冲区
            AL10.alSourcei(index, AL10.AL_BUFFER, AL10.AL_NONE);

            // 3. 循环清理所有队列中的缓冲区，直到完全清空
            int queued;
            while ((queued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED)) > 0) {
                int buffer = AL10.alSourceUnqueueBuffers(index);
                if (buffer != 0) {
                    AL10.alDeleteBuffers(buffer);
                }
            }

            // 4. 清理已处理的缓冲区
            int processed;
            while ((processed = AL10.alGetSourcei(index, AL10.AL_BUFFERS_PROCESSED)) > 0) {
                int buffer = AL10.alSourceUnqueueBuffers(index);
                if (buffer != 0) {
                    AL10.alDeleteBuffers(buffer);
                }
            }
        }
        queue.clear();
    }

    public void run1() {
        if (isPlay && nowTask != null) {
            nowTask.time += 10;
        }
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setTime(int time) {
        if (nowTask == null) {
            return;
        }
        String url = nowTask.url;
        closePlayer();
        PlayTaskObj task = new PlayTaskObj();
        task.url = url;
        task.time = time;
        tasks.push(task);
        semaphore.release();
    }

    public void connect() throws IOException {
        streamClose();
        HttpGet request = new HttpGet(nowTask.url);
        request.setHeader("Range", "bytes=" + local + "-");
        response = AllMusicCore.client.execute(request);
        int statusCode = response.getCode();
        if (statusCode < 200 || statusCode >= 400) {
            throw new IOException("Unexpected code " + statusCode);
        }
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new IOException("Response entity is null");
        }
        content = new BufferedInputStream(entity.getContent());
    }

    private void resetSource() {
        if (index != -1) {
            // 完全重置源状态 - 这是消除杂音的关键
            AL10.alSourceStop(index);
            AL10.alSourcei(index, AL10.AL_BUFFER, AL10.AL_NONE);

            // 循环清理直到队列为空
            int queued;
            do {
                queued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);
                if (queued > 0) {
                    int buffer = AL10.alSourceUnqueueBuffers(index);
                    if (buffer != 0) {
                        AL10.alDeleteBuffers(buffer);
                    }
                }
            } while (queued > 0);

            // 重置音量等参数
            AL10.alSourcef(index, AL10.AL_GAIN, AllMusicCore.bridge.getVolume());
            AL10.alSourcef(index, AL10.AL_PITCH, 1.0f);
        }
        queue.clear();
    }

    private void run() {
        if (isRun) {
            return;
        }
        isRun = true;
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

                // 【关键】每次播放新歌前完全重置源
                resetSource();

                nowTask = tasks.pop();
                if (nowTask == null || nowTask.url == null || nowTask.url.isEmpty()) continue;
                tasks.clear();
                try {
                    local = 0;
                    connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    AllMusicCore.bridge.sendMessage("获取音乐失败");
                    continue;
                }

                byte[] head = new byte[4];
                content.mark(4);
                content.read(head);
                content.reset();

                if (head[0] == 0 && head[1] == 0 && head[2] == 0 && head[3] == 0x1c) {
                    decoder = new M4ADecoder(this);
                } else if (head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
                    decoder = new Mp3Decoder(this);
                } else if (head[0] == (byte) 0xFF && head[1] == (byte) 0xFB) {
                    decoder = new Mp3Decoder(this);
                } else {
                    decoder = new OggDecoder(this);
                }

                if (!decoder.set()) {
                    AllMusicCore.bridge.sendMessage("不支持这样的文件播放");
                    continue;
                }

                isPlay = true;
                frequency = decoder.getOutputFrequency();
                channels = decoder.getOutputChannels();
                if (channels != 1 && channels != 2) continue;
                if (nowTask.time != 0) {
                    decoder.set(nowTask.time);
                }
                queue.clear();
                reload = false;
                isClose = false;

                while (true) {
                    try {
                        if (isClose) break;

                        while (AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED) < AllMusicCore.config.queueSize) {
                            if (isClose) break;
                            BuffPack output = decoder.decodeFrame();
                            if (output == null) break;
                            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(output.len)
                                    .put(output.buff, 0, output.len);
                            byteBuffer.flip();
                            queue.add(byteBuffer);
                        }

                        Thread.sleep(5);
                    } catch (Exception e) {
                        if (!isClose) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }

                streamClose();
                decodeClose();

                while (!isClose && AL10.alGetSourcei(index, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                    Thread.sleep(50);
                }

                if (!reload) {
                    wait = true;
                    if (semaphore1.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                        if (reload) {
                            tasks.push(nowTask);
                            semaphore.release();
                            continue;
                        }
                    }
                    isPlay = false;
                    // 播放完成后也清理一下
                    AL10.alSourceStop(index);
                    AL10.alSourcei(index, AL10.AL_BUFFER, AL10.AL_NONE);
                    int queued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);
                    while (queued > 0) {
                        int buffer = AL10.alSourceUnqueueBuffers(index);
                        if (buffer != 0) {
                            AL10.alDeleteBuffers(buffer);
                        }
                        queued--;
                    }
                } else {
                    tasks.push(nowTask);
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
            return;
        }

        if (isPlay && index != -1) {
            // 设置音量
            float temp = AllMusicCore.bridge.getVolume();
            float now = AL10.alGetSourcef(index, AL10.AL_GAIN);
            if (now != temp) {
                AL10.alSourcef(index, AL10.AL_GAIN, temp);
            }

            // 清理已处理完的缓冲区（只清理，不删除，让 OpenAL 自己管理）
            int processed = AL10.alGetSourcei(index, AL10.AL_BUFFERS_PROCESSED);
            for (int i = 0; i < processed; i++) {
                AL10.alSourceUnqueueBuffers(index);  // 只出队，不删除
            }

            // 添加新的音频数据
            int count = 0;
            int queued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);

            while (!queue.isEmpty() && queued < AllMusicCore.config.queueSize) {
                count++;
                if (count > AllMusicCore.config.exitSize) break;

                ByteBuffer byteBuffer = queue.poll();
                if (byteBuffer == null) continue;
                if (isClose) return;

                IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
                AL10.alGenBuffers(intBuffer);
                int buffer = intBuffer.get(0);

                if (buffer == 0) continue;

                AL10.alBufferData(
                        buffer,
                        channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                        byteBuffer,
                        frequency);

                AL10.alSourceQueueBuffers(index, buffer);
                queued++;

                if (AL10.alGetSourcei(index, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                    AL10.alSourcePlay(index);
                }
            }
        } else if (index != -1) {
            // 不在播放状态，确保停止
            if (AL10.alGetSourcei(index, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                AL10.alSourceStop(index);
            }
        }
    }

    public void closePlayer() {
        isClose = true;
        queue.clear();
        nowTask = null;
    }

    public void setMusic(String url) {
        closePlayer();
        PlayTaskObj taskObj = new PlayTaskObj();
        taskObj.time = 0;
        taskObj.url = url;
        tasks.push(taskObj);
        semaphore.release();
    }

    private void streamClose() throws IOException {
        if (response != null) {
            response.close(CloseMode.IMMEDIATE);
            response = null;
        }
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
        local++;
        return content.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        int temp = content.read(buf);
        local += temp;
        return temp;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 2048) {
            long temp = content.skip(n);
            local += temp;
            return temp;
        } else {
            local += n;
            connect();
        }
        return n;
    }

    @Override
    public synchronized int read(byte[] buf, int off, int len) throws IOException {
        try {
            int temp = content.read(buf, off, len);
            local += temp;
            return temp;
        } catch (SocketTimeoutException | SocketException ex) {
            connect();
            return this.read(buf, off, len);
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