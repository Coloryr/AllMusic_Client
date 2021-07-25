package Color_yr.AllMusic.player;

import Color_yr.AllMusic.player.decoder.BuffPack;
import Color_yr.AllMusic.player.decoder.IDecoder;
import Color_yr.AllMusic.player.decoder.flac.DataFormatException;
import Color_yr.AllMusic.player.decoder.flac.FlacDecoder;
import Color_yr.AllMusic.player.decoder.mp3.Mp3Decoder;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class APlayer {

    private HttpClient client;
    private IDecoder decoder;
    private boolean isClose;
    private AudioFormat audioformat;
    private int index;

    public APlayer() {
        try {
            client = HttpClientBuilder.create().useSystemProperties().build();
            isClose = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SetMusic(URL url) throws Exception {
        synchronized (this) {
            try {
                decoder = new FlacDecoder();
                decoder.set(client, url);
            } catch (DataFormatException e) {
                decoder = new Mp3Decoder();
                decoder.set(client, url);
            }
            audioformat = new AudioFormat(decoder.getOutputFrequency(),
                    16,
                    decoder.getOutputChannels(),
                    true,
                    false);
            index = AL10.alGenSources();
            isClose = false;
        }
    }

    public void play() throws Exception {
        while (true) {
            try {
                if (isClose)
                    break;

                var output = decoder.decodeFrame();
                if (output == null)
                    break;

                // Stream buffers can only be queued for streaming sources:

                var byteBuffer = BufferUtils.createByteBuffer(
                        output.len).put(output.buff, 0, output.len).flip();

                IntBuffer intBuffer;

                // Clear out any previously queued buffers:
                intBuffer = BufferUtils.createIntBuffer(1);
                AL10.alGenBuffers(intBuffer);

                int soundFormat = 0;
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

                AL10.alSourceQueueBuffers(index, intBuffer);
                AL10.alSourcef(index, AL10.AL_GAIN, Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER));
                if (AL10.alGetSourcei(index,
                        AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                    AL10.alSourcePlay(index);
                }

            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        if (!isClose)
            if (decoder != null) {
                decoder.close();
                decoder = null;
            }
        while (AL10.alGetSourcei(index,
                AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
            Thread.sleep(10);
        }
        if (!isClose)
            close();
    }

    public void close() throws Exception {
        isClose = true;
        AL10.alSourceStop(index);
        int m_numqueued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);
        while (m_numqueued > 0) {
            int temp = AL10.alSourceUnqueueBuffers(index);
            AL10.alDeleteBuffers(temp);
            m_numqueued--;
        }
        AL10.alDeleteSources(index);
        if (decoder != null)
            decoder.close();
    }
}
