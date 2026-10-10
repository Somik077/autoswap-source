package org.funtown.autoswap.mixin;

import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
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
        accessor.setKeyPresses(new Input(false, false, false, false, false, false, false));
        accessor.setMoveVector(Vec2.ZERO);
    }
}
