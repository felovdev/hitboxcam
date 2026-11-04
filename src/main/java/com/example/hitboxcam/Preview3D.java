package com.example.hitboxcam;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Математика 3D-предпросмотра (общая для всех версий игры): превращает хитбокс, камеру и землю
 * в список 2D-линий с той же проекцией, что использует игра для модели игрока в меню.
 *
 * Пространство «позы»: начало — точка между ног, Y вверх, игрок смотрит в -Z, его правая рука — +X.
 * Экранная точка = (cx + k*v.x, ground + k*v.y), где v = поворот * точка, а поворот включает
 * переворот Z на 180° (так игра рисует модель в GUI, где ось Y направлена вниз).
 */
public final class Preview3D {
    public static final int GROUND = 0, HITBOX = 1, CAMERA = 2, EYE = 3;

    public static final float DEFAULT_YAW = 0.5f;
    public static final float DEFAULT_TILT = -0.3f; // отрицательный наклон = смотрим немного сверху
    public static final float MIN_TILT = -1.2f;
    public static final float MAX_TILT = 0.35f;

    /** Одна линия предпросмотра; back = ребро на дальней стороне (рисуется бледнее). */
    public record Line(float x0, float y0, float x1, float y1, int kind, boolean back) {}

    private Preview3D() {}

    /** Поворот модели в меню: переворот Z, наклон вида и поворот вокруг вертикальной оси игрока. */
    public static Quaternionf pose(float yaw, float tilt) {
        return new Quaternionf().rotateZ((float) Math.PI).rotateX(tilt).rotateY(yaw);
    }

    /** «Ориентация камеры» для рендера сущностей в меню (как в ванильном инвентаре). */
    public static Quaternionf cameraOrientation(float tilt) {
        return new Quaternionf().rotateX(tilt);
    }

    public static float clampTilt(float t) {
        return Math.max(MIN_TILT, Math.min(MAX_TILT, t));
    }

    public static int color(int kind, boolean back) {
        switch (kind) {
            case GROUND: return 0x55B0B0B0;
            case HITBOX: return back ? 0x7755CC33 : 0xFF55CC33;
            case CAMERA: return back ? 0x77E0B040 : 0xFFE0B040;
            default: return 0xFFFFFFFF;
        }
    }

    // ---------- размеры сцены (не зависят от угла поворота, чтобы масштаб не «прыгал») ----------

    public static float radius() {
        float r = Math.max(0.71f * Math.max(TweakState.width, TweakState.length), 0.45f * TweakState.modelScale);
        r = Math.max(r, (float) Math.hypot(TweakState.camX, TweakState.camZ) * TweakState.modelScale + 0.5f);
        return Math.max(r, 0.8f);
    }

    private static float top() {
        return Math.max(Math.max(TweakState.height, TweakState.eyeHeight + TweakState.camY * TweakState.modelScale + 0.5f),
                1.8f * TweakState.modelScale);
    }

    private static float bottom() {
        return Math.min(0f, TweakState.eyeHeight + TweakState.camY * TweakState.modelScale - 0.5f);
    }

    /** Сколько пикселей на блок, чтобы всё поместилось в область availW x availH. */
    public static float fit(float availW, float availH, float tilt) {
        float r = radius();
        float s = (float) Math.abs(Math.sin(tilt));
        float h = (top() - bottom()) + 2f * r * s;
        float k = Math.min(availH / h, availW / (2f * r));
        return Math.max(4f, Math.min(k, 60f));
    }

    /** Сколько блоков должно поместиться ниже линии ног (до нижнего края области). */
    public static float below(float tilt) {
        return -bottom() + radius() * (float) Math.abs(Math.sin(tilt));
    }

    // ---------- сборка линий ----------

    public static List<Line> lines(float cx, float ground, float k, float yaw, float tilt) {
        List<Line> out = new ArrayList<>(64);
        Quaternionf view = new Quaternionf().rotateZ((float) Math.PI).rotateX(tilt);
        Quaternionf ent = pose(yaw, tilt);

        // сетка земли (стоит на месте, пока модель вращается)
        float g = radius() + 0.2f;
        int n = 4;
        for (int i = -n; i <= n; i++) {
            float t = g * i / n;
            seg(out, view, cx, ground, k, GROUND, Float.NaN, -g, 0, t, g, 0, t);
            seg(out, view, cx, ground, k, GROUND, Float.NaN, t, 0, -g, t, 0, g);
        }

        // хитбокс
        float w = TweakState.width;
        float h = TweakState.height;
        float len = TweakState.length;
        box(out, ent, cx, ground, k, HITBOX, 0, h / 2f, 0, w / 2f, h / 2f, len / 2f);

        // метка глаз
        float eye = TweakState.eyeHeight;
        float m = Math.max(0.12f, 4f / k);
        seg(out, ent, cx, ground, k, EYE, Float.NaN, -m, eye, 0, m, eye, 0);
        seg(out, ent, cx, ground, k, EYE, Float.NaN, 0, eye - m, 0, 0, eye + m, 0);
        seg(out, ent, cx, ground, k, EYE, Float.NaN, 0, eye, -m, 0, eye, m);

        // камера: корпус + «объектив» (пирамида вперёд, в сторону -Z)
        float ms = TweakState.modelScale;
        float camX = TweakState.camX * ms;
        float camY = eye + TweakState.camY * ms;
        float camZ = -TweakState.camZ * ms;
        float u = Math.max(0.16f * ms, 6f / k);
        box(out, ent, cx, ground, k, CAMERA, camX, camY, camZ, u, 0.7f * u, 0.8f * u);
        float zc = proj(ent, cx, ground, k, camX, camY, camZ)[2];
        float z0 = camZ - 0.8f * u;
        float z1 = z0 - 1.6f * u;
        float[][] near = {{-0.45f * u, -0.35f * u}, {0.45f * u, -0.35f * u}, {0.45f * u, 0.35f * u}, {-0.45f * u, 0.35f * u}};
        float[][] far = {{-u, -0.75f * u}, {u, -0.75f * u}, {u, 0.75f * u}, {-u, 0.75f * u}};
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            seg(out, ent, cx, ground, k, CAMERA, zc, camX + near[i][0], camY + near[i][1], z0, camX + near[j][0], camY + near[j][1], z0);
            seg(out, ent, cx, ground, k, CAMERA, zc, camX + far[i][0], camY + far[i][1], z1, camX + far[j][0], camY + far[j][1], z1);
            seg(out, ent, cx, ground, k, CAMERA, zc, camX + near[i][0], camY + near[i][1], z0, camX + far[i][0], camY + far[i][1], z1);
        }
        // связь «глаза -> камера»
        if (TweakState.cameraOffset()) {
            seg(out, ent, cx, ground, k, CAMERA, -1e9f, 0, eye, 0, camX, camY, camZ);
        }
        return out;
    }

    private static float[] proj(Quaternionf q, float cx, float ground, float k, float x, float y, float z) {
        Vector3f v = new Vector3f(x, y, z).rotate(q);
        return new float[] {cx + k * v.x, ground + k * v.y, v.z};
    }

    /** Одна линия; refZ = глубина центра объекта (ребро дальше центра считается «задним»), NaN = без деления. */
    private static void seg(List<Line> out, Quaternionf q, float cx, float ground, float k, int kind, float refZ,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        float[] a = proj(q, cx, ground, k, x0, y0, z0);
        float[] b = proj(q, cx, ground, k, x1, y1, z1);
        boolean back = !Float.isNaN(refZ) && (a[2] + b[2]) / 2f > refZ + 1e-3f;
        out.add(new Line(a[0], a[1], b[0], b[1], kind, back));
    }

    private static void box(List<Line> out, Quaternionf q, float cx, float ground, float k, int kind,
                            float mx, float my, float mz, float hx, float hy, float hz) {
        float refZ = proj(q, cx, ground, k, mx, my, mz)[2];
        for (int i = 0; i < 8; i++) {
            for (int j = i + 1; j < 8; j++) {
                if (Integer.bitCount(i ^ j) != 1) continue;
                seg(out, q, cx, ground, k, kind, refZ,
                        mx + sgn(i, 1) * hx, my + sgn(i, 2) * hy, mz + sgn(i, 4) * hz,
                        mx + sgn(j, 1) * hx, my + sgn(j, 2) * hy, mz + sgn(j, 4) * hz);
            }
        }
    }

    private static float sgn(int bits, int mask) {
        return (bits & mask) != 0 ? 1f : -1f;
    }
}
