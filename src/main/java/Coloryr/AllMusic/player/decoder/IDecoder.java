package Coloryr.AllMusic.player.decoder;

import org.apache.http.client.HttpClient;

import java.net.URL;

public interface IDecoder {
    BuffPack decodeFrame() throws Exception;

    void close() throws Exception;

    void set(HttpClient client, URL url) throws Exception;

    int getOutputFrequency();

    int getOutputChannels();
}
