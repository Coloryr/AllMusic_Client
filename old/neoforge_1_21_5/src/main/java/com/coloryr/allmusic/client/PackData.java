package com.coloryr.allmusic.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record PackData() implements CustomPacketPayload {
    public static final Type<PackData> TYPE = new Type<>(AllMusic.channel);
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
