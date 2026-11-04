package com.example.hitboxcam;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;

/**
 * Меню: слева ползунки (хитбокс и модель / камера), снизу «Сброс» и «Готово»,
 * справа 3D-предпросмотр: вращающаяся модель игрока, 3D-рамка хитбокса и камера.
 * Модель вращается перетаскиванием мышью.
 */
public class TweakScreen extends Screen {
    private static final int LEFT = 20;
    private static final int SLIDER_W = 190;
    private static final int GAP = 3;

    // положение вида запоминается, пока игра запущена
    private static float viewYaw = Preview3D.DEFAULT_YAW;
    private static float viewTilt = Preview3D.DEFAULT_TILT;

    private final List<ValueSlider> sliders = new ArrayList<>();
    /** Экран, на который вернуться при закрытии (меню паузы), либо null. */
    private final Screen parent;

    private int px0, py0, px1, py1;
    private boolean draggingView;

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
        int rows = 8;
        int headers = 2 * 16;
        int free = this.height - 16 - headers - 10 - 34;
        int sliderH = Math.max(12, Math.min(20, free / rows - GAP));

        int y = 12;
        addRenderableWidget(new StringWidget(LEFT, y, SLIDER_W, 12,
                Component.translatable("screen.hitboxcam.hitbox"), this.font));
        y += 16;
        y = addSlider(y, sliderH, "width", PlayerTweaks.MIN_WIDTH, PlayerTweaks.MAX_WIDTH,
                () -> TweakState.width, v -> TweakState.width = v);
        y = addSlider(y, sliderH, "length", PlayerTweaks.MIN_LENGTH, PlayerTweaks.MAX_LENGTH,
                () -> TweakState.length, v -> TweakState.length = v);
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
            viewYaw = Preview3D.DEFAULT_YAW;
            viewTilt = Preview3D.DEFAULT_TILT;
            HitboxCamClient.markDirty();
            for (ValueSlider s : sliders) s.syncFromState();
        }).bounds(LEFT, by, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(LEFT + 100, by, 90, 20).build());

        // правая панель предпросмотра
        px0 = LEFT + SLIDER_W + 20;
        px1 = this.width - 12;
        py0 = 12;
        py1 = this.height - 12;
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

    // ---------- вращение мышью ----------

    private boolean inPanel(double x, double y) {
        return px1 - px0 >= 40 && x >= px0 && x < px1 && y >= py0 && y < py1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 0 && inPanel(event.x(), event.y())) {
            draggingView = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingView) {
            viewYaw += (float) dragX * 0.012f;
            viewTilt = Preview3D.clampTilt(viewTilt - (float) dragY * 0.008f);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingView) {
            draggingView = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    // ---------- рисование ----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (px1 - px0 < 40) return; // окно слишком узкое для предпросмотра

        // фон и рамка панели
        g.fill(px0, py0, px1, py1, 0xC0101010);
        int border = 0xFFB03030;
        g.fill(px0, py0, px1, py0 + 1, border);
        g.fill(px0, py1 - 1, px1, py1, border);
        g.fill(px0, py0, px0 + 1, py1, border);
        g.fill(px1 - 1, py0, px1, py1, border);

        // легенда и подсказка
        g.fill(px0 + 6, py0 + 7, px0 + 12, py0 + 13, Preview3D.color(Preview3D.HITBOX, false));
        g.text(this.font, Component.translatable("screen.hitboxcam.legend_hitbox"), px0 + 16, py0 + 6, 0xFFFFFFFF);
        g.fill(px0 + 6, py0 + 19, px0 + 12, py0 + 25, Preview3D.color(Preview3D.CAMERA, false));
        g.text(this.font, Component.translatable("screen.hitboxcam.legend_camera"), px0 + 16, py0 + 18, 0xFFFFFFFF);
        g.text(this.font, Component.translatable("screen.hitboxcam.hint"), px0 + 6, py1 - 13, 0xFFA0A0A0);

        float innerTop = py0 + 32;
        float innerBottom = py1 - 18;
        float k = Preview3D.fit(px1 - px0 - 16, innerBottom - innerTop, viewTilt);
        int cx = (px0 + px1) / 2;
        float ground = innerBottom - Preview3D.below(viewTilt) * k;

        List<Preview3D.Line> lines = Preview3D.lines(cx, ground, k, viewYaw, viewTilt);

        // 1) сетка земли, 2) модель игрока, 3) хитбокс и камера поверх модели
        for (Preview3D.Line l : lines) {
            if (l.kind() == Preview3D.GROUND) drawLine(g, l);
        }
        drawModel(g, cx, ground, k);
        for (Preview3D.Line l : lines) {
            if (l.kind() != Preview3D.GROUND) drawLine(g, l);
        }
    }

    /** Рисует модель игрока с нашим поворотом (вместо ванильного «следования за мышью»). */
    private void drawModel(GuiGraphicsExtractor g, int cx, float ground, float k) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        try {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            @SuppressWarnings("unchecked")
            EntityRenderer<Entity, EntityRenderState> renderer =
                    (EntityRenderer<Entity, EntityRenderState>) (EntityRenderer<?, ?>) dispatcher.getRenderer((Entity) player);
            EntityRenderState state = renderer.createRenderState(player, 1.0f);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            if (state instanceof LivingEntityRenderState living) {
                living.bodyRot = 180f; // лицом к зрителю
                living.yRot = 0f;
                if (living.pose != Pose.FALL_FLYING) living.xRot = 0f;
            }

            Vector3f translation = new Vector3f(0f, player.getBbHeight() / 2f + 0.0625f, 0f);
            // ноги окажутся на линии земли: центр области = ground - k * translation.y
            int anchorY = Math.round(ground - k * translation.y);
            int hw = (px1 - px0) / 2;
            int hh = Math.max(anchorY - py0, py1 - anchorY) + 4;

            g.enableScissor(px0 + 1, py0 + 1, px1 - 1, py1 - 1);
            g.entity(state, k, translation, Preview3D.pose(viewYaw, viewTilt),
                    Preview3D.cameraOrientation(viewTilt), cx - hw, anchorY - hh, cx + hw, anchorY + hh);
            g.disableScissor();
        } catch (Throwable ignored) {
        }
    }

    /** Линия толщиной в один пиксель GUI: повёрнутый тонкий прямоугольник. */
    private static void drawLine(GuiGraphicsExtractor g, Preview3D.Line l) {
        float dx = l.x1() - l.x0();
        float dy = l.y1() - l.y0();
        int len = Math.round((float) Math.hypot(dx, dy));
        if (len < 1) return;
        float angle = (float) Math.atan2(dy, dx);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(l.x0(), l.y0());
        pose.rotate(angle);
        pose.translate(0f, -0.5f);
        g.fill(0, 0, len, 1, Preview3D.color(l.kind(), l.back()));
        pose.popMatrix();
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
