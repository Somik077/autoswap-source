package org.funtown.autoswap.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.AutoSwapSettings;
import org.funtown.autoswap.config.ModTranslation;

public class SettingsScreen extends Screen {

    private final Screen parent;
    private AutoSwapSettings s;
    private Button actionBarBtn;
    private Button langBtn;

    private static final int[] COOLDOWN_STEPS = {0, 100, 200, 300, 500, 750, 1000};

    public SettingsScreen(Screen parent) {
        super(ModTranslation.t("autoswap.screen.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        s = AutoSwapConfig.getInstance().settings;

        int cx = width / 2, y0 = height / 2 - 80, row = 38;

        addRenderableWidget(Button.builder(Component.literal("◀"), btn -> {
            s.inventoryOpenDelayTicks = Math.max(1, s.inventoryOpenDelayTicks - 1);
            AutoSwapConfig.save();
        }).bounds(cx - 60, y0 + 16, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▶"), btn -> {
            s.inventoryOpenDelayTicks = Math.min(10, s.inventoryOpenDelayTicks + 1);
            AutoSwapConfig.save();
        }).bounds(cx + 40, y0 + 16, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("◀"), btn -> {
            s.swapCooldownMs = prevStep(s.swapCooldownMs);
            AutoSwapConfig.save();
        }).bounds(cx - 60, y0 + row + 16, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▶"), btn -> {
            s.swapCooldownMs = nextStep(s.swapCooldownMs);
            AutoSwapConfig.save();
        }).bounds(cx + 40, y0 + row + 16, 20, 20).build());

        actionBarBtn = Button.builder(actionBarLabel(), btn -> {
            s.showActionBar = !s.showActionBar;
            actionBarBtn.setMessage(actionBarLabel());
            AutoSwapConfig.save();
        }).bounds(cx - 60, y0 + row * 2 + 14, 120, 20).build();
        addRenderableWidget(actionBarBtn);

        langBtn = Button.builder(langLabel(), btn -> {
            String newLang = isRu() ? "en_us" : "ru_ru";
            s.language = newLang;
            AutoSwapConfig.save();
            ModTranslation.load(newLang);

            rebuildWidgets();

            if (parent instanceof AutoSwapConfigScreen cfg) cfg.refresh();
        }).bounds(cx - 60, y0 + row * 3 + 12, 120, 20).build();
        addRenderableWidget(langBtn);

        addRenderableWidget(Button.builder(
                ModTranslation.t("autoswap.screen.settings.done"),
                btn -> onClose()
        ).bounds(cx - 50, y0 + row * 4 + 14, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        int cx = width / 2, y0 = height / 2 - 80, row = 38;

        ctx.centeredText(getFont(),
                ModTranslation.t("autoswap.screen.settings.title"), cx, y0 - 10, 0xFFFFFFFF);

        ctx.centeredText(getFont(),
                ModTranslation.t("autoswap.screen.settings.delay"), cx, y0 + 4, 0xFFAAAAAA);
        ctx.centeredText(getFont(),
                Component.literal(s.inventoryOpenDelayTicks + " tick(s)"), cx, y0 + 20, 0xFFFFFFFF);

        ctx.centeredText(getFont(),
                ModTranslation.t("autoswap.screen.settings.cooldown"), cx, y0 + row + 4, 0xFFAAAAAA);
        ctx.centeredText(getFont(),
                Component.literal(s.swapCooldownMs + " ms"), cx, y0 + row + 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        AutoSwapConfig.save();
        minecraft.setScreen(parent);
    }

    private boolean isRu() { return "ru_ru".equals(s.language); }

    private Component actionBarLabel() {
        return isActionBarOn()
                ? ModTranslation.t("autoswap.screen.settings.actionbar_on").copy().withStyle(ChatFormatting.GREEN)
                : ModTranslation.t("autoswap.screen.settings.actionbar_off").copy().withStyle(ChatFormatting.GRAY);
    }
    private boolean isActionBarOn() { return s.showActionBar; }

    private Component langLabel() {
        return isRu()
                ? Component.literal("🌐 English").withStyle(ChatFormatting.AQUA)
                : Component.literal("🌐 Русский").withStyle(ChatFormatting.AQUA);
    }

    private int nextStep(int cur) {
        for (int v : COOLDOWN_STEPS) if (v > cur) return v;
        return COOLDOWN_STEPS[COOLDOWN_STEPS.length - 1];
    }
    private int prevStep(int cur) {
        for (int i = COOLDOWN_STEPS.length - 1; i >= 0; i--)
            if (COOLDOWN_STEPS[i] < cur) return COOLDOWN_STEPS[i];
        return 0;
    }
}
