package com.example.hitboxcam;

import java.util.UUID;

import com.example.hitboxcam.net.SettingsToServer;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** Клиентская часть: клавиша, меню, применение моих настроек и отправка их на сервер. */
@Mod(value = HitboxCamMod.MOD_ID, dist = Dist.CLIENT)
public class HitboxCamClient {
    public static KeyMapping OPEN_MENU;

    private static LocalPlayer lastPlayer;
    private static int loginTicks;
    private static boolean sendAfterLogin;
    private static boolean dirty;
    private static int ticksSinceSend = 100;

    public HitboxCamClient(IEventBus modBus) {
        TweakState.load();
        modBus.addListener(HitboxCamClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(HitboxCamClient::onTick);
        NeoForge.EVENT_BUS.addListener(HitboxCamClient::onLoggingOut);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        // В 26.3 коды клавиш — SDL-сканкоды, поэтому берём готовую константу
        OPEN_MENU = new KeyMapping("key.hitboxcam.open", InputConstants.KEY_H, KeyMapping.Category.MISC);
        event.register(OPEN_MENU);
    }

    /** Показывает экран (в 26.3 метод setScreen находится в Minecraft.gui). */
    public static void openScreen(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }

    /** Вызывается ползунками: настройки изменились, надо применить и отправить. */
    public static void markDirty() {
        dirty = true;
    }

    /** Немедленно применить и отправить (например, при закрытии меню). */
    public static void flushNow() {
        dirty = true;
        ticksSinceSend = 100;
    }

    private static void applyLocal(LocalPlayer p) {
        UUID id = p.getUUID();
        PlayerTweaks t = TweakState.toTweaks();
        PlayerTweaks old = TweakRegistry.get(id);
        PlayerTweaks cur = old == null ? PlayerTweaks.DEFAULT : old;
        if (!t.equals(cur)) {
            TweakRegistry.set(id, t);
            p.refreshDimensions();
        }
    }

    private static void sendToServer() {
        try {
            ClientPacketDistributor.sendToServer(new SettingsToServer(TweakState.toTweaks()));
        } catch (Throwable ignored) {
            // на сервере нет мода — настройки остаются только у тебя
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        TweakRegistry.clear();
        lastPlayer = null;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        TweakState.picking = false; // страховка: флаг не должен «залипнуть»

        LocalPlayer p = mc.player;
        if (p == null) {
            lastPlayer = null;
            return;
        }

        if (p != lastPlayer) {
            lastPlayer = p;
            loginTicks = 0;
            sendAfterLogin = true;
            applyLocal(p);
        }
        if (sendAfterLogin && ++loginTicks >= 20) {
            sendAfterLogin = false;
            sendToServer();
        }

        ticksSinceSend++;
        if (dirty) {
            applyLocal(p);
            if (ticksSinceSend >= 3) {
                sendToServer();
                ticksSinceSend = 0;
                dirty = false;
            }
        }

        while (OPEN_MENU != null && OPEN_MENU.consumeClick()) {
            openScreen(mc, new TweakScreen());
        }
    }
}
