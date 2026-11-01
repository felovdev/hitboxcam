package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.hitboxcam.PlayerTweaks;
import com.example.hitboxcam.TweakRegistry;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;

/** Меняет размер модели игрока (видно и в мире, и в меню, и другим игрокам с модом). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void hitboxcam$scale(AbstractClientPlayer entity, PlayerRenderState state, float partialTick, CallbackInfo ci) {
        try {
            PlayerTweaks t = TweakRegistry.get(entity.getUUID());
            if (t != null && t.scale() != PlayerTweaks.DEF_SCALE) {
                state.scale *= t.scale();
            }
        } catch (Throwable ignored) {
        }
    }
}
