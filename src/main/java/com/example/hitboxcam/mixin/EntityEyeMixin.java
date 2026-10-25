package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.hitboxcam.TweakState;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Пока считается прицел, луч стартует из позиции камеры, а не из глаз —
 * тогда ломается тот блок, на который ты реально смотришь.
 */
@Mixin(Entity.class)
public abstract class EntityEyeMixin {
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void hitboxcam$eye(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!TweakState.picking || !TweakState.cameraOffset()) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if ((Object) this != mc.player) return;
            Camera cam = mc.gameRenderer.mainCamera();
            if (cam == null || cam.isDetached()) return;
            cir.setReturnValue(cam.position());
        } catch (Throwable ignored) {
        }
    }
}
