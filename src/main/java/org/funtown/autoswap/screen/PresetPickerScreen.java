package org.funtown.autoswap.screen;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.ServerPresets;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PresetPickerScreen extends Screen {

    private static final int ROW_H = 13, PAD = 8;

    private final Screen                     parent;
    private final Consumer<String>           callback;
    private List<ServerPresets.Preset>       filtered = new ArrayList<>(ServerPresets.all());
    private EditBox                  searchField;
    private int                              scroll = 0;

    public PresetPickerScreen(Screen parent, Consumer<String> callback) {
        super(ModTranslation.t("autoswap.screen.preset.title"));
        this.parent   = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        searchField = new EditBox(getFont(), width / 2 - 110, PAD + 16, 220, 20,
                ModTranslation.t("autoswap.screen.picker.search"));
        searchField.setMaxLength(64);
        searchField.setHint(ModTranslation.t("autoswap.screen.picker.search")
                .copy().withStyle(ChatFormatting.DARK_GRAY));
        searchField.setResponder(q -> {
            scroll = 0;
            String query = q.trim().toLowerCase(Locale.ROOT);
            filtered = new ArrayList<>();
            for (ServerPresets.Preset p : ServerPresets.all())
                if (query.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(query)) filtered.add(p);
        });
        addRenderableWidget(searchField);
        setInitialFocus(searchField);
        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.picker.cancel"),
                btn -> minecraft.setScreen(parent)
        ).bounds(width / 2 - 50, height - PAD - 20, 100, 20).build());
    }

    private int listTop()     { return PAD + 16 + 20 + PAD; }
    private int listBottom()  { return height - PAD - 20 - PAD; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_H); }
    private int maxScroll()   { return Math.max(0, filtered.size() - visibleRows()); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(getFont(), title, width / 2, PAD, 0xFFFFFFFF);

        int x = width / 2 - 110;
        for (int row = 0; row < visibleRows(); row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            ServerPresets.Preset p = filtered.get(idx);
            int y = listTop() + row * ROW_H;
            boolean hov = mouseX >= x && mouseX < x + 220 && mouseY >= y && mouseY < y + ROW_H;
            if (hov) ctx.fill(x, y, x + 220, y + ROW_H, 0x40FFFFFF);
            ctx.text(getFont(), Component.literal(p.name()), x + 3, y + 3, 0xFFFFFFFF);
            int gw = getFont().width(p.group());
            ctx.text(getFont(), Component.literal(p.group()), x + 217 - gw, y + 3, 0xFF777777);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean consumed) {
        if (super.mouseClicked(click, consumed)) return true;
        double mx = click.x(), my = click.y();
        int x = width / 2 - 110;
        if (mx < x || mx >= x + 220) return false;
        for (int row = 0; row < visibleRows(); row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            int y = listTop() + row * ROW_H;
            if (my >= y && my < y + ROW_H) {
                callback.accept(filtered.get(idx).name());
                minecraft.setScreen(parent);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Mth.clamp(scroll - (int) Math.signum(v) * 2, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (InputConstants.getKey(input).getValue() == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }
}
