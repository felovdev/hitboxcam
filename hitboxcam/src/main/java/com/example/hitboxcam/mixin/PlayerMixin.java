package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.hitboxcam.TweakState;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/** Подменяет размеры хитбокса (только локальный игрок на клиенте). */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true, require = 0)
    private void hitboxcam$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        try {
            if (!TweakState.hitboxModified()) return;
            Player self = (Player) (Object) this;
            if (!self.level().isClientSide()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != self) return;
            // меняем только стоячую позу, чтобы приседание/плавание не ломались
            if (pose != Pose.STANDING) return;
            cir.setReturnValue(EntityDimensions.scalable(TweakState.width, TweakState.height)
                    .withEyeHeight(TweakState.eyeHeight));
        } catch (Throwable ignored) {
            // защита от крэша при несовпадении API
        }
    }
}
