package org.funtown.autoswap.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import org.funtown.autoswap.config.ModTranslation;

import java.util.ArrayList;
import java.util.List;

public class SwapHud {

    private record Entry(ItemStack icon, String text, int rgb) {}

    private static final List<Entry> entries   = new ArrayList<>();
    private static int               ticksLeft = 0;

    private static final int HOLD_TICKS  = 50;
    private static final int FADE_TICKS  = 20;
    private static final int TOTAL_TICKS = HOLD_TICKS + FADE_TICKS;

    

    public static void showSuccess(List<ItemStack> icons, List<String> names) {
        entries.clear();
        for (int i = 0; i < Math.min(icons.size(), names.size()); i++) {
            String text = (i == 0 ? "⚔ " : "") + names.get(i);
            entries.add(new Entry(icons.get(i), text, 0xFFFF55));
        }
        ticksLeft = TOTAL_TICKS;
    }

    public static void showNotFound(String itemName) {
        entries.clear();
        String template = ModTranslation.get("autoswap.hud.not_found");
        String msg = template.contains("%s") ? template.replace("%s", itemName)
                : itemName + " — " + template;
        entries.add(new Entry(ItemStack.EMPTY, "✗ " + msg, 0xFF5555));
        ticksLeft = TOTAL_TICKS;
    }

    public static void tick() {
        if (ticksLeft > 0) ticksLeft--;
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (ticksLeft <= 0 || entries.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        float alpha   = ticksLeft <= FADE_TICKS ? (float) ticksLeft / FADE_TICKS : 1f;
        int   a       = Math.max(4, (int)(alpha * 255));

        TextRenderer tr      = client.textRenderer;
        int          screenW = context.getScaledWindowWidth();
        int          screenH = context.getScaledWindowHeight();
        int          baseY   = screenH - 49; 
        int          iconSize = 16;
        int          iconGap  = 3;
        String       sep      = "  |  ";
        int          sepW     = tr.getWidth(sep);

        
        int totalW = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!e.icon().isEmpty()) totalW += iconSize + iconGap;
            totalW += tr.getWidth(e.text());
            if (i < entries.size() - 1) totalW += sepW;
        }

        int x     = (screenW - totalW) / 2;
        int iconY = baseY + (tr.fontHeight / 2) - (iconSize / 2);

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);

            
            
            if (!e.icon().isEmpty()) {
                context.drawItem(e.icon(), x, iconY);
                x += iconSize + iconGap;
            }

            
            context.drawText(tr, e.text(), x, baseY,
                    (a << 24) | (e.rgb() & 0x00FFFFFF), true);
            x += tr.getWidth(e.text());

            
            if (i < entries.size() - 1) {
                context.drawText(tr, sep, x, baseY,
                        (a << 24) | 0x888888, true);
                x += sepW;
            }
        }
    }
}