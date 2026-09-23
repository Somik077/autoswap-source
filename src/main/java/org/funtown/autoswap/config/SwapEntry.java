package org.funtown.autoswap.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class SwapEntry {
    public String         label   = "autoswap.entry.default_label";
    public List<SwapPair> pairs   = new ArrayList<>();
    public String         keyName = "key.keyboard.unknown";
    private transient boolean prevPressed = false;

    public boolean wasJustPressed(Minecraft client) {
        if ("key.keyboard.unknown".equals(keyName)) { prevPressed = false; return false; }
        InputConstants.Key key = resolveKey(keyName);
        if (key.equals(InputConstants.UNKNOWN)) { prevPressed = false; return false; }
        boolean pressed     = InputConstants.isKeyDown(client.getWindow(), key.getValue());
        boolean justPressed = pressed && !prevPressed;
        prevPressed         = pressed;
        return justPressed;
    }

    public String getKeyDisplayName() {
        if ("key.keyboard.unknown".equals(keyName)) return ModTranslation.get("autoswap.key.unbound");
        InputConstants.Key key = resolveKey(keyName);
        if (key.equals(InputConstants.UNKNOWN)) return ModTranslation.get("autoswap.key.unbound");
        return key.getDisplayName().getString();
    }

    public String getDisplayLabel() {
        String def = ModTranslation.get("autoswap.entry.default_label");
        boolean isDefault = label.equals("autoswap.entry.default_label") || label.equals(def);
        if (isDefault && !pairs.isEmpty()) {
            SwapPair p = pairs.get(0);
            return p.getItemAName() + " ↔ " + p.getItemBName();
        }
        return isDefault ? def : label;
    }

    private static InputConstants.Key resolveKey(String name) {
        try { return InputConstants.getKey(name); }
        catch (Exception e) { return InputConstants.UNKNOWN; }
    }
}
