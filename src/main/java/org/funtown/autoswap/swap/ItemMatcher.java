package org.funtown.autoswap.swap;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.funtown.autoswap.config.ItemFilter;

public final class ItemMatcher {

    private ItemMatcher() {}

    public static boolean matches(ItemStack stack, Item item, ItemFilter filter) {
        if (item == null || stack.isEmpty() || !stack.isOf(item)) return false;
        return filter == null || filter.matches(stack);
    }

    public static int findBest(PlayerInventory inv, Item item, ItemFilter filter, int excludeInvSlot) {
        if (item == null) return -1;
        int best = -1, bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < 36; i++) {
            if (i == excludeInvSlot) continue;
            ItemStack s = inv.getStack(i);
            if (!matches(s, item, filter)) continue;
            int score = filter == null ? 0 : filter.score(s);
            if (score > bestScore) { best = i; bestScore = score; }
        }
        return best;
    }
}
