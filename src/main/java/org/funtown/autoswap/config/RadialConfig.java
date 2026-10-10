package org.funtown.autoswap.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

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

    public InputUtil.Key getKey() {
        try {
            return InputUtil.fromTranslationKey(keyName);
        } catch (Exception e) {
            return InputUtil.UNKNOWN_KEY;
        }
    }

    public boolean isKeyBound() {
        return !UNBOUND.equals(keyName) && !getKey().equals(InputUtil.UNKNOWN_KEY);
    }

    public boolean isKeyDown(MinecraftClient client) {
        if (!isKeyBound()) return false;
        return InputUtil.isKeyPressed(client.getWindow(), getKey().getCode());
    }

    public String getKeyDisplayName() {
        if (!isKeyBound()) return ModTranslation.get("autoswap.key.unbound");
        return getKey().getLocalizedText().getString();
    }
}
