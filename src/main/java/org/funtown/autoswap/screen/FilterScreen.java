package org.funtown.autoswap.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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
    private Button               durabilityBtn;
    private EditBox            nameField;
    private EditBox            cmdField;

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

            addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                r.minLevel = Math.max(1, r.minLevel - 1);
            }).bounds(cx + 20, by, 20, 20).build());

            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                r.minLevel = Math.min(10, r.minLevel + 1);
            }).bounds(cx + 64, by, 20, 20).build());

            addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), b -> {
                work.enchants.remove(idx);
                rebuildWidgets();
            }).bounds(cx + 92, by, 20, 20).build());
        }

        int y = TOP + work.enchants.size() * ROW_H + 6;

        if (work.enchants.size() < MAX_ENCHANTS) {
            addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.filter.add_enchant"), b ->
                    minecraft.setScreen(new EnchantPickerScreen(this, id -> {
                        for (ItemFilter.EnchantReq r : work.enchants) if (r.id.equals(id)) return;
                        work.enchants.add(new ItemFilter.EnchantReq(id, 1));
                    }))
            ).bounds(cx - 100, y, 200, 20).build());
            y += 26;
        }

        durabilityBtn = Button.builder(durabilityLabel(), b -> {
            int cur = 0;
            for (int i = 0; i < DURABILITY_STEPS.length; i++)
                if (DURABILITY_STEPS[i] == work.minDurabilityPercent) cur = i;
            work.minDurabilityPercent = DURABILITY_STEPS[(cur + 1) % DURABILITY_STEPS.length];
            durabilityBtn.setMessage(durabilityLabel());
        }).bounds(cx - 100, y, 200, 20).build();
        addRenderableWidget(durabilityBtn);
        y += 26;

        nameField = new EditBox(getFont(), cx - 100, y, 122, 20,
                ModTranslation.t("autoswap.screen.filter.name"));
        nameField.setMaxLength(40);
        nameField.setValue(work.nameContains == null ? "" : work.nameContains);
        nameField.setHint(ModTranslation.t("autoswap.screen.filter.name")
                .copy().withStyle(ChatFormatting.DARK_GRAY));
        nameField.setResponder(t -> work.nameContains = t);
        addRenderableWidget(nameField);
        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.filter.presets"), b ->
                minecraft.setScreen(new PresetPickerScreen(this, name -> work.nameContains = name))
        ).bounds(cx + 26, y, 74, 20).build());
        y += 26;

        cmdField = new EditBox(getFont(), cx - 100, y, 92, 20,
                ModTranslation.t("autoswap.screen.filter.cmd"));
        cmdField.setMaxLength(10);
        cmdField.setValue(work.cmd == null ? "" : String.valueOf(work.cmd));
        cmdField.setHint(ModTranslation.t("autoswap.screen.filter.cmd")
                .copy().withStyle(ChatFormatting.DARK_GRAY));
        cmdField.setResponder(t -> {
            try {
                work.cmd = t.matches("-?\\d{1,9}") ? Integer.valueOf(t) : null;
            } catch (NumberFormatException e) {
                work.cmd = null;
            }
        });
        addRenderableWidget(cmdField);

        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.filter.from_hand"), b -> {
            if (minecraft.player == null || minecraft.player.getMainHandItem().isEmpty()) return;
            ItemFilter id = ItemFilter.identityOf(minecraft.player.getMainHandItem(), false);
            work.cmd          = id.cmd;
            work.nameContains = id.nameContains == null ? "" : id.nameContains;
            rebuildWidgets();
        }).bounds(cx - 4, y, 104, 20).build());

        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.filter.done"), b -> onClose())
                .bounds(cx - 50, height - 28, 100, 20).build());
    }

    private Component durabilityLabel() {
        String value = work.minDurabilityPercent <= 0
                ? ModTranslation.get("autoswap.screen.filter.any")
                : work.minDurabilityPercent + "%";
        return ModTranslation.t("autoswap.screen.filter.durability", value);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.centeredText(getFont(), title, cx, 10, 0xFFFFFFFF);
        ctx.centeredText(getFont(), ModTranslation.t("autoswap.screen.filter.hint"), cx, 24, 0xFF888888);

        for (int i = 0; i < work.enchants.size(); i++) {
            ItemFilter.EnchantReq r = work.enchants.get(i);
            int by = TOP + i * ROW_H;
            ctx.text(getFont(), Component.literal(ItemFilter.enchantName(r.id)), cx - 130, by + 6, 0xFFFFFFFF);
            ctx.centeredText(getFont(), Component.literal(ItemFilter.levelText(r.minLevel) + "+"),
                    cx + 52, by + 6, 0xFFFFFF55);
        }
    }

    @Override
    public void onClose() {
        onDone.accept(work.isEmpty() ? null : work);
        minecraft.setScreen(parent);
    }
}
