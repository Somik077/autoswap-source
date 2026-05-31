package org.funtown.autoswap.swap;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotDetector {

    public static TargetSlot detectSlot(Item item) {
        ItemStack stack = new ItemStack(item);
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);

        if (equippable != null) {
            return switch (equippable.slot()) {
                case HEAD    -> TargetSlot.HELMET;
                case CHEST   -> TargetSlot.CHESTPLATE;
                case LEGS    -> TargetSlot.LEGGINGS;
                case FEET    -> TargetSlot.BOOTS;
                case OFFHAND -> TargetSlot.OFFHAND;
                default      -> TargetSlot.OFFHAND;
            };
        }

        
        return TargetSlot.OFFHAND;
    }
}