package org.funtown.autoswap.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemPickerScreen extends Screen {

    private static final int CELL = 18;
    private static final int COLS = 12;
    private static final int PAD  = 8;

    private final Screen         parent;
    private final Consumer<Item> callback;
    private TextFieldWidget      searchField;
    private final List<Item>     allItems;
    private List<Item>           filtered;
    private int                  scrollRow = 0;
    private Item                 hoveredItem = null;

    public ItemPickerScreen(Screen parent, Consumer<Item> callback) {
        super(Text.translatable("autoswap.screen.picker.title"));
        this.parent   = parent;
        this.callback = callback;
        allItems = new ArrayList<>();
        Registries.ITEM.forEach(item -> { if (item != Items.AIR) allItems.add(item); });
        allItems.sort((a, b) -> a.getName().getString().compareToIgnoreCase(b.getName().getString()));
        filtered = new ArrayList<>(allItems);
    }

    @Override
    protected void init() {
        searchField = new TextFieldWidget(textRenderer,
                width / 2 - 110, PAD + 16, 220, 20,
                Text.translatable("autoswap.screen.picker.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.translatable("autoswap.screen.picker.search"));
        searchField.setChangedListener(q -> {
            scrollRow = 0;
            String query = q.trim().toLowerCase(Locale.ROOT);
            filtered = query.isEmpty() ? new ArrayList<>(allItems) :
                    allItems.stream().filter(item ->
                            item.getName().getString().toLowerCase(Locale.ROOT).contains(query) ||
                                    Registries.ITEM.getId(item).toString().toLowerCase(Locale.ROOT).contains(query)
                    ).collect(Collectors.toList());
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("autoswap.screen.picker.cancel"),
                btn -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - PAD - 20, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, PAD, 0xFFFFFF);

        int gridX      = (width - COLS * CELL) / 2;
        int gridTop    = PAD + 16 + 20 + PAD;
        int gridBottom = height - PAD - 20 - PAD;
        int visRows    = (gridBottom - gridTop) / CELL;
        hoveredItem    = null;

        for (int row = 0; row < visRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = (scrollRow + row) * COLS + col;
                if (idx >= filtered.size()) break;
                Item item = filtered.get(idx);
                int  ix   = gridX + col * CELL;
                int  iy   = gridTop + row * CELL;
                boolean hov = mouseX >= ix && mouseX < ix + CELL && mouseY >= iy && mouseY < iy + CELL;
                if (hov) { ctx.fill(ix, iy, ix + CELL, iy + CELL, 0x60FFFFFF); hoveredItem = item; }
                ctx.drawItem(item.getDefaultStack(), ix + 1, iy + 1);
            }
        }
        if (hoveredItem != null) ctx.drawTooltip(textRenderer, hoveredItem.getName(), mouseX, mouseY);

        
        if (maxScrollRow() > 0) {
            int totalRows = (filtered.size() + COLS - 1) / COLS;
            int barH = Math.max(10, (gridBottom - gridTop) * visRows / totalRows);
            int barY = gridTop + (gridBottom - gridTop - barH) * scrollRow / maxScrollRow();
            ctx.fill(width - 6, gridTop, width - 2, gridBottom, 0x40FFFFFF);
            ctx.fill(width - 6, barY, width - 2, barY + barH, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int gridX = (width - COLS * CELL) / 2, gridTop = PAD + 16 + 20 + PAD;
        int gridBottom = height - PAD - 20 - PAD, visRows = (gridBottom - gridTop) / CELL;
        for (int row = 0; row < visRows; row++) for (int col = 0; col < COLS; col++) {
            int idx = (scrollRow + row) * COLS + col; if (idx >= filtered.size()) break;
            int ix = gridX + col * CELL, iy = gridTop + row * CELL;
            if (mx >= ix && mx < ix + CELL && my >= iy && my < iy + CELL) {
                callback.accept(filtered.get(idx)); client.setScreen(parent); return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollRow = MathHelper.clamp(scrollRow - (int) Math.signum(v), 0, maxScrollRow());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { client.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int maxScrollRow() {
        int gt = PAD + 16 + 20 + PAD, gb = height - PAD - 20 - PAD, vr = (gb - gt) / CELL;
        return Math.max(0, (filtered.size() + COLS - 1) / COLS - vr);
    }
}