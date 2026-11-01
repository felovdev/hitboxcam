package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.hitboxcam.TweakState;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;

/** Смещает камеру относительно направления взгляда. Работает и от первого лица, и от третьего. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void move(float forward, float up, float left);

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void hitboxcam$offset(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
                                  float partialTick, CallbackInfo ci) {
        try {
            if (!TweakState.cameraOffset()) return;
            // x: вправо(+) => left = -x
            this.move(TweakState.camZ, TweakState.camY, -TweakState.camX);
        } catch (Throwable ignored) {
        }
    }
}
