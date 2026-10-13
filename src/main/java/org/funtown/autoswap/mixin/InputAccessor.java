package org.funtown.autoswap.mixin;

import net.minecraft.client.input.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {

    @Accessor("pressingForward")
    void setPressingForward(boolean value);

    @Accessor("pressingBack")
    void setPressingBack(boolean value);

    @Accessor("pressingLeft")
    void setPressingLeft(boolean value);

    @Accessor("pressingRight")
    void setPressingRight(boolean value);

    @Accessor("movementForward")
    void setMovementForward(float value);

    @Accessor("movementSideways")
    void setMovementSideways(float value);

    @Accessor("jumping")
    void setJumping(boolean value);

    @Accessor("sneaking")
    void setSneaking(boolean value);
}
