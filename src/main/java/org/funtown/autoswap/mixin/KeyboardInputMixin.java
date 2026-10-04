package org.funtown.autoswap.mixin;

import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.funtown.autoswap.swap.SwapExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void autoswap$silenceInput(CallbackInfo ci) {
        if (!SwapExecutor.isSwapActive()) return;
        InputAccessor accessor = (InputAccessor) (Object) this;
        accessor.setPlayerInput(PlayerInput.DEFAULT);
        accessor.setMovementVector(Vec2f.ZERO);
    }
}
