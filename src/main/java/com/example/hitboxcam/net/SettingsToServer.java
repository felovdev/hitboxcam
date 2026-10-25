package com.example.hitboxcam.net;

import com.example.hitboxcam.HitboxCamMod;
import com.example.hitboxcam.PlayerTweaks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Клиент -> сервер: мои настройки. */
public record SettingsToServer(PlayerTweaks tweaks) implements CustomPacketPayload {
    public static final Type<SettingsToServer> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HitboxCamMod.MOD_ID, "to_server"));
    public static final StreamCodec<ByteBuf, SettingsToServer> CODEC =
            CustomPacketPayload.codec(SettingsToServer::write, SettingsToServer::new);

    public SettingsToServer(ByteBuf buf) {
        this(PlayerTweaks.read(buf));
    }

    public void write(ByteBuf buf) {
        tweaks.write(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
