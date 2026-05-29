package org.funtown.autoswap.config;

import org.funtown.autoswap.swap.TargetSlot;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class SwapPair {

    public String     itemId     = "minecraft:air";
    public String     itemId2    = "minecraft:air";
    public TargetSlot targetSlot = TargetSlot.CHESTPLATE;

    

    public String getItemAName() { return displayName(itemId); }
    public String getItemBName() { return displayName(itemId2); }

    private static String displayName(String id) {
        if (id == null || id.isEmpty() || "minecraft:air".equals(id)) return "Select...";
        try {
            Item item = Registries.ITEM.get(Identifier.of(id));
            String name = item.getName().getString();
            return name.length() > 14 ? name.substring(0, 13) + "…" : name;
        } catch (Exception e) { return id; }
    }
}