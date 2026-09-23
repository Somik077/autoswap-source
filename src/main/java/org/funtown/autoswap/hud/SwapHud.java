package org.funtown.autoswap.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.funtown.autoswap.config.ModTranslation;

import java.util.ArrayList;
import java.util.List;

public class SwapHud {

    private record Entry(ItemStack icon, Component text, int rgb) {}

    private static final List<Entry> entries   = new ArrayList<>();
    private static int               ticksLeft = 0;

    private static final int HOLD_TICKS  = 50;
    private static final int FADE_TICKS  = 20;
    private static final int TOTAL_TICKS = HOLD_TICKS + FADE_TICKS;

    public static void showSuccess(List<ItemStack> icons, List<String> names) {
        entries.clear();
        for (int i = 0; i < Math.min(icons.size(), names.size()); i++) {
            String text = (i == 0 ? "⚔ " : "") + names.get(i);
            entries.add(new Entry(icons.get(i), Component.literal(text), 0xFFFF55));
        }
        ticksLeft = TOTAL_TICKS;
    }

    public static void showNotFound(String itemName) {
        entries.clear();
        String template = ModTranslation.get("autoswap.hud.not_found");
        String msg = template.contains("%s") ? template.replace("%s", itemName)
                : itemName + " — " + template;
        entries.add(new Entry(ItemStack.EMPTY, Component.literal("✗ " + msg), 0xFF5555));
        ticksLeft = TOTAL_TICKS;
    }

    public static void tick() {
        if (ticksLeft > 0) ticksLeft--;
    }

    public static void extractRenderState(GuiGraphicsExtractor context, DeltaTracker delta) {
        if (ticksLeft <= 0 || entries.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        float alpha   = ticksLeft <= FADE_TICKS ? (float) ticksLeft / FADE_TICKS : 1f;
        int   a       = Math.max(4, (int)(alpha * 255));

        Font font    = client.font;
        int  screenW = client.getWindow().getGuiScaledWidth();
        int  screenH = client.getWindow().getGuiScaledHeight();
        int  baseY   = screenH - 49;
        int  iconSize = 16;
        int  iconGap  = 3;
        String sep    = "  |  ";
        int  sepW     = font.width(sep);

        int totalW = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!e.icon().isEmpty()) totalW += iconSize + iconGap;
            totalW += font.width(e.text());
            if (i < entries.size() - 1) totalW += sepW;
        }

        int x     = (screenW - totalW) / 2;
        int iconY = baseY + (font.lineHeight / 2) - (iconSize / 2);

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);

            if (!e.icon().isEmpty()) {
                context.item(e.icon(), x, iconY);
                x += iconSize + iconGap;
            }

            context.text(font, e.text(), x, baseY,
                    (a << 24) | (e.rgb() & 0x00FFFFFF), true);
            x += font.width(e.text());

            if (i < entries.size() - 1) {
                context.text(font, Component.literal(sep), x, baseY,
                        (a << 24) | 0x888888, true);
                x += sepW;
            }
        }
    }
}
