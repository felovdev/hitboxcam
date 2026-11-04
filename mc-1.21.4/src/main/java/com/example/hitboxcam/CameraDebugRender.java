package com.example.hitboxcam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** При включённых хитбоксах (F3 + B) рисует жёлтую рамку там, где находится камера, и линию от глаз к ней. */
public final class CameraDebugRender {
    private CameraDebugRender() {}

    public static void onRender(RenderLevelStageEvent event) {
        try {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (p == null || !TweakState.cameraOffset()) return;
            if (!mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) return;
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) return;

            float pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
            Vec3 eye = p.getEyePosition(pt);
            Vec3 fwd = p.getViewVector(pt);
            Vec3 left = new Vec3(0, 1, 0).cross(fwd);
            if (left.lengthSqr() < 1.0E-6) left = new Vec3(1, 0, 0);
            left = left.normalize();
            Vec3 up = fwd.cross(left).normalize();

            float s = TweakState.modelScale;
            Vec3 cam = eye.add(fwd.scale(TweakState.camZ * s))
                    .add(up.scale(TweakState.camY * s))
                    .add(left.scale(-TweakState.camX * s));

            Vec3 c = event.getCamera().getPosition();
            PoseStack ps = event.getPoseStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer vc = buffers.getBuffer(RenderType.lines());

            double h = 0.15 * s + 0.05;
            LevelRenderer.renderLineBox(ps, vc, cam.x - c.x - h, cam.y - c.y - h, cam.z - c.z - h,
                    cam.x - c.x + h, cam.y - c.y + h, cam.z - c.z + h, 1f, 0.85f, 0.1f, 1f);
            // «лучик» взгляда камеры
            Vec3 tip = cam.add(fwd.scale(0.6));
            LevelRenderer.renderLineBox(ps, vc, tip.x - c.x - 0.02, tip.y - c.y - 0.02, tip.z - c.z - 0.02,
                    tip.x - c.x + 0.02, tip.y - c.y + 0.02, tip.z - c.z + 0.02, 1f, 0.85f, 0.1f, 1f);
            // связь «глаза -> камера»
            LevelRenderer.renderLineBox(ps, vc,
                    Math.min(eye.x, cam.x) - c.x, Math.min(eye.y, cam.y) - c.y, Math.min(eye.z, cam.z) - c.z,
                    Math.max(eye.x, cam.x) - c.x, Math.max(eye.y, cam.y) - c.y, Math.max(eye.z, cam.z) - c.z,
                    1f, 0.85f, 0.1f, 0.5f);
            buffers.endBatch(RenderType.lines());
        } catch (Throwable ignored) {
        }
    }
}
