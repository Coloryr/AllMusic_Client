/*
 * 11/19/04 1.0 moved to LGPL.
 * -----------------------------------------------------------------------
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Library General Public License for more details.
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * ----------------------------------------------------------------------
 */

package com.coloryr.allmusic.client.core.player.decoder.mp3;

import java.io.InputStream;

/**
 * The <code>JavaLayerHooks</code> class allows developers to change
 * the way the JavaLayer library uses Resources.
 */

public interface JavaLayerHook {

    /**
     * Retrieves the named resource. This allows resources to be
     * obtained without specifying how they are retrieved.
     */
    InputStream getResourceAsStream(String name);
}
