package org.funtown.autoswap.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.RadialConfig;
import org.funtown.autoswap.config.RadialSlot;
import org.lwjgl.glfw.GLFW;

public class RadialSettingsScreen extends Screen {

    private final Screen parent;
    private Button keyBtn;
    private boolean      waitingKey = false;

    public RadialSettingsScreen(Screen parent) {
        super(ModTranslation.t("autoswap.screen.radial.title"));
        this.parent = parent;
    }

    private RadialConfig cfg() { return AutoSwapConfig.getInstance().radial; }

    private Component keyLabel() {
        return waitingKey
                ? ModTranslation.t("autoswap.screen.config.press_key").copy().withStyle(ChatFormatting.YELLOW)
                : Component.literal(cfg().getKeyDisplayName());
    }

    @Override
    protected void init() {
        RadialConfig cfg = cfg();
        int cx = width / 2, y0 = 34;

        keyBtn = Button.builder(keyLabel(), btn -> {
            waitingKey = true;
            keyBtn.setMessage(keyLabel());
        }).bounds(cx - 20, y0, 100, 20).build();
        addRenderableWidget(keyBtn);

        addRenderableWidget(Button.builder(Component.literal("◀"), btn -> {
            cfg.sectors = Math.max(3, cfg.sectors - 1);
            AutoSwapConfig.save();
            rebuildWidgets();
        }).bounds(cx - 20, y0 + 26, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("▶"), btn -> {
            cfg.sectors = Math.min(RadialConfig.MAX_SLOTS, cfg.sectors + 1);
            AutoSwapConfig.save();
            rebuildWidgets();
        }).bounds(cx + 60, y0 + 26, 20, 20).build());

        int rowsTop = y0 + 58;
        for (int i = 0; i < cfg.sectors; i++) {
            int idx = i, ry = rowsTop + i * 24;
            RadialSlot rs = cfg.slots.get(i);

            addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.radial.pick"), btn ->
                    minecraft.setScreen(new ItemPickerScreen(this, item -> {
                        rs.itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                        rs.filter = null;
                        rs.name   = "";
                        rs.setStack(null);
                        AutoSwapConfig.save();
                    }))
            ).bounds(cx + 10, ry, 56, 20).build());

            addRenderableWidget(Button.builder(Component.literal("⚙"), btn -> {
                if (rs.isEmpty()) return;
                minecraft.setScreen(new FilterScreen(this, rs.filter, f -> {
                    rs.filter = f;
                    AutoSwapConfig.save();
                }));
            }).bounds(cx + 70, ry, 20, 20).build());

            addRenderableWidget(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), btn -> {
                cfg.slots.get(idx).clear();
                AutoSwapConfig.save();
                rebuildWidgets();
            }).bounds(cx + 94, ry, 20, 20).build());
        }

        addRenderableWidget(Button.builder(ModTranslation.t("autoswap.screen.settings.done"), btn -> onClose())
                .bounds(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        RadialConfig cfg = cfg();
        int cx = width / 2, y0 = 34;

        ctx.centeredText(getFont(), title, cx, 10, 0xFFFFFFFF);
        ctx.text(getFont(), ModTranslation.t("autoswap.screen.radial.key"), cx - 130, y0 + 6, 0xFFAAAAAA);
        ctx.text(getFont(), ModTranslation.t("autoswap.screen.radial.sectors"), cx - 130, y0 + 32, 0xFFAAAAAA);
        ctx.centeredText(getFont(), Component.literal(String.valueOf(cfg.sectors)), cx + 30, y0 + 32, 0xFFFFFFFF);

        int rowsTop = y0 + 58;
        for (int i = 0; i < cfg.sectors; i++) {
            int ry = rowsTop + i * 24;
            RadialSlot rs = cfg.slots.get(i);
            ctx.text(getFont(), Component.literal((i + 1) + "."), cx - 130, ry + 6, 0xFF888888);

            Item item = rs.item();
            if (item != null && minecraft.player != null) {
                ctx.item(rs.displayStack(), cx - 116, ry + 2);
            } else {
                ctx.fill(cx - 116, ry + 2, cx - 100, ry + 18, 0x44AAAAAA);
            }

            String label;
            int color;
            if (item == null) {
                label = ModTranslation.get("autoswap.screen.radial.empty_slot");
                color = 0xFF666666;
            } else {
                label = trim(rs.displayName() + (rs.hasFilter() ? " ⚙" : ""), 96);
                color = 0xFFFFFFFF;
            }
            ctx.text(getFont(), Component.literal(label), cx - 94, ry + 6, color);
        }

        ctx.centeredText(getFont(), ModTranslation.t("autoswap.screen.radial.info"),
                cx, height - 44, 0xFF777777);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (waitingKey) {
            InputConstants.Key key = InputConstants.getKey(input);
            if (key.getValue() != GLFW.GLFW_KEY_ESCAPE) {
                cfg().keyName = key.getName();
                AutoSwapConfig.save();
            }
            waitingKey = false;
            keyBtn.setMessage(keyLabel());
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        AutoSwapConfig.save();
        minecraft.setScreen(parent);
    }

    private String trim(String text, int maxWidth) {
        if (getFont().width(text) <= maxWidth) return text;
        String t = text;
        while (!t.isEmpty() && getFont().width(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
