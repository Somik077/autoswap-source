package org.funtown.autoswap.swap;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.funtown.autoswap.config.ItemFilter;

public final class ItemMatcher {

    private ItemMatcher() {}

    public static boolean matches(ItemStack stack, Item item, ItemFilter filter) {
        if (item == null || stack.isEmpty() || stack.getItem() != item) return false;
        return filter == null || filter.matches(stack);
    }

    
    public static int findBest(Inventory inv, Item item, ItemFilter filter, int excludeInvSlot) {
        if (item == null) return -1;
        int best = -1, bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < 36; i++) {
            if (i == excludeInvSlot) continue;
            ItemStack s = inv.getItem(i);
            if (!matches(s, item, filter)) continue;
            int score = filter == null ? 0 : filter.score(s);
            if (score > bestScore) { best = i; bestScore = score; }
        }
        return best;
    }
}
