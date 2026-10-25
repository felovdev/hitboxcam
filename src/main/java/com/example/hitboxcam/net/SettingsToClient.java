package com.example.hitboxcam.net;

import java.util.UUID;

import com.example.hitboxcam.HitboxCamMod;
import com.example.hitboxcam.PlayerTweaks;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Сервер -> клиенты: настройки игрока с данным UUID (по умолчанию = сбросить). */
public record SettingsToClient(UUID player, PlayerTweaks tweaks) implements CustomPacketPayload {
    public static final Type<SettingsToClient> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HitboxCamMod.MOD_ID, "to_client"));
    public static final StreamCodec<ByteBuf, SettingsToClient> CODEC =
            CustomPacketPayload.codec(SettingsToClient::write, SettingsToClient::new);

    public SettingsToClient(ByteBuf buf) {
        this(new UUID(buf.readLong(), buf.readLong()), PlayerTweaks.read(buf));
    }

    public void write(ByteBuf buf) {
        buf.writeLong(player.getMostSignificantBits());
        buf.writeLong(player.getLeastSignificantBits());
        tweaks.write(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
