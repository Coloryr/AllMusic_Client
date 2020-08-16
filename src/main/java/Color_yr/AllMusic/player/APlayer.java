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

import Color_yr.AllMusic.AllMusic;
import Color_yr.AllMusic.decoder.Bitstream;
import Color_yr.AllMusic.decoder.Decoder;
import Color_yr.AllMusic.decoder.Header;
import Color_yr.AllMusic.decoder.SampleBuffer;
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
    private Bitstream bitstream;
    private SoundAudioDevice audio;
    private Decoder decoder;
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
            bitstream = new Bitstream(client, url);
            decoder = new Decoder();
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
            AllMusic.v = a;
        }
    }

    public void play() throws Exception {
        boolean ret = true;
        AllMusic.isPlay = true;
        while (ret) {
            ret = decodeFrame();
        }
        synchronized (this) {
            close();
        }
    }

    public void close() throws Exception {
        isClose = true;
        if (bitstream != null)
            bitstream.close();
        if (audio != null)
            audio.close();
        AllMusic.isPlay = false;
    }

    protected boolean decodeFrame() {
        try {
            if (audio == null)
                return false;
            if (isClose)
                return false;
            Header h = bitstream.readFrame();

            if (h == null)
                return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);

            synchronized (this) {
                if (audio != null && !isClose) {
                    audio.write(output.getBuffer(), 0, output.getBufferLength());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        bitstream.closeFrame();
        return true;
    }
}
