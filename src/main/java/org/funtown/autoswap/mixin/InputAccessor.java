package org.funtown.autoswap.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {

    @Accessor("playerInput")
    PlayerInput getPlayerInput();

    @Accessor("playerInput")
    void setPlayerInput(PlayerInput input);

    @Accessor("movementVector")
    void setMovementVector(Vec2f vec);
}
