package org.funtown.autoswap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.funtown.autoswap.radial.RadialMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void autoswap$radialLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (!RadialMenu.isOpen()) return;
        if ((Object) this != MinecraftClient.getInstance().player) return;
        RadialMenu.addDelta(cursorDeltaX, cursorDeltaY);
        ci.cancel();
    }
}
