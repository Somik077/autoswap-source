package org.funtown.autoswap.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SwapEntry {

    public String         label   = "autoswap.entry.default_label"; 
    public List<SwapPair> pairs   = new ArrayList<>();
    public String         keyName = "key.keyboard.unknown";

    private transient boolean prevPressed = false;

    

    public boolean wasJustPressed(MinecraftClient client) {
        if ("key.keyboard.unknown".equals(keyName)) { prevPressed = false; return false; }
        InputUtil.Key key = InputUtil.fromTranslationKey(keyName);
        if (key.equals(InputUtil.UNKNOWN_KEY)) { prevPressed = false; return false; }
        boolean pressed     = InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
        boolean justPressed = pressed && !prevPressed;
        prevPressed         = pressed;
        return justPressed;
    }

    

    public String getKeyDisplayName() {
        if ("key.keyboard.unknown".equals(keyName))
            return Text.translatable("autoswap.key.unbound").getString();
        InputUtil.Key key = InputUtil.fromTranslationKey(keyName);
        if (key.equals(InputUtil.UNKNOWN_KEY))
            return Text.translatable("autoswap.key.unbound").getString();
        return key.getLocalizedText().getString();
    }

    public String getDisplayLabel() {
        String def = Text.translatable("autoswap.entry.default_label").getString();
        boolean isDefault = label.equals("autoswap.entry.default_label") || label.equals(def);
        if (isDefault && !pairs.isEmpty()) {
            SwapPair p = pairs.get(0);
            return p.getItemAName() + " ↔ " + p.getItemBName();
        }
        return isDefault ? def : label;
    }
}