/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
 * JOrbis
 * Copyright (C) 2000 ymnk, JCraft,Inc.
 * Written by: 2000 ymnk<ymnk@jcraft.com>
 * Many thanks to
 * Monty <monty@xiph.org> and
 * The XIPHOPHORUS Company http://www.xiph.org/ .
 * JOrbis has been based on their awesome works, Vorbis codec.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Library General Public License for more details.
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.coloryr.allmusic.client.core.player.decoder.ogg.jcraft.jorbis;

import com.coloryr.allmusic.client.core.player.decoder.ogg.jcraft.jogg.Buffer;

abstract class FuncMapping {

    public static FuncMapping[] mapping_P = {new Mapping0()};

    abstract void pack(Info info, Object imap, Buffer buffer);

    abstract Object unpack(Info info, Buffer buffer);

    abstract Object look(DspState vd, InfoMode vm, Object m);

    abstract void free_info(Object imap);

    abstract void free_look(Object imap);

    abstract int inverse(Block vd, Object lm);
}
