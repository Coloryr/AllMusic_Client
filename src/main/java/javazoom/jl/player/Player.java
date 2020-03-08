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

package javazoom.jl.player;

import javazoom.jl.decoder.*;

import javax.sound.sampled.FloatControl;
import java.io.InputStream;

/**
 * The <code>Player</code> class implements a simple player for playback
 * of an MPEG audio stream.
 *
 * @author Mat McGowan
 * @since 0.0.8
 */

// REVIEW: the audio device should not be opened until the
// first MPEG audio frame has been decoded. 
public class Player {

    private Bitstream bitstream;
    private Decoder decoder;
    private AudioDevice audio;
    private boolean isClose;

    public Player() {
        try {
            FactoryRegistry r = FactoryRegistry.systemRegistry();
            audio = r.createAudioDevice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void SetMusic(InputStream stream) throws JavaLayerException {
        bitstream = new Bitstream(stream);
        decoder = new Decoder();
        audio.open(decoder);
        isClose = false;
    }

    public synchronized void Set(float a) {
        if (audio == null)
            return;
        FloatControl temp = audio.getVolctrl();
        if (temp != null) {
            temp.setValue(a);
        }
    }

    public void play() {
        boolean ret = true;

        while (ret) {
            ret = decodeFrame();
        }

        if (audio != null) {
            audio.flush();
            synchronized (this) {
                close();
            }
        }
    }

    public synchronized void close() {
        try {
            isClose = true;
            if (bitstream != null)
                bitstream.close();
        } catch (BitstreamException ex) {
        }
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
                bitstream.closeFrame();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }
}
