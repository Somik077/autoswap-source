package org.funtown.autoswap.screen;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ItemFilter;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.RadialSlot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;


public class RadialAssignScreen extends Screen {

    private static final int CELL = 18, COLS = 9, GAP = 6;

    private final int sector;

    public RadialAssignScreen(int sector) {
        super(ModTranslation.t("autoswap.radial.assign.title", sector + 1));
        this.sector = sector;
    }

    private int gridX()   { return (width - COLS * CELL) / 2; }
    private int gridTop() { return height / 2 - (4 * CELL + GAP) / 2; }

    private int cellX(int col) { return gridX() + col * CELL; }
    private int cellY(int row) { return row < 3 ? gridTop() + row * CELL : gridTop() + 3 * CELL + GAP; }

    
    private int slotAt(double mx, double my) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = cellX(col), y = cellY(row);
                if (mx >= x && mx < x + CELL && my >= y && my < y + CELL)
                    return row < 3 ? 9 + row * COLS + col : col;
            }
        }
        return -1;
    }

    private boolean shiftHeld() {
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        if (minecraft.player == null) return;

        int left = gridX() - 6, right = gridX() + COLS * CELL + 6;
        int top = gridTop() - 28, bottom = gridTop() + 4 * CELL + GAP + 6;
        ctx.fill(left, top, right, bottom, 0xAA000000);

        ctx.centeredText(getFont(), title, width / 2, top + 6, 0xFFFFFFFF);
        ctx.centeredText(getFont(), ModTranslation.t("autoswap.radial.assign.hint"),
                width / 2, bottom + 6, 0xFF888888);

        ItemStack hoveredStack = ItemStack.EMPTY;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = cellX(col), y = cellY(row);
                int slot = row < 3 ? 9 + row * COLS + col : col;
                ctx.fill(x, y, x + CELL - 1, y + CELL - 1, 0x44FFFFFF);
                ItemStack stack = minecraft.player.getInventory().getItem(slot);
                boolean hov = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
                if (hov) {
                    ctx.fill(x, y, x + CELL - 1, y + CELL - 1, 0x66FFFFFF);
                    hoveredStack = stack;
                }
                if (!stack.isEmpty()) ctx.item(stack, x + 1, y + 1);
            }
        }

        if (!hoveredStack.isEmpty()) {
            List<Component> tip = new ArrayList<>();
            tip.add(hoveredStack.getHoverName());
            int cmd = ItemFilter.cmdOf(hoveredStack);
            if (cmd != ItemFilter.NO_CMD)
                tip.add(Component.literal("CMD: " + cmd).withStyle(s -> s.withColor(0x8FB4FF)));
            ItemFilter f = ItemFilter.fromStack(hoveredStack);
            if (f != null && shiftHeld()) tip.add(Component.literal("⚙ " + f.describe()).withStyle(s -> s.withColor(0xFFFF55)));
            ctx.setTooltipForNextFrame(getFont(), tip, Optional.<TooltipComponent>empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean consumed) {
        if (super.mouseClicked(click, consumed)) return true;
        if (minecraft.player == null) return false;

        int slot = slotAt(click.x(), click.y());
        if (slot < 0) return false;
        ItemStack stack = minecraft.player.getInventory().getItem(slot);
        if (stack.isEmpty()) return false;

        RadialSlot rs = AutoSwapConfig.getInstance().radial.slots.get(sector);
        rs.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        rs.filter = ItemFilter.identityOf(stack, shiftHeld());
        rs.name   = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "";
        rs.setStack(stack);
        AutoSwapConfig.save();
        minecraft.setScreen(null);
        return true;
    }
}
