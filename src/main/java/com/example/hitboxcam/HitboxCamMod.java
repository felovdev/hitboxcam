package com.example.hitboxcam;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
        // Категория задаётся строкой-ключом перевода; см. lang/ru_ru.json и en_us.json
        OPEN_MENU = new KeyMapping("key.hitboxcam.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H,
                KeyMapping.Category.MISC);
        event.register(OPEN_MENU);
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_MENU != null && OPEN_MENU.consumeClick()) {
                if (mc.player != null) {
                    mc.setScreen(new TweakScreen());
                }
            }
        }
    }
}
