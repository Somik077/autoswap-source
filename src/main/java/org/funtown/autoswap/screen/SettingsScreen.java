package org.funtown.autoswap.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.AutoSwapSettings;
import org.funtown.autoswap.config.ModTranslation;

public class SettingsScreen extends Screen {

    private final Screen parent;
    private AutoSwapSettings s;
    private ButtonWidget actionBarBtn;
    private ButtonWidget langBtn;

    private static final int[] COOLDOWN_STEPS = {0, 100, 200, 300, 500, 750, 1000};

    public SettingsScreen(Screen parent) {
        super(ModTranslation.t("autoswap.screen.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        s = AutoSwapConfig.getInstance().settings;

        int cx = width / 2, y0 = height / 2 - 80, row = 38;

        
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), btn -> {
            s.inventoryOpenDelayTicks = Math.max(1, s.inventoryOpenDelayTicks - 1);
            AutoSwapConfig.save();
        }).dimensions(cx - 60, y0 + 16, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), btn -> {
            s.inventoryOpenDelayTicks = Math.min(10, s.inventoryOpenDelayTicks + 1);
            AutoSwapConfig.save();
        }).dimensions(cx + 40, y0 + 16, 20, 20).build());

        
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), btn -> {
            s.swapCooldownMs = prevStep(s.swapCooldownMs);
            AutoSwapConfig.save();
        }).dimensions(cx - 60, y0 + row + 16, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), btn -> {
            s.swapCooldownMs = nextStep(s.swapCooldownMs);
            AutoSwapConfig.save();
        }).dimensions(cx + 40, y0 + row + 16, 20, 20).build());

        
        actionBarBtn = ButtonWidget.builder(actionBarLabel(), btn -> {
            s.showActionBar = !s.showActionBar;
            actionBarBtn.setMessage(actionBarLabel());
            AutoSwapConfig.save();
        }).dimensions(cx - 60, y0 + row * 2 + 14, 120, 20).build();
        addDrawableChild(actionBarBtn);

        
        langBtn = ButtonWidget.builder(langLabel(), btn -> {
            
            String newLang = isRu() ? "en_us" : "ru_ru";
            s.language = newLang;
            AutoSwapConfig.save();
            ModTranslation.load(newLang);
            
            clearAndInit();
            
            if (parent instanceof AutoSwapConfigScreen cfg) cfg.refresh();
        }).dimensions(cx - 60, y0 + row * 3 + 12, 120, 20).build();
        addDrawableChild(langBtn);

        
        addDrawableChild(ButtonWidget.builder(
                ModTranslation.t("autoswap.screen.settings.done"),
                btn -> close()
        ).dimensions(cx - 50, y0 + row * 4 + 14, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int cx = width / 2, y0 = height / 2 - 80, row = 38;

        ctx.drawCenteredTextWithShadow(textRenderer,
                ModTranslation.t("autoswap.screen.settings.title"), cx, y0 - 10, 0xFFFFFFFF);

        
        ctx.drawCenteredTextWithShadow(textRenderer,
                ModTranslation.t("autoswap.screen.settings.delay"), cx, y0 + 4, 0xFFAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(s.inventoryOpenDelayTicks + " tick(s)"), cx, y0 + 20, 0xFFFFFFFF);

        
        ctx.drawCenteredTextWithShadow(textRenderer,
                ModTranslation.t("autoswap.screen.settings.cooldown"), cx, y0 + row + 4, 0xFFAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(s.swapCooldownMs + " ms"), cx, y0 + row + 20, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        AutoSwapConfig.save();
        assert client != null;
        client.setScreen(parent);
    }

    

    private boolean isRu() { return "ru_ru".equals(s.language); }

    private Text actionBarLabel() {
        return isActionBarOn()
                ? ModTranslation.t("autoswap.screen.settings.actionbar_on").copy().formatted(Formatting.GREEN)
                : ModTranslation.t("autoswap.screen.settings.actionbar_off").copy().formatted(Formatting.GRAY);
    }
    private boolean isActionBarOn() { return s.showActionBar; }

    private Text langLabel() {
        return isRu()
                ? Text.literal("🌐 English").formatted(Formatting.AQUA)
                : Text.literal("🌐 Русский").formatted(Formatting.AQUA);
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