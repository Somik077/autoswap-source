package org.funtown.autoswap.radial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.RadialConfig;
import org.funtown.autoswap.config.RadialSlot;

public final class RadialRenderer {

    private static final int BASE  = 0x88202020;
    private static final int HOVER = 0xAA8A8A8A;

    private RadialRenderer() {}

    public static void extractRenderState(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        if (!RadialMenu.isOpen()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        RadialConfig cfg = AutoSwapConfig.getInstance().radial;
        Font tr = client.font;
        int n  = cfg.sectors;
        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;
        int hovered = RadialMenu.hovered();

        double step = Math.PI * 2 / n;
        double gap  = 0.025;

        for (int i = 0; i < n; i++) {
            double a0 = (i - 0.5) * step + gap;
            double a1 = (i + 0.5) * step - gap;
            fillSector(ctx, cx, cy, RadialMenu.R_IN, RadialMenu.R_OUT, a0, a1, i == hovered ? HOVER : BASE);
        }

        double rMid = (RadialMenu.R_IN + RadialMenu.R_OUT) / 2.0;
        for (int i = 0; i < n; i++) {
            int ix = cx + (int) Math.round(Math.sin(i * step) * rMid);
            int iy = cy - (int) Math.round(Math.cos(i * step) * rMid);
            RadialSlot slot = cfg.slots.get(i);
            Item item = slot.item();
            if (item == null) {
                ctx.centeredText(tr, Component.literal("+"), ix, iy - 4, 0xFF888888);
            } else {
                ctx.item(slot.displayStack(), ix - 8, iy - 8);
                if (slot.hasFilter())
                    ctx.text(tr, Component.literal("⚙"), ix + 5, iy + 3, 0xFFFFFF55);
            }
        }

        int dotX = cx + (int) RadialMenu.cursorX();
        int dotY = cy + (int) RadialMenu.cursorY();
        ctx.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFFFFFFFF);

        int textY = cy + RadialMenu.R_OUT + 8;
        if (hovered >= 0) {
            RadialSlot slot = cfg.slots.get(hovered);
            Item item = slot.item();
            if (item != null) {
                ctx.centeredText(tr, Component.literal(slot.displayName()), cx, textY, 0xFFFFFFFF);
                if (slot.hasFilter())
                    ctx.centeredText(tr, Component.literal(slot.filter.describe()), cx, textY + 11, 0xFFFFFF55);
            } else {
                ctx.centeredText(tr, ModTranslation.t("autoswap.screen.radial.empty_slot"),
                        cx, textY, 0xFF888888);
            }
        }
        ctx.centeredText(tr, ModTranslation.t("autoswap.radial.hint"), cx, textY + 24, 0xFF777777);
    }

    
    private static void fillSector(GuiGraphicsExtractor ctx, int cx, int cy, int r0, int r1,
                                   double a0, double a1, int color) {
        double d0x = Math.sin(a0), d0y = -Math.cos(a0);
        double d1x = Math.sin(a1), d1y = -Math.cos(a1);

        for (int dy = -r1; dy <= r1; dy++) {
            double wo2 = (double) r1 * r1 - (double) dy * dy;
            if (wo2 < 0) continue;
            double wo = Math.sqrt(wo2);

            double[] range = {-wo, wo};
            
            if (!clip(range, -d0y, d0x * dy)) continue;
            if (!clip(range, d1y, -d1x * dy)) continue;

            double wi2 = (double) r0 * r0 - (double) dy * dy;
            if (wi2 > 0) {
                double wi = Math.sqrt(wi2);
                span(ctx, cx, cy + dy, range[0], Math.min(range[1], -wi), color);
                span(ctx, cx, cy + dy, Math.max(range[0], wi), range[1], color);
            } else {
                span(ctx, cx, cy + dy, range[0], range[1], color);
            }
        }
    }

    
    private static boolean clip(double[] range, double a, double b) {
        if (Math.abs(a) < 1e-9) return b >= 0;
        double x0 = -b / a;
        if (a > 0) range[0] = Math.max(range[0], x0);
        else       range[1] = Math.min(range[1], x0);
        return range[0] <= range[1];
    }

    private static void span(GuiGraphicsExtractor ctx, int cx, int y, double lo, double hi, int color) {
        if (hi - lo < 1) return;
        ctx.fill(cx + (int) Math.ceil(lo), y, cx + (int) Math.floor(hi), y + 1, color);
    }
}
