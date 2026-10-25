package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.hitboxcam.TweakState;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

/**
 * Помечает момент, когда игра считает, куда смотрит прицел (какой блок/моб под курсором).
 * В это время EntityEyeMixin подставляет позицию камеры вместо позиции глаз.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    private static final String PICK = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;";

    @Inject(method = PICK, at = @At("HEAD"), require = 0)
    private static void hitboxcam$pickStart(Entity e, double a, double b, float pt, CallbackInfoReturnable<HitResult> cir) {
        TweakState.picking = true;
    }

    @Inject(method = PICK, at = @At("RETURN"), require = 0)
    private static void hitboxcam$pickEnd(Entity e, double a, double b, float pt, CallbackInfoReturnable<HitResult> cir) {
        TweakState.picking = false;
    }
}
