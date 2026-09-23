package org.funtown.autoswap.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.funtown.autoswap.swap.TargetSlot;

public class SwapPair {

    public String     itemId     = "minecraft:air";
    public String     itemId2    = "minecraft:air";
    public TargetSlot targetSlot = TargetSlot.CHESTPLATE;

    public String getItemAName() { return displayName(itemId); }
    public String getItemBName() { return displayName(itemId2); }

    private static String displayName(String id) {
        if (id == null || id.isEmpty() || "minecraft:air".equals(id)) return "Select...";
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            String name = new ItemStack(item).getHoverName().getString();
            return name.length() > 14 ? name.substring(0, 13) + "…" : name;
        } catch (Exception e) { return id; }
    }
}
