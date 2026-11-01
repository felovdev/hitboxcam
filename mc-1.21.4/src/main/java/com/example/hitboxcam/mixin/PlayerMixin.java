package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.hitboxcam.PlayerTweaks;
import com.example.hitboxcam.TweakRegistry;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/** Подменяет размеры хитбокса и высоту глаз игрока. Работает и на клиенте, и на сервере. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true, require = 1)
    private void hitboxcam$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        try {
            PlayerTweaks t = TweakRegistry.get(((Player) (Object) this).getUUID());
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
