package org.funtown.autoswap.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class RadialConfig {

    public static final int    MAX_SLOTS = 6;
    public static final String UNBOUND   = "key.keyboard.unknown";

    public String           keyName = UNBOUND;
    public int              sectors = 4;
    public List<RadialSlot> slots   = new ArrayList<>();

    public RadialConfig() { normalize(); }

    public void normalize() {
        if (slots == null) slots = new ArrayList<>();
        while (slots.size() < MAX_SLOTS) slots.add(new RadialSlot());
        for (int i = 0; i < slots.size(); i++)
            if (slots.get(i) == null) slots.set(i, new RadialSlot());
        sectors = Math.max(3, Math.min(MAX_SLOTS, sectors));
        if (keyName == null) keyName = UNBOUND;
    }

    public InputConstants.Key getKey() {
        try {
            return InputConstants.getKey(keyName);
        } catch (Exception e) {
            return InputConstants.UNKNOWN;
        }
    }

    public boolean isKeyBound() {
        return !UNBOUND.equals(keyName) && !getKey().equals(InputConstants.UNKNOWN);
    }

    public boolean isKeyDown(Minecraft client) {
        if (!isKeyBound()) return false;
        return InputConstants.isKeyDown(client.getWindow(), getKey().getValue());
    }

    public String getKeyDisplayName() {
        if (!isKeyBound()) return ModTranslation.get("autoswap.key.unbound");
        return getKey().getDisplayName().getString();
    }
}
