package org.funtown.autoswap.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.funtown.autoswap.config.ItemFilter;
import org.funtown.autoswap.config.ModTranslation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EnchantPickerScreen extends Screen {

    private static final String[] VANILLA = {
            "minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
            "minecraft:projectile_protection", "minecraft:feather_falling", "minecraft:respiration",
            "minecraft:aqua_affinity", "minecraft:thorns", "minecraft:depth_strider",
            "minecraft:frost_walker", "minecraft:soul_speed", "minecraft:swift_sneak",
            "minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods",
            "minecraft:knockback", "minecraft:fire_aspect", "minecraft:looting",
            "minecraft:sweeping_edge", "minecraft:lunge", "minecraft:efficiency", "minecraft:silk_touch",
            "minecraft:unbreaking", "minecraft:fortune", "minecraft:power", "minecraft:punch",
            "minecraft:flame", "minecraft:infinity", "minecraft:luck_of_the_sea", "minecraft:lure",
            "minecraft:loyalty", "minecraft:impaling", "minecraft:riptide", "minecraft:channeling",
            "minecraft:multishot", "minecraft:quick_charge", "minecraft:piercing",
            "minecraft:density", "minecraft:breach", "minecraft:wind_burst", "minecraft:mending",
            "minecraft:binding_curse", "minecraft:vanishing_curse"
    };

    private static final int ROW_H = 13, PAD = 8;

    private final Screen           parent;
    private final Consumer<String> callback;
    private final List<String>     all = new ArrayList<>();
    private List<String>           filtered;
    private TextFieldWidget        searchField;
    private int                    scroll = 0;

    public EnchantPickerScreen(Screen parent, Consumer<String> callback) {
        super(ModTranslation.t("autoswap.screen.enchant.title"));
        this.parent   = parent;
        this.callback = callback;
        all.addAll(Arrays.asList(VANILLA));
        all.sort((a, b) -> ItemFilter.enchantName(a).compareToIgnoreCase(ItemFilter.enchantName(b)));
        filtered = new ArrayList<>(all);
    }

    @Override
    protected void init() {
        searchField = new TextFieldWidget(textRenderer, width / 2 - 110, PAD + 16, 220, 20,
                ModTranslation.t("autoswap.screen.picker.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(ModTranslation.t("autoswap.screen.picker.search")
                .copy().formatted(net.minecraft.util.Formatting.DARK_GRAY));
        searchField.setChangedListener(q -> {
            scroll = 0;
            String query = q.trim().toLowerCase(Locale.ROOT);
            filtered = new ArrayList<>();
            for (String id : all) {
                if (query.isEmpty()
                        || ItemFilter.enchantName(id).toLowerCase(Locale.ROOT).contains(query)
                        || id.contains(query)) filtered.add(id);
            }
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);
        addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.picker.cancel"),
                btn -> client.setScreen(parent)
        ).dimensions(width / 2 - 50, height - PAD - 20, 100, 20).build());
    }

    private int listTop()    { return PAD + 16 + 20 + PAD; }
    private int listBottom() { return height - PAD - 20 - PAD; }
    private int visibleRows() { return Math.max(1, (listBottom() - listTop()) / ROW_H); }
    private int maxScroll()  { return Math.max(0, filtered.size() - visibleRows()); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, PAD, 0xFFFFFFFF);

        int x = width / 2 - 110;
        for (int row = 0; row < visibleRows(); row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            int y = listTop() + row * ROW_H;
            boolean hov = mouseX >= x && mouseX < x + 220 && mouseY >= y && mouseY < y + ROW_H;
            if (hov) ctx.fill(x, y, x + 220, y + ROW_H, 0x40FFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal(ItemFilter.enchantName(filtered.get(idx))),
                    x + 3, y + 3, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean consumed) {
        if (super.mouseClicked(click, consumed)) return true;
        double mx = click.x(), my = click.y();
        int x = width / 2 - 110;
        if (mx < x || mx >= x + 220) return false;
        for (int row = 0; row < visibleRows(); row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            int y = listTop() + row * ROW_H;
            if (my >= y && my < y + ROW_H) {
                callback.accept(filtered.get(idx));
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
    public boolean keyPressed(KeyInput input) {
        if (net.minecraft.client.util.InputUtil.fromKeyCode(input).getCode() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }
}
