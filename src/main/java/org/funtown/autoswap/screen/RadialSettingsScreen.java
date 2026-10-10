package org.funtown.autoswap.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.RadialConfig;
import org.funtown.autoswap.config.RadialSlot;
import org.lwjgl.glfw.GLFW;

public class RadialSettingsScreen extends Screen {

    private final Screen parent;
    private ButtonWidget keyBtn;
    private boolean      waitingKey = false;

    public RadialSettingsScreen(Screen parent) {
        super(ModTranslation.t("autoswap.screen.radial.title"));
        this.parent = parent;
    }

    private RadialConfig cfg() { return AutoSwapConfig.getInstance().radial; }

    private Text keyLabel() {
        return waitingKey
                ? ModTranslation.t("autoswap.screen.config.press_key").copy().formatted(Formatting.YELLOW)
                : Text.literal(cfg().getKeyDisplayName());
    }

    @Override
    protected void init() {
        RadialConfig cfg = cfg();
        int cx = width / 2, y0 = 34;

        keyBtn = ButtonWidget.builder(keyLabel(), btn -> {
            waitingKey = true;
            keyBtn.setMessage(keyLabel());
        }).dimensions(cx - 20, y0, 100, 20).build();
        addDrawableChild(keyBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), btn -> {
            cfg.sectors = Math.max(3, cfg.sectors - 1);
            AutoSwapConfig.save();
            clearAndInit();
        }).dimensions(cx - 20, y0 + 26, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), btn -> {
            cfg.sectors = Math.min(RadialConfig.MAX_SLOTS, cfg.sectors + 1);
            AutoSwapConfig.save();
            clearAndInit();
        }).dimensions(cx + 60, y0 + 26, 20, 20).build());

        int rowsTop = y0 + 58;
        for (int i = 0; i < cfg.sectors; i++) {
            int idx = i, ry = rowsTop + i * 24;
            RadialSlot rs = cfg.slots.get(i);

            addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.radial.pick"), btn ->
                    client.setScreen(new ItemPickerScreen(this, item -> {
                        rs.itemId = Registries.ITEM.getId(item).toString();
                        rs.filter = null;
                        rs.name   = "";
                        rs.setStack(null);
                        AutoSwapConfig.save();
                    }))
            ).dimensions(cx + 10, ry, 56, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("⚙"), btn -> {
                if (rs.isEmpty()) return;
                client.setScreen(new FilterScreen(this, rs.filter, f -> {
                    rs.filter = f;
                    AutoSwapConfig.save();
                }));
            }).dimensions(cx + 70, ry, 20, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("✕").formatted(Formatting.RED), btn -> {
                cfg.slots.get(idx).clear();
                AutoSwapConfig.save();
                clearAndInit();
            }).dimensions(cx + 94, ry, 20, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(ModTranslation.t("autoswap.screen.settings.done"), btn -> close())
                .dimensions(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        RadialConfig cfg = cfg();
        int cx = width / 2, y0 = 34;

        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, ModTranslation.t("autoswap.screen.radial.key"), cx - 130, y0 + 6, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, ModTranslation.t("autoswap.screen.radial.sectors"), cx - 130, y0 + 32, 0xFFAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(String.valueOf(cfg.sectors)), cx + 30, y0 + 32, 0xFFFFFFFF);

        int rowsTop = y0 + 58;
        for (int i = 0; i < cfg.sectors; i++) {
            int ry = rowsTop + i * 24;
            RadialSlot rs = cfg.slots.get(i);
            ctx.drawTextWithShadow(textRenderer, Text.literal((i + 1) + "."), cx - 130, ry + 6, 0xFF888888);

            Item item = rs.item();
            if (item != null && client.player != null) {
                ctx.drawItem(rs.displayStack(), cx - 116, ry + 2);
            } else {
                ctx.fill(cx - 116, ry + 2, cx - 100, ry + 18, 0x44AAAAAA);
            }

            String label;
            int color;
            if (item == null) {
                label = ModTranslation.get("autoswap.screen.radial.empty_slot");
                color = 0xFF666666;
            } else {
                label = textRenderer.trimToWidth(rs.displayName() + (rs.hasFilter() ? " ⚙" : ""), 96);
                color = 0xFFFFFFFF;
            }
            ctx.drawTextWithShadow(textRenderer, Text.literal(label), cx - 94, ry + 6, color);
        }

        ctx.drawCenteredTextWithShadow(textRenderer, ModTranslation.t("autoswap.screen.radial.info"),
                cx, height - 44, 0xFF777777);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingKey) {
            InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
            if (key.getCode() != GLFW.GLFW_KEY_ESCAPE) {
                cfg().keyName = key.getTranslationKey();
                AutoSwapConfig.save();
            }
            waitingKey = false;
            keyBtn.setMessage(keyLabel());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        AutoSwapConfig.save();
        client.setScreen(parent);
    }
}
