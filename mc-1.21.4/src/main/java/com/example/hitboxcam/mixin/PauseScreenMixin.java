package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.hitboxcam.HitboxCamClient;
import com.example.hitboxcam.TweakScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Добавляет кнопку «Хитбокс и камера» в левый верхний угол меню паузы (Esc). */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void hitboxcam$addButton(CallbackInfo ci) {
        try {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("screen.hitboxcam.open_button"),
                    b -> HitboxCamClient.openScreen(Minecraft.getInstance(), new TweakScreen((Screen) (Object) this)))
                    .bounds(8, 8, 140, 20).build());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
