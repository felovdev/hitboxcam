package com.example.hitboxcam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.neoforged.fml.loading.FMLPaths;

/** Хранит все настройки (только клиент) и сохраняет их в config/hitboxcam.properties. */
public final class TweakState {
    // значения по умолчанию = ванильный игрок
    public static final float DEF_WIDTH = 0.6f;
    public static final float DEF_HEIGHT = 1.8f;
    public static final float DEF_EYE = 1.62f;

    public static final float MIN_WIDTH = 0.1f, MAX_WIDTH = 4.0f;
    public static final float MIN_HEIGHT = 0.2f, MAX_HEIGHT = 6.0f;
    public static final float MIN_EYE = 0.1f, MAX_EYE = 6.0f;
    public static final float CAM_RANGE = 5.0f; // камера: -5..+5 блоков по каждой оси

    public static float width = DEF_WIDTH;
    public static float height = DEF_HEIGHT;
    public static float eyeHeight = DEF_EYE;

    /** Смещение камеры: x = вправо(+)/влево(-), y = вверх(+)/вниз(-), z = вперёд(+)/назад(-). */
    public static float camX = 0f;
    public static float camY = 0f;
    public static float camZ = 0f;

    private TweakState() {}

    public static void reset() {
        width = DEF_WIDTH;
        height = DEF_HEIGHT;
        eyeHeight = DEF_EYE;
        camX = 0f;
        camY = 0f;
        camZ = 0f;
    }

    public static boolean hitboxModified() {
        return width != DEF_WIDTH || height != DEF_HEIGHT || eyeHeight != DEF_EYE;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("hitboxcam.properties");
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("width", Float.toString(width));
        p.setProperty("height", Float.toString(height));
        p.setProperty("eye", Float.toString(eyeHeight));
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
            width = clamp(Float.parseFloat(p.getProperty("width", "" + DEF_WIDTH)), MIN_WIDTH, MAX_WIDTH);
            height = clamp(Float.parseFloat(p.getProperty("height", "" + DEF_HEIGHT)), MIN_HEIGHT, MAX_HEIGHT);
            eyeHeight = clamp(Float.parseFloat(p.getProperty("eye", "" + DEF_EYE)), MIN_EYE, MAX_EYE);
            camX = clamp(Float.parseFloat(p.getProperty("camX", "0")), -CAM_RANGE, CAM_RANGE);
            camY = clamp(Float.parseFloat(p.getProperty("camY", "0")), -CAM_RANGE, CAM_RANGE);
            camZ = clamp(Float.parseFloat(p.getProperty("camZ", "0")), -CAM_RANGE, CAM_RANGE);
        } catch (IOException | NumberFormatException ignored) {
        }
    }

    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
