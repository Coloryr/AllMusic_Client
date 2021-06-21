/*
 * 11/19/04		1.0 moved to LGPL.
 * 29/01/00		Initial version. mdm@techie.com
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package Color_yr.AllMusic.player;

import Color_yr.AllMusic.player.decoder.BuffPack;
import Color_yr.AllMusic.player.decoder.IDecoder;
import Color_yr.AllMusic.player.decoder.flac.DataFormatException;
import Color_yr.AllMusic.player.decoder.flac.FlacDecoder;
import Color_yr.AllMusic.player.decoder.mp3.Mp3Decoder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.sound.sampled.FloatControl;
import java.net.URL;

/**
 * The <code>Player</code> class implements a simple player for playback
 * of an MPEG audio stream.
 *
 * @author Mat McGowan
 * @since 0.0.8
 */

// REVIEW: the audio device should not be opened until the
// first MPEG audio frame has been decoded. 
public class APlayer {

    private HttpClient client;
    private SoundAudioDevice audio;
    private IDecoder decoder;
    private boolean isClose;

    public APlayer() {
        try {
            client = HttpClientBuilder.create().useSystemProperties().build();
            audio = new SoundAudioDevice();
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
            audio.open(decoder);
            isClose = false;
        }
    }

    public void Set(int a) {
        if (audio == null)
            return;
        FloatControl temp = audio.getVolctrl();
        if (temp != null) {
            float temp1 = (a == 0) ? -80.0f : ((float) (a * 0.2 - 35.0));
            temp.setValue(temp1);
        }
    }

    public void play() throws Exception {
        boolean ret = true;
        while (ret) {
            ret = decodeFrame();
        }
        synchronized (this) {
            close();
        }
    }

    public void close() throws Exception {
        isClose = true;
        if (decoder != null)
            decoder.close();
        if (audio != null)
            audio.close();
    }

    protected boolean decodeFrame() {
        try {
            if (audio == null)
                return false;
            if (isClose)
                return false;

            BuffPack output = decoder.decodeFrame();
            if (output == null)
                return false;

            synchronized (this) {
                if (audio != null && !isClose) {
                    audio.write(output.buff, output.len);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
