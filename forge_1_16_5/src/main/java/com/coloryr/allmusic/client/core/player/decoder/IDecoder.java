package com.coloryr.allmusic.client.core.player.decoder;

public interface IDecoder {

    com.coloryr.allmusic.client.player.decoder.BuffPack decodeFrame() throws Exception;

    void close() throws Exception;

    boolean set() throws Exception;

    int getOutputFrequency();

    int getOutputChannels();

    void set(int time);
}
