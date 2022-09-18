package coloryr.allmusic.player.decoder;

import org.apache.http.client.HttpClient;

import java.net.URL;

public interface IDecoder {
    BuffPack decodeFrame() throws Exception;

    void close() throws Exception;

    void set() throws Exception;

    int getOutputFrequency();

    int getOutputChannels();

    void set(int time);
}
