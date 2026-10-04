package org.funtown.autoswap.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.funtown.autoswap.swap.SwapExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Input.class)
public abstract class InputMixin {

    @Shadow public PlayerInput playerInput;
    @Shadow protected Vec2f movementVector;

    @Inject(method = "tick", at = @At("TAIL"))
    private void autoswap$silenceInput(CallbackInfo ci) {
        if (SwapExecutor.isSwapActive()) {
            this.playerInput = PlayerInput.DEFAULT;
            this.movementVector = Vec2f.ZERO;
        }
    }
}
