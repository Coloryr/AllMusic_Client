package com.coloryr.allmusic.comm;

import com.coloryr.allmusic.buffercodec.MusicPacketCodec;
import com.coloryr.allmusic.codec.MusicPack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MusicCodec(MusicPack pack) implements CustomPayload {
    public static final Identifier ChannelID = Identifier.of("allmusic", "channel");

    public static final Id<MusicCodec> ID = new Id<>(ChannelID);
    public static final PacketCodec<PacketByteBuf, MusicCodec> CODEC =
            PacketCodec.of((value, buf) -> MusicPacketCodec.pack(buf, value.pack),
                    buf -> new MusicCodec(MusicPacketCodec.decode(buf)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
