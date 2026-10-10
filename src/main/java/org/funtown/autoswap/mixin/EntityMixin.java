package org.funtown.autoswap.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.funtown.autoswap.radial.RadialMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void autoswap$radialLook(double yRot, double xRot, CallbackInfo ci) {
        if (!RadialMenu.isOpen()) return;
        if ((Object) this != Minecraft.getInstance().player) return;
        RadialMenu.addDelta(yRot, xRot);
        ci.cancel();
    }
}
