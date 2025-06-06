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

// psychoacoustic setup
class PsyInfo {

    int athp;
    int decayp;
    int smoothp;
    int noisefitp;
    int noisefit_subblock;
    float noisefit_threshdB;

    float ath_att;

    int tonemaskp;
    float[] toneatt_125Hz = new float[5];
    float[] toneatt_250Hz = new float[5];
    float[] toneatt_500Hz = new float[5];
    float[] toneatt_1000Hz = new float[5];
    float[] toneatt_2000Hz = new float[5];
    float[] toneatt_4000Hz = new float[5];
    float[] toneatt_8000Hz = new float[5];

    int peakattp;
    float[] peakatt_125Hz = new float[5];
    float[] peakatt_250Hz = new float[5];
    float[] peakatt_500Hz = new float[5];
    float[] peakatt_1000Hz = new float[5];
    float[] peakatt_2000Hz = new float[5];
    float[] peakatt_4000Hz = new float[5];
    float[] peakatt_8000Hz = new float[5];

    int noisemaskp;
    float[] noiseatt_125Hz = new float[5];
    float[] noiseatt_250Hz = new float[5];
    float[] noiseatt_500Hz = new float[5];
    float[] noiseatt_1000Hz = new float[5];
    float[] noiseatt_2000Hz = new float[5];
    float[] noiseatt_4000Hz = new float[5];
    float[] noiseatt_8000Hz = new float[5];

    float max_curve_dB;

    float attack_coeff;
    float decay_coeff;

    void free() {
    }
}
