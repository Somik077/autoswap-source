package org.funtown.autoswap.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface InputAccessor {

    @Accessor("keyPresses")
    void setKeyPresses(Input input);

    @Accessor("moveVector")
    void setMoveVector(Vec2 vec);
}
