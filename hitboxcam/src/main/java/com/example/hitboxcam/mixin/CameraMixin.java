package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.hitboxcam.TweakState;

import net.minecraft.client.Camera;

/**
 * Смещает камеру относительно направления взгляда после стандартной настройки.
 * Работает и в первом лице, и в третьем.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    // Camera.move(forward, up, left) — защищённый метод ванили, вызывается через shadow-обёртку
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void move(float forward, float up, float left);

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void hitboxcam$offset(CallbackInfo ci) {
        try {
            if (TweakState.camX == 0f && TweakState.camY == 0f && TweakState.camZ == 0f) return;
            // x: вправо(+) => left = -x
            this.move(TweakState.camZ, TweakState.camY, -TweakState.camX);
        } catch (Throwable ignored) {
            // защита от крэша при несовпадении API
        }
    }
}
