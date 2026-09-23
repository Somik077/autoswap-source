package org.funtown.autoswap.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.funtown.autoswap.config.ModTranslation;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemPickerScreen extends Screen {

    private static final int CELL = 18, COLS = 12, PAD = 8;

    private final Screen parent;
    private final Consumer<Item> callback;
    private EditBox searchField;
    private final List<Item> allItems;
    private List<Item> filtered;
    private int  scrollRow   = 0;
    private Item hoveredItem = null;

    public ItemPickerScreen(Screen parent, Consumer<Item> callback) {
        super(ModTranslation.t("autoswap.screen.picker.title"));
        this.parent = parent; this.callback = callback;
        allItems = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> { if (item != Items.AIR) allItems.add(item); });

        allItems.sort((a, b) -> {
            if (a == Items.PLAYER_HEAD && b != Items.PLAYER_HEAD) return -1;
            if (a != Items.PLAYER_HEAD && b == Items.PLAYER_HEAD) return  1;
            return name(a).compareToIgnoreCase(name(b));
        });
        filtered = new ArrayList<>(allItems);
    }

    private static String name(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    @Override
    protected void init() {
        searchField = new EditBox(getFont(), width/2-110, PAD+16, 220, 20,
                ModTranslation.t("autoswap.screen.picker.search"));
        searchField.setMaxLength(64);
        searchField.setHint(ModTranslation.t("autoswap.screen.picker.search")
                .copy().withStyle(ChatFormatting.DARK_GRAY));
        searchField.setResponder(q -> {
            scrollRow = 0;
            String query = q.trim().toLowerCase(Locale.ROOT);
            filtered = query.isEmpty() ? new ArrayList<>(allItems) :
                    allItems.stream().filter(item ->
                            name(item).toLowerCase(Locale.ROOT).contains(query) ||
                                    BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase(Locale.ROOT).contains(query)
                    ).collect(Collectors.toList());
        });
        addRenderableWidget(searchField);
        setInitialFocus(searchField);
        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.picker.cancel"),
                btn -> minecraft.setScreen(parent)
        ).bounds(width/2-50, height-PAD-20, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(getFont(), title, width/2, PAD, 0xFFFFFFFF);

        int gridX = (width-COLS*CELL)/2, gridTop = PAD+16+20+PAD, gridBottom = height-PAD-20-PAD;
        int visRows = (gridBottom-gridTop)/CELL;
        hoveredItem = null;

        for (int row = 0; row < visRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = (scrollRow+row)*COLS+col;
                if (idx >= filtered.size()) break;
                Item item = filtered.get(idx);
                int ix = gridX+col*CELL, iy = gridTop+row*CELL;
                boolean hov = mouseX>=ix && mouseX<ix+CELL && mouseY>=iy && mouseY<iy+CELL;
                if (hov) { ctx.fill(ix, iy, ix+CELL, iy+CELL, 0x60FFFFFF); hoveredItem = item; }
                ctx.item(new ItemStack(item), ix+1, iy+1);
            }
        }

        if (hoveredItem != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(hoveredItem.getName(new ItemStack(hoveredItem)));
            if (hoveredItem == Items.PLAYER_HEAD) {
                tip.add(Component.literal("Used for server custom items").withStyle(s -> s.withColor(0x888888).withItalic(true)));
                tip.add(Component.literal("(custom spheres, etc.)").withStyle(s -> s.withColor(0x888888).withItalic(true)));
            }
            ctx.setTooltipForNextFrame(getFont(), tip, Optional.<TooltipComponent>empty(), mouseX, mouseY);
        }

        if (maxScrollRow() > 0) {
            int totalRows = (filtered.size()+COLS-1)/COLS;
            int barH = Math.max(10, (gridBottom-gridTop)*visRows/totalRows);
            int barY = gridTop+(gridBottom-gridTop-barH)*scrollRow/maxScrollRow();
            ctx.fill(width-6, gridTop, width-2, gridBottom, 0x40FFFFFF);
            ctx.fill(width-6, barY, width-2, barY+barH, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean consumed) {
        if (super.mouseClicked(click, consumed)) return true;
        double mx = click.x(), my = click.y();
        int gridX = (width-COLS*CELL)/2, gridTop = PAD+16+20+PAD, gridBottom = height-PAD-20-PAD;
        int visRows = (gridBottom-gridTop)/CELL;
        for (int row = 0; row < visRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = (scrollRow+row)*COLS+col;
                if (idx >= filtered.size()) break;
                int ix = gridX+col*CELL, iy = gridTop+row*CELL;
                if (mx>=ix && mx<ix+CELL && my>=iy && my<iy+CELL) {
                    callback.accept(filtered.get(idx));
                    minecraft.setScreen(parent);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollRow = Mth.clamp(scrollRow-(int)Math.signum(v), 0, maxScrollRow());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (InputConstants.getKey(input).getValue() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(parent); return true;
        }
        return super.keyPressed(input);
    }

    private int maxScrollRow() {
        int gt = PAD+16+20+PAD, gb = height-PAD-20-PAD;
        return Math.max(0, (filtered.size()+COLS-1)/COLS - (gb-gt)/CELL);
    }
}
