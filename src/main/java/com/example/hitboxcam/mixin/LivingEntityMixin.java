package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.hitboxcam.TweakState;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * Подменяет размеры хитбокса и высоту глаз локального игрока (только клиент).
 * В 26.3 метод getDefaultDimensions объявлен в LivingEntity, а не в Player.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
    private void hitboxcam$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        try {
            if (!TweakState.hitboxModified()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || (Object) this != mc.player) return;
            if (pose != Pose.STANDING) return; // приседание и плавание остаются ванильными
            cir.setReturnValue(EntityDimensions.scalable(TweakState.width, TweakState.height)
                    .withEyeHeight(TweakState.eyeHeight));
        } catch (Throwable ignored) {
        }
    }
}
