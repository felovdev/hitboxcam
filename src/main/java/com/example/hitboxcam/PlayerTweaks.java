package com.example.hitboxcam;

import io.netty.buffer.ByteBuf;

/** Настройки одного игрока: размеры хитбокса, высота глаз и размер модели. Общий класс (клиент и сервер). */
public record PlayerTweaks(float width, float height, float eye, float scale) {
    public static final float DEF_WIDTH = 0.6f;
    public static final float DEF_HEIGHT = 1.8f;
    public static final float DEF_EYE = 1.62f;
    public static final float DEF_SCALE = 1.0f;

    public static final float MIN_WIDTH = 0.1f, MAX_WIDTH = 4.0f;
    public static final float MIN_HEIGHT = 0.2f, MAX_HEIGHT = 6.0f;
    public static final float MIN_EYE = 0.1f, MAX_EYE = 6.0f;
    public static final float MIN_SCALE = 0.25f, MAX_SCALE = 4.0f;

    public static final PlayerTweaks DEFAULT = new PlayerTweaks(DEF_WIDTH, DEF_HEIGHT, DEF_EYE, DEF_SCALE);

    public boolean isDefault() {
        return this.equals(DEFAULT);
    }

    /** Ограничивает значения допустимыми пределами (защита от мусора из сети). */
    public static PlayerTweaks clamped(float w, float h, float e, float s) {
        return new PlayerTweaks(
                clamp(w, MIN_WIDTH, MAX_WIDTH, DEF_WIDTH),
                clamp(h, MIN_HEIGHT, MAX_HEIGHT, DEF_HEIGHT),
                clamp(e, MIN_EYE, MAX_EYE, DEF_EYE),
                clamp(s, MIN_SCALE, MAX_SCALE, DEF_SCALE));
    }

    private static float clamp(float v, float lo, float hi, float def) {
        if (!Float.isFinite(v)) return def;
        return Math.max(lo, Math.min(hi, v));
    }

    public void write(ByteBuf buf) {
        buf.writeFloat(width);
        buf.writeFloat(height);
        buf.writeFloat(eye);
        buf.writeFloat(scale);
    }

    public static PlayerTweaks read(ByteBuf buf) {
        return clamped(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
