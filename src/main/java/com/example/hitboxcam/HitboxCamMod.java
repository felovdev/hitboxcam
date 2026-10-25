package com.example.hitboxcam;

import java.util.Map;
import java.util.UUID;

import com.example.hitboxcam.net.SettingsToClient;
import com.example.hitboxcam.net.SettingsToServer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Общая часть мода (работает и на клиенте, и на сервере): сеть и рассылка настроек всем игрокам.
 * Если мод стоит и на сервере, размер модели и хитбокс видят все игроки, у которых установлен мод.
 */
@Mod(HitboxCamMod.MOD_ID)
public class HitboxCamMod {
    public static final String MOD_ID = "hitboxcam";

    public HitboxCamMod(IEventBus modBus) {
        modBus.addListener(HitboxCamMod::registerPayloads);
        NeoForge.EVENT_BUS.addListener(HitboxCamMod::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(HitboxCamMod::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(HitboxCamMod::onEntityJoin);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): ванильные клиенты и серверы без мода по-прежнему могут подключаться
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(SettingsToServer.TYPE, SettingsToServer.CODEC, HitboxCamMod::handleOnServer);
        registrar.playToClient(SettingsToClient.TYPE, SettingsToClient.CODEC, HitboxCamMod::handleOnClient);
    }

    /** Сервер получил настройки игрока: запоминает, применяет и рассылает всем. */
    private static void handleOnServer(SettingsToServer payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        PlayerTweaks t = payload.tweaks();
        TweakRegistry.set(sp.getUUID(), t);
        sp.refreshDimensions();
        MinecraftServer server = sp.level().getServer();
        if (server != null) {
            broadcast(server, sp.getUUID(), t);
        }
    }

    /** Клиент получил настройки какого-то игрока: запоминает и обновляет его размеры. */
    private static void handleOnClient(SettingsToClient payload, IPayloadContext ctx) {
        TweakRegistry.set(payload.player(), payload.tweaks());
        Player target = ctx.player().level().getPlayerByUUID(payload.player());
        if (target != null) {
            target.refreshDimensions();
        }
    }

    private static void broadcast(MinecraftServer server, UUID id, PlayerTweaks t) {
        SettingsToClient msg = new SettingsToClient(id, t);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            try {
                PacketDistributor.sendToPlayer(other, msg);
            } catch (Throwable ignored) {
                // у этого игрока нет мода — пропускаем
            }
        }
    }

    /** Новому игроку отправляем настройки всех, кто уже на сервере. */
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joined)) return;
        for (Map.Entry<UUID, PlayerTweaks> e : TweakRegistry.all().entrySet()) {
            if (e.getKey().equals(joined.getUUID())) continue;
            try {
                PacketDistributor.sendToPlayer(joined, new SettingsToClient(e.getKey(), e.getValue()));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer left)) return;
        TweakRegistry.remove(left.getUUID());
        MinecraftServer server = left.level().getServer();
        if (server != null) {
            broadcast(server, left.getUUID(), PlayerTweaks.DEFAULT);
        }
    }

    /** Игрок появился в мире (вход, респавн, смена измерения) — сразу применяем его размеры. */
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player p && TweakRegistry.get(p.getUUID()) != null) {
            p.refreshDimensions();
        }
    }
}
