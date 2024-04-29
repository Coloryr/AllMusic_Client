package com.coloryr.allmusic.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record PackData(ByteBuf buffer) implements CustomPacketPayload {
    @Override
    public void write(@NotNull FriendlyByteBuf pBuffer) {
        pBuffer.writeBytes(buffer);
    }

    @Override
    public @NotNull ResourceLocation id() {
        return AllMusic.channel;
    }
}
