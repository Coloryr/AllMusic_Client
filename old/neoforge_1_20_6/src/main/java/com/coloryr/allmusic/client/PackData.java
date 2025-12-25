package com.coloryr.allmusic.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PackData() implements CustomPacketPayload {
    public static final Type<PackData> TYPE = new Type<>(AllMusic.channel);
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
