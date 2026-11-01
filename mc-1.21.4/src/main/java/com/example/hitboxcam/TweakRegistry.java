package com.example.hitboxcam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Настройки всех игроков, о которых мы знаем (общий класс для клиента и сервера).
 * Игроки с настройками по умолчанию в таблице не хранятся.
 */
public final class TweakRegistry {
    private static final Map<UUID, PlayerTweaks> MAP = new ConcurrentHashMap<>();

    private TweakRegistry() {}

    public static PlayerTweaks get(UUID id) {
        return id == null ? null : MAP.get(id);
    }

    public static void set(UUID id, PlayerTweaks t) {
        if (t == null || t.isDefault()) {
            MAP.remove(id);
        } else {
            MAP.put(id, t);
        }
    }

    public static void remove(UUID id) {
        MAP.remove(id);
    }

    public static Map<UUID, PlayerTweaks> all() {
        return MAP;
    }

    public static void clear() {
        MAP.clear();
    }
}
