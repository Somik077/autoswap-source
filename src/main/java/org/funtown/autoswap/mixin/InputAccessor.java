package org.funtown.autoswap.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {

    @Accessor("playerInput")
    void setPlayerInput(PlayerInput input);

    @Accessor("movementForward")
    void setMovementForward(float value);

    @Accessor("movementSideways")
    void setMovementSideways(float value);
}
