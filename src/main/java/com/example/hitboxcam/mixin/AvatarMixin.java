package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.hitboxcam.PlayerTweaks;
import com.example.hitboxcam.TweakRegistry;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;

/**
 * Подменяет размеры хитбокса и высоту глаз игрока. Работает и на клиенте, и на сервере.
 * В 26.3 размеры игрока определяет класс Avatar (а не LivingEntity), поэтому целимся в него.
 */
@Mixin(Avatar.class)
public abstract class AvatarMixin {
    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true, require = 1)
    private void hitboxcam$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        try {
            PlayerTweaks t = TweakRegistry.get(((Avatar) (Object) this).getUUID());
            if (t == null) return;
            EntityDimensions d = cir.getReturnValue();
            // масштабируем ванильные размеры пропорционально — приседание и плавание тоже работают
            float sw = t.width() / PlayerTweaks.DEF_WIDTH;
            float sh = t.height() / PlayerTweaks.DEF_HEIGHT;
            float se = t.eye() / PlayerTweaks.DEF_EYE;
            cir.setReturnValue(EntityDimensions.scalable(d.width() * sw, d.height() * sh)
                    .withEyeHeight(d.eyeHeight() * se));
        } catch (Throwable ignored) {
        }
    }
}
