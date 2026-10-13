package org.funtown.autoswap.swap;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;

public class SlotDetector {

    public static TargetSlot detectSlot(Item item) {
        if (item instanceof ArmorItem armor) {
            return switch (armor.getSlotType()) {
                case HEAD -> TargetSlot.HELMET;
                case CHEST -> TargetSlot.CHESTPLATE;
                case LEGS -> TargetSlot.LEGGINGS;
                case FEET -> TargetSlot.BOOTS;
                default -> TargetSlot.OFFHAND;
            };
        }
        if (item instanceof Equipment equipment) {
            EquipmentSlot slot = equipment.getSlotType();
            if (slot == EquipmentSlot.HEAD) return TargetSlot.HELMET;
            if (slot == EquipmentSlot.CHEST) return TargetSlot.CHESTPLATE;
            if (slot == EquipmentSlot.LEGS) return TargetSlot.LEGGINGS;
            if (slot == EquipmentSlot.FEET) return TargetSlot.BOOTS;
            if (slot == EquipmentSlot.OFFHAND) return TargetSlot.OFFHAND;
        }

        return TargetSlot.OFFHAND;
    }
}