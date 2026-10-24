package com.example.hitboxcam;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

    // В 26.3 ввод идёт через SDL: код клавиши = SDL keycode, для буквы это её ASCII-код в нижнем регистре.
    // 'h' = 104. Клавишу всё равно можно поменять в Настройки -> Управление.
    private static final int KEY_H = 104;

    public static KeyMapping OPEN_MENU;

    public HitboxCamMod(IEventBus modBus) {
        TweakState.load();
        modBus.addListener(HitboxCamMod::registerKeys);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN_MENU = new KeyMapping("key.hitboxcam.open", KEY_H, KeyMapping.Category.MISC);
        event.register(OPEN_MENU);
    }

    /**
     * Открывает экран. В 26.3 метод setScreen переехал из Minecraft в Minecraft.gui,
     * поэтому ищем его через рефлексию — так сборка не зависит от точного места метода.
     */
    static void openScreen(Minecraft mc, Screen screen) {
        try {
            Object target = mc;
            Method m = findSetScreen(mc.getClass());
            if (m == null) {
                Field f = mc.getClass().getField("gui");
                target = f.get(mc);
                m = findSetScreen(target.getClass());
            }
            if (m != null) {
                m.setAccessible(true);
                m.invoke(target, screen);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Method findSetScreen(Class<?> c) {
        for (Method m : c.getMethods()) {
            if (m.getName().equals("setScreen") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(Screen.class)) {
                return m;
            }
        }
        return null;
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_MENU != null && OPEN_MENU.consumeClick()) {
                if (mc.player != null) {
                    openScreen(mc, new TweakScreen());
                }
            }
        }
    }
}
