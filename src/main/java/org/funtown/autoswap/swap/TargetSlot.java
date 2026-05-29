package org.funtown.autoswap.swap;

import net.minecraft.text.Text;

public enum TargetSlot {
    HELMET     ("autoswap.slot.helmet",      5),
    CHESTPLATE ("autoswap.slot.chestplate",  6),
    LEGGINGS   ("autoswap.slot.leggings",    7),
    BOOTS      ("autoswap.slot.boots",       8),
    OFFHAND    ("autoswap.slot.offhand",    45);

    public final String translationKey;
    public final int    screenSlot;

    TargetSlot(String translationKey, int screenSlot) {
        this.translationKey = translationKey;
        this.screenSlot     = screenSlot;
    }

    public String getDisplayName() {
        return Text.translatable(translationKey).getString();
    }

    public TargetSlot next() {
        TargetSlot[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}