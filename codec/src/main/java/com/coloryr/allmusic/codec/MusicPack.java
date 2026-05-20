package com.coloryr.allmusic.codec;

public class MusicPack {
    public CommandType type;

    public MusicPack(CommandType type) {
        this.type = type;
    }

    public static class StringMusicPack extends MusicPack {
        public String data;

        public StringMusicPack(CommandType type, String data) {
            super(type);
            this.data = data;
        }
    }

    public static class IntMusicPack extends MusicPack {
        public int data;

        public IntMusicPack(CommandType type, int data) {
            super(type);
            this.data = data;
        }
    }

    public static class LyricMusicPack extends MusicPack {
        public String lyric;
        public String tlyric;
        public String klyric;

        public LyricMusicPack(String lyric, String tlyric, String klyric) {
            super(CommandType.LYRIC);
            this.lyric = lyric;
            this.tlyric = tlyric;
            this.klyric = klyric;
        }
    }

    public static class FloatMusicPack extends MusicPack {
        public float data;

        public FloatMusicPack(CommandType type, float data) {
            super(type);
            this.data = data;
        }
    }
}
