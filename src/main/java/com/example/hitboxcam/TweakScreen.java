package com.example.hitboxcam;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * Меню по наброску: слева два блока ползунков (хитбокс / камера), снизу кнопки «Сброс» и «Готово»,
 * справа панель с фигуркой игрока, зелёной рамкой хитбокса и жёлтым квадратом камеры.
 */
public class TweakScreen extends Screen {
    private static final int SLIDER_W = 190;
    private static final int SLIDER_H = 20;

    private final List<ValueSlider> sliders = new ArrayList<>();
    /** Экран, на который вернуться при закрытии (меню паузы), либо null. */
    private final Screen parent;

    public TweakScreen() {
        this(null);
    }

    public TweakScreen(Screen parent) {
        super(Component.translatable("screen.hitboxcam.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        sliders.clear();
        int left = 20;
        int y = 20;

        addRenderableWidget(new StringWidget(left, y, SLIDER_W, 12,
                Component.translatable("screen.hitboxcam.hitbox"), this.font));
        y += 16;
        y = addSlider(left, y, "width", TweakState.MIN_WIDTH, TweakState.MAX_WIDTH,
                () -> TweakState.width, v -> { TweakState.width = v; applyHitbox(); });
        y = addSlider(left, y, "height", TweakState.MIN_HEIGHT, TweakState.MAX_HEIGHT,
                () -> TweakState.height, v -> { TweakState.height = v; applyHitbox(); });
        y = addSlider(left, y, "eye", TweakState.MIN_EYE, TweakState.MAX_EYE,
                () -> TweakState.eyeHeight, v -> { TweakState.eyeHeight = v; applyHitbox(); });

        y += 10;
        addRenderableWidget(new StringWidget(left, y, SLIDER_W, 12,
                Component.translatable("screen.hitboxcam.camera"), this.font));
        y += 16;
        y = addSlider(left, y, "camx", -TweakState.CAM_RANGE, TweakState.CAM_RANGE,
                () -> TweakState.camX, v -> TweakState.camX = v);
        y = addSlider(left, y, "camy", -TweakState.CAM_RANGE, TweakState.CAM_RANGE,
                () -> TweakState.camY, v -> TweakState.camY = v);
        y = addSlider(left, y, "camz", -TweakState.CAM_RANGE, TweakState.CAM_RANGE,
                () -> TweakState.camZ, v -> TweakState.camZ = v);

        int by = this.height - 32;
        addRenderableWidget(Button.builder(Component.translatable("screen.hitboxcam.reset"), b -> {
            TweakState.reset();
            applyHitbox();
            for (ValueSlider s : sliders) s.syncFromState();
        }).bounds(left, by, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 100, by, 90, 20).build());
    }

    private int addSlider(int x, int y, String key, float min, float max, Supplier<Float> get, Consumer<Float> set) {
        ValueSlider s = new ValueSlider(x, y, SLIDER_W, SLIDER_H, key, min, max, get, set);
        sliders.add(s);
        addRenderableWidget(s);
        return y + SLIDER_H + 4;
    }

    private static void applyHitbox() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.refreshDimensions();
        }
    }

    @Override
    public void onClose() {
        TweakState.save();
        if (parent != null) {
            HitboxCamMod.openScreen(Minecraft.getInstance(), parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // правая панель
        int px0 = this.width / 2 + 20;
        int px1 = this.width - 20;
        int py0 = 20;
        int py1 = this.height - 20;
        g.fill(px0, py0, px1, py1, 0xC0101010);
        g.fill(px0, py0, px1, py0 + 1, 0xFFB03030);
        g.fill(px0, py1 - 1, px1, py1, 0xFFB03030);
        g.fill(px0, py0, px0 + 1, py1, 0xFFB03030);
        g.fill(px1 - 1, py0, px1, py1, 0xFFB03030);

        // масштаб: 1 блок = k пикселей, чтобы всё влезало
        float k = Math.min((py1 - py0 - 40) / 6.5f, (px1 - px0 - 20) / 4.5f);
        k = Math.max(8f, Math.min(k, 40f));
        int cx = (px0 + px1) / 2;
        int ground = py1 - 20;

        // модель игрока (стоит ногами на линии земли; рисуем её ванильного размера)
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            try {
                double bbH = mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY;
                float yOff = 0.0625f;
                // в ваниле модель центрируется по bbHeight/2 + yOff, ноги окажутся на ground
                int centerY = Math.round(ground - (float) ((bbH / 2.0 + yOff) * k));
                int halfW = Math.round(2.5f * k);
                int halfH = Math.round(4f * k);
                InventoryScreen.extractEntityInInventoryFollowsMouse(g, cx - halfW, centerY - halfH,
                        cx + halfW, centerY + halfH, Math.round(k), yOff, mouseX, mouseY, mc.player);
            } catch (Throwable ignored) {
            }
        }

        // линия земли
        g.fill(px0 + 4, ground, px1 - 4, ground + 1, 0xFF808080);

        // зелёная рамка хитбокса
        int hw = Math.round(TweakState.width * k);
        int hh = Math.round(TweakState.height * k);
        int gx0 = cx - hw / 2;
        int gy0 = ground - hh;
        int gx1 = cx + hw / 2;
        int green = 0xFF55CC33;
        g.fill(gx0, gy0, gx1, gy0 + 1, green);
        g.fill(gx0, ground - 1, gx1, ground, green);
        g.fill(gx0, gy0, gx0 + 1, ground, green);
        g.fill(gx1 - 1, gy0, gx1, ground, green);

        // жёлтый квадрат = камера: уровень глаз + смещение
        int camPxX = cx + Math.round(TweakState.camX * k);
        int camPxY = ground - Math.round((TweakState.eyeHeight + TweakState.camY) * k);
        int s = 5;
        int yellow = 0xFFD0A030;
        g.fill(camPxX - s, camPxY - s, camPxX + s, camPxY - s + 1, yellow);
        g.fill(camPxX - s, camPxY + s - 1, camPxX + s, camPxY + s, yellow);
        g.fill(camPxX - s, camPxY - s, camPxX - s + 1, camPxY + s, yellow);
        g.fill(camPxX + s - 1, camPxY - s, camPxX + s, camPxY + s, yellow);
        // «хвостик» направления взгляда
        g.fill(camPxX, camPxY, camPxX + 14, camPxY + 1, yellow);
    }

    /** Ползунок со значением; в тексте показывает текущее число. */
    private static class ValueSlider extends AbstractSliderButton {
        private final String key;
        private final float min, max;
        private final Supplier<Float> get;
        private final Consumer<Float> set;

        ValueSlider(int x, int y, int w, int h, String key, float min, float max,
                    Supplier<Float> get, Consumer<Float> set) {
            super(x, y, w, h, Component.empty(), (get.get() - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
            updateMessage();
        }

        void syncFromState() {
            this.value = (get.get() - min) / (max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("slider.hitboxcam." + key,
                    String.format(java.util.Locale.ROOT, "%.2f", min + this.value * (max - min))));
        }

        @Override
        protected void applyValue() {
            set.accept((float) (min + this.value * (max - min)));
        }
    }
}
