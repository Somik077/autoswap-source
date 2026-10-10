package org.funtown.autoswap.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
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
    private TextFieldWidget                  searchField;
    private int                              scroll = 0;

    public PresetPickerScreen(Screen parent, Consumer<String> callback) {
        super(ModTranslation.t("autoswap.screen.preset.title"));
        this.parent   = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        searchField = new TextFieldWidget(textRenderer, width / 2 - 110, PAD + 16, 220, 20,
                ModTranslation.t("autoswap.screen.picker.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(ModTranslation.t("autoswap.screen.picker.search")
                .copy().formatted(Formatting.DARK_GRAY));
        searchField.setChangedListener(q -> {
            scroll = 0;
            String query = q.trim().toLowerCase(Locale.ROOT);
            filtered = new ArrayList<>();
            for (ServerPresets.Preset p : ServerPresets.all())
                if (query.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(query)) filtered.add(p);
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);
        addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.picker.cancel"),
                btn -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - PAD - 20, 100, 20).build());
    }

    private int listTop()     { return PAD + 16 + 20 + PAD; }
    private int listBottom()  { return height - PAD - 20 - PAD; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_H); }
    private int maxScroll()   { return Math.max(0, filtered.size() - visibleRows()); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, PAD, 0xFFFFFFFF);

        int x = width / 2 - 110;
        for (int row = 0; row < visibleRows(); row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            ServerPresets.Preset p = filtered.get(idx);
            int y = listTop() + row * ROW_H;
            boolean hov = mouseX >= x && mouseX < x + 220 && mouseY >= y && mouseY < y + ROW_H;
            if (hov) ctx.fill(x, y, x + 220, y + ROW_H, 0x40FFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal(p.name()), x + 3, y + 3, 0xFFFFFFFF);
            int gw = textRenderer.getWidth(p.group());
            ctx.drawTextWithShadow(textRenderer, Text.literal(p.group()), x + 217 - gw, y + 3, 0xFF777777);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int x = width / 2 - 110;
        if (mx < x || mx >= x + 220) return false;
        for (int row = 0; row < visibleRows(); row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            int y = listTop() + row * ROW_H;
            if (my >= y && my < y + ROW_H) {
                callback.accept(filtered.get(idx).name());
                client.setScreen(parent);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = MathHelper.clamp(scroll - (int) Math.signum(v) * 2, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
