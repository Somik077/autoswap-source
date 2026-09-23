package org.funtown.autoswap.swap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public class SlotDetector {

    public static TargetSlot detectSlot(Item item) {
        ItemStack stack = new ItemStack(item);
        Equippable equippable = stack.getComponents().get(DataComponents.EQUIPPABLE);

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
