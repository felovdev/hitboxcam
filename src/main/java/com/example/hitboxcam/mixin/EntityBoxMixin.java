package com.example.hitboxcam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.example.hitboxcam.PlayerTweaks;
import com.example.hitboxcam.TweakRegistry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** Длина хитбокса: растягивает рамку игрока вдоль оси Z (в Minecraft рамка всегда параллельна осям). */
@Mixin(Entity.class)
public abstract class EntityBoxMixin {
    @ModifyVariable(method = "setBoundingBox", at = @At("HEAD"), argsOnly = true, require = 0)
    private AABB hitboxcam$length(AABB box) {
        try {
            if (!((Object) this instanceof Player p)) return box;
            PlayerTweaks t = TweakRegistry.get(p.getUUID());
            if (t == null || t.length() == t.width()) return box;
            double zc = (box.minZ + box.maxZ) / 2.0;
            double half = (box.maxZ - box.minZ) / 2.0 * (t.length() / t.width());
            return new AABB(box.minX, box.minY, zc - half, box.maxX, box.maxY, zc + half);
        } catch (Throwable e) {
            return box;
        }
    }
}
