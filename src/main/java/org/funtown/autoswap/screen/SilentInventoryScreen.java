package org.funtown.autoswap.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;

public class SilentInventoryScreen extends InventoryScreen {

    public SilentInventoryScreen(PlayerEntity player) {
        super(player);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
