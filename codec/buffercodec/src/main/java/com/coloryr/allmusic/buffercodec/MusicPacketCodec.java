package com.coloryr.allmusic.buffercodec;

import com.coloryr.allmusic.codec.CommandType;
import com.coloryr.allmusic.codec.MusicPack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class MusicPacketCodec {
    public static final CommandType[] types = CommandType.values();

    public static ByteBuf pack(MusicPack pack) {
        ByteBuf buf = Unpooled.buffer(0);
        pack(buf, pack);
        return buf;
    }

    public static void pack(ByteBuf buf, MusicPack pack) {
        buf.writeByte(pack.type.ordinal());
        switch (pack.type) {
            case HUD_DATA:
            case LIST:
            case INFO:
            case IMG:
            case PLAY:
                MusicPack.StringMusicPack pack1 = (MusicPack.StringMusicPack) pack;
                writeString(buf, pack1.data);
                break;
            case LYRIC:
                MusicPack.LyricMusicPack pack2 = (MusicPack.LyricMusicPack) pack;
                writeString(buf, pack2.lyric);
                writeString(buf, pack2.tlyric);
                writeString(buf, pack2.klyric);
                break;
            case LYRIC_STATE:
                MusicPack.FloatMusicPack pack3 = (MusicPack.FloatMusicPack) pack;
                buf.writeFloat(pack3.data);
                break;
            case POS:
                MusicPack.IntMusicPack pack4 = (MusicPack.IntMusicPack) pack;
                buf.writeInt(pack4.data);
                break;
        }
    }

    private static void writeString(ByteBuf buf, String text) {
        byte[] temp = text.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(temp.length);
        buf.writeBytes(temp);
    }

    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    public static MusicPack decode(ByteBuf buf) {
        byte type = buf.readByte();
        if (type >= types.length || type < 0) {
            buf.clear();
            return new MusicPack(CommandType.STOP);
        }
        CommandType type1 = types[type];
        MusicPack pack;
        switch (type1) {
            case INFO:
            case LIST:
            case PLAY:
            case IMG:
            case HUD_DATA:
                pack = new MusicPack.StringMusicPack(type1, readString(buf));
                break;
            case LYRIC:
                pack = new MusicPack.LyricMusicPack(readString(buf), readString(buf), readString(buf));
                break;
            case LYRIC_STATE:
                pack = new MusicPack.FloatMusicPack(type1, buf.readFloat());
                break;
            case POS:
                pack = new MusicPack.IntMusicPack(type1, buf.readInt());
                break;
            default:
                pack = new MusicPack(type1);
        }
        buf.clear();

        return pack;
    }
}
