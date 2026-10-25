package com.example.hitboxcam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.neoforged.fml.loading.FMLPaths;

/** Мои собственные настройки (только клиент) + сохранение в config/hitboxcam.properties. */
public final class TweakState {
    public static final float CAM_RANGE = 5.0f; // камера: -5..+5 блоков по каждой оси

    public static float width = PlayerTweaks.DEF_WIDTH;
    public static float height = PlayerTweaks.DEF_HEIGHT;
    public static float eyeHeight = PlayerTweaks.DEF_EYE;
    public static float modelScale = PlayerTweaks.DEF_SCALE;

    /** Смещение камеры: x = вправо(+)/влево(-), y = вверх(+)/вниз(-), z = вперёд(+)/назад(-). */
    public static float camX = 0f;
    public static float camY = 0f;
    public static float camZ = 0f;

    /** true только внутри расчёта луча прицела (см. LocalPlayerMixin). */
    public static boolean picking = false;

    private TweakState() {}

    public static PlayerTweaks toTweaks() {
        return PlayerTweaks.clamped(width, height, eyeHeight, modelScale);
    }

    public static boolean cameraOffset() {
        return camX != 0f || camY != 0f || camZ != 0f;
    }

    public static void reset() {
        width = PlayerTweaks.DEF_WIDTH;
        height = PlayerTweaks.DEF_HEIGHT;
        eyeHeight = PlayerTweaks.DEF_EYE;
        modelScale = PlayerTweaks.DEF_SCALE;
        camX = 0f;
        camY = 0f;
        camZ = 0f;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("hitboxcam.properties");
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("width", Float.toString(width));
        p.setProperty("height", Float.toString(height));
        p.setProperty("eye", Float.toString(eyeHeight));
        p.setProperty("scale", Float.toString(modelScale));
        p.setProperty("camX", Float.toString(camX));
        p.setProperty("camY", Float.toString(camY));
        p.setProperty("camZ", Float.toString(camZ));
        try (var out = Files.newOutputStream(file())) {
            p.store(out, "Hitbox & Camera Tweaker");
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (var in = Files.newInputStream(f)) {
            p.load(in);
            PlayerTweaks t = PlayerTweaks.clamped(
                    Float.parseFloat(p.getProperty("width", "" + PlayerTweaks.DEF_WIDTH)),
                    Float.parseFloat(p.getProperty("height", "" + PlayerTweaks.DEF_HEIGHT)),
                    Float.parseFloat(p.getProperty("eye", "" + PlayerTweaks.DEF_EYE)),
                    Float.parseFloat(p.getProperty("scale", "" + PlayerTweaks.DEF_SCALE)));
            width = t.width();
            height = t.height();
            eyeHeight = t.eye();
            modelScale = t.scale();
            camX = clamp(Float.parseFloat(p.getProperty("camX", "0")), -CAM_RANGE, CAM_RANGE);
            camY = clamp(Float.parseFloat(p.getProperty("camY", "0")), -CAM_RANGE, CAM_RANGE);
            camZ = clamp(Float.parseFloat(p.getProperty("camZ", "0")), -CAM_RANGE, CAM_RANGE);
        } catch (IOException | NumberFormatException ignored) {
        }
    }

    public static float clamp(float v, float lo, float hi) {
        if (!Float.isFinite(v)) return 0f;
        return Math.max(lo, Math.min(hi, v));
    }
}
