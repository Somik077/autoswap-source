package org.funtown.autoswap.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.funtown.autoswap.config.ItemFilter;
import org.funtown.autoswap.config.ModTranslation;

import java.util.function.Consumer;


public class FilterScreen extends Screen {

    private static final int[] DURABILITY_STEPS = {0, 25, 50, 75, 90};
    private static final int   MAX_ENCHANTS     = 8;
    private static final int   ROW_H            = 22;
    private static final int   TOP              = 42;

    private final Screen               parent;
    private final ItemFilter           work;
    private final Consumer<ItemFilter> onDone;
    private ButtonWidget               durabilityBtn;
    private TextFieldWidget            nameField;
    private TextFieldWidget            cmdField;

    public FilterScreen(Screen parent, ItemFilter initial, Consumer<ItemFilter> onDone) {
        super(ModTranslation.t("autoswap.screen.filter.title"));
        this.parent = parent;
        this.onDone = onDone;
        this.work   = initial == null ? new ItemFilter() : initial.copy();
    }

    @Override
    protected void init() {
        int cx = width / 2;

        for (int i = 0; i < work.enchants.size(); i++) {
            ItemFilter.EnchantReq r = work.enchants.get(i);
            int idx = i, by = TOP + i * ROW_H;

            addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
                r.minLevel = Math.max(1, r.minLevel - 1);
            }).dimensions(cx + 20, by, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
                r.minLevel = Math.min(10, r.minLevel + 1);
            }).dimensions(cx + 64, by, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("✕").formatted(Formatting.RED), b -> {
                work.enchants.remove(idx);
                clearAndInit();
            }).dimensions(cx + 92, by, 20, 20).build());
        }

        int y = TOP + work.enchants.size() * ROW_H + 6;

        if (work.enchants.size() < MAX_ENCHANTS) {
            addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.filter.add_enchant"), b ->
                    client.setScreen(new EnchantPickerScreen(this, id -> {
                        for (ItemFilter.EnchantReq r : work.enchants) if (r.id.equals(id)) return;
                        work.enchants.add(new ItemFilter.EnchantReq(id, 1));
                    }))
            ).dimensions(cx - 100, y, 200, 20).build());
            y += 26;
        }

        durabilityBtn = ButtonWidget.builder(durabilityLabel(), b -> {
            int cur = 0;
            for (int i = 0; i < DURABILITY_STEPS.length; i++)
                if (DURABILITY_STEPS[i] == work.minDurabilityPercent) cur = i;
            work.minDurabilityPercent = DURABILITY_STEPS[(cur + 1) % DURABILITY_STEPS.length];
            durabilityBtn.setMessage(durabilityLabel());
        }).dimensions(cx - 100, y, 200, 20).build();
        addDrawableChild(durabilityBtn);
        y += 26;

        nameField = new TextFieldWidget(textRenderer, cx - 100, y, 122, 20,
                ModTranslation.t("autoswap.screen.filter.name"));
        nameField.setMaxLength(40);
        nameField.setText(work.nameContains == null ? "" : work.nameContains);
        nameField.setPlaceholder(ModTranslation.t("autoswap.screen.filter.name")
                .copy().formatted(Formatting.DARK_GRAY));
        nameField.setChangedListener(t -> work.nameContains = t);
        addDrawableChild(nameField);
        addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.filter.presets"), b ->
                client.setScreen(new PresetPickerScreen(this, name -> work.nameContains = name))
        ).dimensions(cx + 26, y, 74, 20).build());
        y += 26;

        cmdField = new TextFieldWidget(textRenderer, cx - 100, y, 92, 20,
                ModTranslation.t("autoswap.screen.filter.cmd"));
        cmdField.setMaxLength(10);
        cmdField.setTextPredicate(t -> t.matches("-?\\d{0,9}"));
        cmdField.setText(work.cmd == null ? "" : String.valueOf(work.cmd));
        cmdField.setPlaceholder(ModTranslation.t("autoswap.screen.filter.cmd")
                .copy().formatted(Formatting.DARK_GRAY));
        cmdField.setChangedListener(t -> {
            try {
                work.cmd = t.isEmpty() || t.equals("-") ? null : Integer.valueOf(t);
            } catch (NumberFormatException e) {
                work.cmd = null;
            }
        });
        addDrawableChild(cmdField);

        addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.filter.from_hand"), b -> {
            if (client.player == null || client.player.getMainHandStack().isEmpty()) return;
            ItemFilter id = ItemFilter.identityOf(client.player.getMainHandStack(), false);
            work.cmd          = id.cmd;
            work.nameContains = id.nameContains == null ? "" : id.nameContains;
            clearAndInit();
        }).dimensions(cx - 4, y, 104, 20).build());

        addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.filter.done"), b -> close())
                .dimensions(cx - 50, height - 28, 100, 20).build());
    }

    private Text durabilityLabel() {
        String value = work.minDurabilityPercent <= 0
                ? ModTranslation.get("autoswap.screen.filter.any")
                : work.minDurabilityPercent + "%";
        return ModTranslation.t("autoswap.screen.filter.durability", value);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, ModTranslation.t("autoswap.screen.filter.hint"), cx, 24, 0xFF888888);

        for (int i = 0; i < work.enchants.size(); i++) {
            ItemFilter.EnchantReq r = work.enchants.get(i);
            int by = TOP + i * ROW_H;
            ctx.drawTextWithShadow(textRenderer, Text.literal(ItemFilter.enchantName(r.id)), cx - 130, by + 6, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(ItemFilter.levelText(r.minLevel) + "+"),
                    cx + 52, by + 6, 0xFFFFFF55);
        }
    }

    @Override
    public void close() {
        onDone.accept(work.isEmpty() ? null : work);
        client.setScreen(parent);
    }
}
