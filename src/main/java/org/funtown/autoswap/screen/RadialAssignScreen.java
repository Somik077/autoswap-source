package org.funtown.autoswap.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ItemFilter;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.RadialSlot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
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
        return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (client.player == null) return;

        int left = gridX() - 6, right = gridX() + COLS * CELL + 6;
        int top = gridTop() - 28, bottom = gridTop() + 4 * CELL + GAP + 6;
        ctx.fill(left, top, right, bottom, 0xAA000000);

        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 6, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, ModTranslation.t("autoswap.radial.assign.hint"),
                width / 2, bottom + 6, 0xFF888888);

        ItemStack hoveredStack = ItemStack.EMPTY;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = cellX(col), y = cellY(row);
                int slot = row < 3 ? 9 + row * COLS + col : col;
                ctx.fill(x, y, x + CELL - 1, y + CELL - 1, 0x44FFFFFF);
                ItemStack stack = client.player.getInventory().getStack(slot);
                boolean hov = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
                if (hov) {
                    ctx.fill(x, y, x + CELL - 1, y + CELL - 1, 0x66FFFFFF);
                    hoveredStack = stack;
                }
                if (!stack.isEmpty()) ctx.drawItem(stack, x + 1, y + 1);
            }
        }

        if (!hoveredStack.isEmpty()) {
            List<Text> tip = new ArrayList<>();
            tip.add(hoveredStack.getName());
            int cmd = ItemFilter.cmdOf(hoveredStack);
            if (cmd != ItemFilter.NO_CMD)
                tip.add(Text.literal("CMD: " + cmd).styled(s -> s.withColor(0x8FB4FF)));
            ItemFilter f = ItemFilter.fromStack(hoveredStack);
            if (f != null && shiftHeld()) tip.add(Text.literal("⚙ " + f.describe()).styled(s -> s.withColor(0xFFFF55)));
            ctx.drawTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean consumed) {
        if (super.mouseClicked(click, consumed)) return true;
        if (client.player == null) return false;

        int slot = slotAt(click.x(), click.y());
        if (slot < 0) return false;
        ItemStack stack = client.player.getInventory().getStack(slot);
        if (stack.isEmpty()) return false;

        RadialSlot rs = AutoSwapConfig.getInstance().radial.slots.get(sector);
        rs.itemId = Registries.ITEM.getId(stack.getItem()).toString();
        rs.filter = ItemFilter.identityOf(stack, shiftHeld());
        rs.name   = stack.contains(DataComponentTypes.CUSTOM_NAME) ? stack.getName().getString() : "";
        rs.setStack(stack);
        AutoSwapConfig.save();
        client.setScreen(null);
        return true;
    }
}
