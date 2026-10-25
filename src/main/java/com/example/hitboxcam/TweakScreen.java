package com.example.hitboxcam;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * Меню по наброску: слева блоки ползунков (хитбокс и модель / камера), снизу кнопки «Сброс» и «Готово»,
 * справа панель с моделью игрока, зелёной рамкой хитбокса и жёлтым квадратом камеры.
 */
public class TweakScreen extends Screen {
    private static final int LEFT = 20;
    private static final int SLIDER_W = 190;
    private static final int GAP = 3;

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

        // Вёрстка подстраивается под высоту окна: 7 ползунков + 2 заголовка + кнопки
        int rows = 7;
        int headers = 2 * 16;
        int free = this.height - 16 - headers - 10 - 34;
        int sliderH = Math.max(12, Math.min(20, free / rows - GAP));

        int y = 12;
        addRenderableWidget(new StringWidget(LEFT, y, SLIDER_W, 12,
                Component.translatable("screen.hitboxcam.hitbox"), this.font));
        y += 16;
        y = addSlider(y, sliderH, "width", PlayerTweaks.MIN_WIDTH, PlayerTweaks.MAX_WIDTH,
                () -> TweakState.width, v -> TweakState.width = v);
        y = addSlider(y, sliderH, "height", PlayerTweaks.MIN_HEIGHT, PlayerTweaks.MAX_HEIGHT,
                () -> TweakState.height, v -> TweakState.height = v);
        y = addSlider(y, sliderH, "eye", PlayerTweaks.MIN_EYE, PlayerTweaks.MAX_EYE,
                () -> TweakState.eyeHeight, v -> TweakState.eyeHeight = v);
        y = addSlider(y, sliderH, "scale", PlayerTweaks.MIN_SCALE, PlayerTweaks.MAX_SCALE,
                () -> TweakState.modelScale, v -> TweakState.modelScale = v);

        y += 10;
        addRenderableWidget(new StringWidget(LEFT, y, SLIDER_W, 12,
                Component.translatable("screen.hitboxcam.camera"), this.font));
        y += 16;
        y = addSlider(y, sliderH, "camx", -TweakState.CAM_RANGE, TweakState.CAM_RANGE,
                () -> TweakState.camX, v -> TweakState.camX = v);
        y = addSlider(y, sliderH, "camy", -TweakState.CAM_RANGE, TweakState.CAM_RANGE,
                () -> TweakState.camY, v -> TweakState.camY = v);
        y = addSlider(y, sliderH, "camz", -TweakState.CAM_RANGE, TweakState.CAM_RANGE,
                () -> TweakState.camZ, v -> TweakState.camZ = v);

        int by = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.hitboxcam.reset"), b -> {
            TweakState.reset();
            HitboxCamClient.markDirty();
            for (ValueSlider s : sliders) s.syncFromState();
        }).bounds(LEFT, by, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(LEFT + 100, by, 90, 20).build());
    }

    private int addSlider(int y, int h, String key, float min, float max, Supplier<Float> get, Consumer<Float> set) {
        ValueSlider s = new ValueSlider(LEFT, y, SLIDER_W, h, key, min, max, get, set);
        sliders.add(s);
        addRenderableWidget(s);
        return y + h + GAP;
    }

    @Override
    public void onClose() {
        TweakState.save();
        HitboxCamClient.flushNow();
        if (parent != null) {
            HitboxCamClient.openScreen(Minecraft.getInstance(), parent);
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
        int px0 = LEFT + SLIDER_W + 20;
        int px1 = this.width - 12;
        int py0 = 12;
        int py1 = this.height - 12;
        if (px1 - px0 < 40) return; // окно слишком узкое для предпросмотра
        g.fill(px0, py0, px1, py1, 0xC0101010);
        int border = 0xFFB03030;
        g.fill(px0, py0, px1, py0 + 1, border);
        g.fill(px0, py1 - 1, px1, py1, border);
        g.fill(px0, py0, px0 + 1, py1, border);
        g.fill(px1 - 1, py0, px1, py1, border);

        // легенда
        int green = 0xFF55CC33;
        int yellow = 0xFFD0A030;
        g.fill(px0 + 6, py0 + 7, px0 + 12, py0 + 13, green);
        g.text(this.font, Component.translatable("screen.hitboxcam.legend_hitbox"), px0 + 16, py0 + 6, 0xFFFFFFFF);
        g.fill(px0 + 6, py0 + 19, px0 + 12, py0 + 25, yellow);
        g.text(this.font, Component.translatable("screen.hitboxcam.legend_camera"), px0 + 16, py0 + 18, 0xFFFFFFFF);

        // автомасштаб: всё, что нарисовано (модель, рамка, камера), должно помещаться в панель
        float camEyeY = TweakState.eyeHeight + TweakState.camY;
        float topY = Math.max(Math.max(TweakState.height, camEyeY), 1.8f * TweakState.modelScale);
        float botY = Math.min(0f, camEyeY);
        float halfW = Math.max(Math.max(TweakState.width, 0.6f * TweakState.modelScale) / 2f,
                Math.abs(TweakState.camX)) + 0.3f;
        float availH = (py1 - py0) - 34 - 14;
        float availW = (px1 - px0) - 16;
        float k = Math.min(availH / (topY - botY + 0.2f), availW / (2f * halfW));
        k = Math.max(6f, Math.min(k, 60f));

        int cx = (px0 + px1) / 2;
        int ground = py1 - 12 + Math.round(botY * k);

        // модель игрока (ногами на линии земли, размер зависит от ползунка «Размер модели»)
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            try {
                float yOff = 0.0625f;
                float bbH = mc.player.getBbHeight();
                int centerY = Math.round(ground - (bbH / 2f + yOff) * k);
                int halfWpx = Math.round(2.5f * k);
                int halfHpx = Math.round(4f * k);
                InventoryScreen.extractEntityInInventoryFollowsMouse(g, cx - halfWpx, centerY - halfHpx,
                        cx + halfWpx, centerY + halfHpx, Math.round(k), yOff, mouseX, mouseY, mc.player);
            } catch (Throwable ignored) {
            }
        }

        // линия земли
        g.fill(px0 + 4, ground, px1 - 4, ground + 1, 0xFF808080);

        // зелёная рамка хитбокса
        int hw = Math.max(2, Math.round(TweakState.width * k));
        int hh = Math.max(2, Math.round(TweakState.height * k));
        int gx0 = cx - hw / 2;
        int gy0 = ground - hh;
        int gx1 = gx0 + hw;
        g.fill(gx0, gy0, gx1, gy0 + 1, green);
        g.fill(gx0, ground - 1, gx1, ground, green);
        g.fill(gx0, gy0, gx0 + 1, ground, green);
        g.fill(gx1 - 1, gy0, gx1, ground, green);

        // жёлтый квадрат = камера: уровень глаз + смещение
        int camPxX = cx + Math.round(TweakState.camX * k);
        int camPxY = ground - Math.round(camEyeY * k);
        int s = 5;
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
                    String.format(Locale.ROOT, "%.2f", min + this.value * (max - min))));
        }

        @Override
        protected void applyValue() {
            set.accept((float) (min + this.value * (max - min)));
            HitboxCamClient.markDirty();
        }
    }
}
