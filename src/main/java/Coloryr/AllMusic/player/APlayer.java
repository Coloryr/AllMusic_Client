package Coloryr.AllMusic.player;

import Coloryr.AllMusic.AllMusic;
import Coloryr.AllMusic.player.decoder.BuffPack;
import Coloryr.AllMusic.player.decoder.IDecoder;
import Coloryr.AllMusic.player.decoder.flac.DataFormatException;
import Coloryr.AllMusic.player.decoder.flac.FlacDecoder;
import Coloryr.AllMusic.player.decoder.mp3.Mp3Decoder;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class APlayer {

    private HttpClient client;
    private boolean isClose;
    private final List<URL> urls = new ArrayList<>();

    public APlayer() {
        try {
            new Thread(this::run, "allmusic_run").start();
            client = HttpClientBuilder.create().useSystemProperties().build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() {
        while (true) {
            try {
                if (urls.size() > 0) {
                    AllMusic.isPlay = true;
                    URL url = urls.remove(urls.size() - 1);
                    urls.clear();
                    IDecoder decoder;
                    try {
                        decoder = new FlacDecoder();
                        decoder.set(client, url);
                    } catch (DataFormatException e) {
                        decoder = new Mp3Decoder();
                        decoder.set(client, url);
                    }
                    AudioFormat audioformat = new AudioFormat(decoder.getOutputFrequency(),
                            16,
                            decoder.getOutputChannels(),
                            true,
                            false);
                    int index = AL10.alGenSources();
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

                            IntBuffer intBuffer;

                            intBuffer = BufferUtils.createIntBuffer(1);
                            AL10.alGenBuffers(intBuffer);

                            int soundFormat;
                            if (audioformat.getChannels() == 1) {
                                if (audioformat.getSampleSizeInBits() == 8) {
                                    soundFormat = AL10.AL_FORMAT_MONO8;
                                } else if (audioformat.getSampleSizeInBits() == 16) {
                                    soundFormat = AL10.AL_FORMAT_MONO16;
                                } else {
                                    break;
                                }
                            } else if (audioformat.getChannels() == 2) {
                                if (audioformat.getSampleSizeInBits() == 8) {
                                    soundFormat = AL10.AL_FORMAT_STEREO8;
                                } else if (audioformat.getSampleSizeInBits() == 16) {
                                    soundFormat = AL10.AL_FORMAT_STEREO16;
                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }

                            AL10.alBufferData(intBuffer.get(0), soundFormat, byteBuffer, (int) audioformat.getSampleRate());
                            AL10.alSourcef(index, AL10.AL_GAIN, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS));

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
                            AL10.alSourcef(index, AL10.AL_GAIN, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS));
                            Thread.sleep(10);
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

    public void SetMusic(URL url) {
        urls.add(url);
        isClose = true;
    }

    public void close() {
        isClose = true;
    }
}
