package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.hitboxcam.TweakState;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;

/**
 * Смещает камеру относительно направления взгляда после стандартного обновления.
 * В 26.3 метод называется update (раньше setup). Работает и от первого лица, и от третьего.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    // protected void move(float forward, float up, float left)
    @Shadow
    protected abstract void move(float forward, float up, float left);

    @Inject(method = "update", at = @At("TAIL"))
    private void hitboxcam$offset(DeltaTracker deltaTracker, CallbackInfo ci) {
        try {
            if (TweakState.camX == 0f && TweakState.camY == 0f && TweakState.camZ == 0f) return;
            // x: вправо(+) => left = -x
            this.move(TweakState.camZ, TweakState.camY, -TweakState.camX);
        } catch (Throwable ignored) {
        }
    }
}
