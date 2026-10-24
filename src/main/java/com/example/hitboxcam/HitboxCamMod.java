package com.example.hitboxcam;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = HitboxCamMod.MOD_ID, dist = Dist.CLIENT)
public class HitboxCamMod {
    public static final String MOD_ID = "hitboxcam";

    public static KeyMapping OPEN_MENU;

    public HitboxCamMod(IEventBus modBus) {
        TweakState.load();
        modBus.addListener(HitboxCamMod::registerKeys);
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

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        private static Player lastPlayer;
        private static int lastSignature = Integer.MIN_VALUE;

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();

            // Применяем размеры хитбокса при входе в мир, после респавна и при изменении настроек
            Player p = mc.player;
            if (p != null) {
                int sig = Float.hashCode(TweakState.width) * 31 * 31 + Float.hashCode(TweakState.height) * 31
                        + Float.hashCode(TweakState.eyeHeight);
                if (p != lastPlayer || sig != lastSignature) {
                    lastPlayer = p;
                    lastSignature = sig;
                    p.refreshDimensions();
                }
            } else {
                lastPlayer = null;
            }

            while (OPEN_MENU != null && OPEN_MENU.consumeClick()) {
                if (mc.player != null) {
                    openScreen(mc, new TweakScreen());
                }
            }
        }
    }
}
