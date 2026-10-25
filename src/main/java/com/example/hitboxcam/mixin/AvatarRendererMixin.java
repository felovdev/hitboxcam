package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.hitboxcam.PlayerTweaks;
import com.example.hitboxcam.TweakRegistry;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;

/** Меняет размер модели игрока (видно и в мире, и в меню, и другим игрокам с модом). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void hitboxcam$scale(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        try {
            PlayerTweaks t = TweakRegistry.get(entity.getUUID());
            if (t != null && t.scale() != PlayerTweaks.DEF_SCALE) {
                state.scale *= t.scale();
            }
        } catch (Throwable ignored) {
        }
    }
}
