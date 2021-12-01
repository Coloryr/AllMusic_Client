/*
 * FLAC library (Java)
 *
 * Copyright (c) Project Nayuki
 * https://www.nayuki.io/page/flac-library-java
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (see COPYING.txt and COPYING.LESSER.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */

package coloryr.allmusic.player.decoder.flac;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.net.URL;


/**
 * A FLAC input stream based on a {@link RandomAccessFile}.
 */
public final class SeekableFileFlacInput extends AbstractFlacLowLevelInput {

    /*---- Fields ----*/

    // The underlying byte-based input stream to read from.
    private BufferedInputStream raf;
    private final HttpGet get;
    private final HttpClient client;
    private InputStream content;
    private long local;


    /*---- Constructors ----*/

    public SeekableFileFlacInput(HttpClient client, URL url) throws Exception {
        super();
        this.client = client;
        this.get = new HttpGet(url.toString());
        this.get.setHeader("Range", "bytes=" + local + "-");
        HttpResponse response = this.client.execute(get);
        HttpEntity entity = response.getEntity();
        content = entity.getContent();
        this.raf = new BufferedInputStream(content);
    }

    /*---- Methods ----*/

    public long getLength() {
        try {
            return raf.available();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected int readUnderlying(byte[] buf, int off, int len) throws IOException {
        try {
            int temp = raf.read(buf, off, len);
            local += temp;
            return temp;
        } catch (ConnectionClosedException | SocketException ex) {
            this.get.setHeader("Range", "bytes=" + local + "-");
            HttpResponse response = this.client.execute(get);
            HttpEntity entity = response.getEntity();
            content = entity.getContent();
            this.raf = new BufferedInputStream(content);
            return readUnderlying(buf, off, len);
        }
    }

    // Closes the underlying RandomAccessFile stream (very important).
    public void close() throws IOException {
        get.abort();
        if (raf != null) {
            raf.close();
            raf = null;
            super.close();
        }
    }
}
