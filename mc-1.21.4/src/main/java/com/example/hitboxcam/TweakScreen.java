package com.example.hitboxcam;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && inPanel(mouseX, mouseY)) {
            draggingView = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingView) {
            viewYaw += (float) dragX * 0.012f;
            viewTilt = Preview3D.clampTilt(viewTilt - (float) dragY * 0.008f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingView) {
            draggingView = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ---------- рисование ----------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
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
        g.drawString(this.font, Component.translatable("screen.hitboxcam.legend_hitbox"), px0 + 16, py0 + 6, 0xFFFFFFFF);
        g.fill(px0 + 6, py0 + 19, px0 + 12, py0 + 25, Preview3D.color(Preview3D.CAMERA, false));
        g.drawString(this.font, Component.translatable("screen.hitboxcam.legend_camera"), px0 + 16, py0 + 18, 0xFFFFFFFF);
        g.drawString(this.font, Component.translatable("screen.hitboxcam.hint"), px0 + 6, py1 - 13, 0xFFA0A0A0);

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
    private void drawModel(GuiGraphics g, int cx, float ground, float k) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // как в ванильном инвентаре: временно ставим игроку «взгляд на зрителя» и потом возвращаем
        float yBodyRot = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float yHeadRotO = player.yHeadRotO;
        float yHeadRot = player.yHeadRot;
        try {
            player.yBodyRot = 180f;
            player.setYRot(180f);
            player.setXRot(0f);
            player.yHeadRot = player.getYRot();
            player.yHeadRotO = player.getYRot();

            Vector3f translation = new Vector3f(0f, player.getBbHeight() / 2f + 0.0625f, 0f);
            // ноги окажутся на линии земли: опорная точка = ground - k * translation.y
            float anchorY = ground - k * translation.y;
            Quaternionf pose = Preview3D.pose(viewYaw, viewTilt);
            Quaternionf cam = Preview3D.cameraOrientation(viewTilt);

            g.enableScissor(px0 + 1, py0 + 1, px1 - 1, py1 - 1);
            InventoryScreen.renderEntityInInventory(g, cx, anchorY, k, translation, pose, cam, player);
            g.disableScissor();
        } catch (Throwable ignored) {
        } finally {
            player.yBodyRot = yBodyRot;
            player.setYRot(yRot);
            player.setXRot(xRot);
            player.yHeadRotO = yHeadRotO;
            player.yHeadRot = yHeadRot;
        }
    }

    /** Линия толщиной в один пиксель GUI: повёрнутый тонкий прямоугольник (поверх всех элементов). */
    private static void drawLine(GuiGraphics g, Preview3D.Line l) {
        float dx = l.x1() - l.x0();
        float dy = l.y1() - l.y0();
        int len = Math.round((float) Math.hypot(dx, dy));
        if (len < 1) return;
        float angle = (float) Math.atan2(dy, dx);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(l.x0(), l.y0(), 300f);
        pose.mulPose(new Quaternionf().rotateZ(angle));
        pose.translate(0f, -0.5f, 0f);
        g.fill(0, 0, len, 1, Preview3D.color(l.kind(), l.back()));
        pose.popPose();
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
