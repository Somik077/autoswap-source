package org.funtown.autoswap.swap;

import net.minecraft.text.Text;

public enum TargetSlot {
    HELMET     ("autoswap.slot.helmet",      5, -1),
    CHESTPLATE ("autoswap.slot.chestplate",  6, -1),
    LEGGINGS   ("autoswap.slot.leggings",    7, -1),
    BOOTS      ("autoswap.slot.boots",       8, -1),
    OFFHAND    ("autoswap.slot.offhand",    45, -1),

    HELD       ("autoswap.slot.held",       -1, -1),

    HOTBAR_1   ("autoswap.slot.hotbar",     36,  0),
    HOTBAR_2   ("autoswap.slot.hotbar",     37,  1),
    HOTBAR_3   ("autoswap.slot.hotbar",     38,  2),
    HOTBAR_4   ("autoswap.slot.hotbar",     39,  3),
    HOTBAR_5   ("autoswap.slot.hotbar",     40,  4),
    HOTBAR_6   ("autoswap.slot.hotbar",     41,  5),
    HOTBAR_7   ("autoswap.slot.hotbar",     42,  6),
    HOTBAR_8   ("autoswap.slot.hotbar",     43,  7),
    HOTBAR_9   ("autoswap.slot.hotbar",     44,  8);

    public final String translationKey;
    public final int    screenSlot;
    public final int    hotbarIndex;

    TargetSlot(String translationKey, int screenSlot, int hotbarIndex) {
        this.translationKey = translationKey;
        this.screenSlot     = screenSlot;
        this.hotbarIndex    = hotbarIndex;
    }

    public boolean isHotbarLike() {
        return this == HELD || hotbarIndex >= 0;
    }

    public String getDisplayName() {
        if (hotbarIndex >= 0) return Text.translatable(translationKey, hotbarIndex + 1).getString();
        return Text.translatable(translationKey).getString();
    }

    public TargetSlot next() {
        TargetSlot[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public TargetSlot prev() {
        TargetSlot[] v = values();
        return v[(ordinal() - 1 + v.length) % v.length];
    }
}
