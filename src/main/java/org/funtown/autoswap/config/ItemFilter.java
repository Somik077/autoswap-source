package org.funtown.autoswap.config;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ItemFilter {

    public static class EnchantReq {
        public String id       = "";
        public int    minLevel = 1;

        public EnchantReq() {}
        public EnchantReq(String id, int minLevel) { this.id = id; this.minLevel = minLevel; }
    }

    public static final int NO_CMD = -1;
    public Integer          cmd                  = null;
    public List<EnchantReq> enchants             = new ArrayList<>();
    public int              minDurabilityPercent = 0;
    public String           nameContains         = "";

    public boolean hasName() { return nameContains != null && !nameContains.isBlank(); }

    public boolean isEmpty() {
        return cmd == null && (enchants == null || enchants.isEmpty()) && minDurabilityPercent <= 0 && !hasName();
    }

    public boolean isOnlyPlain() {
        return cmd != null && cmd == NO_CMD
                && (enchants == null || enchants.isEmpty()) && minDurabilityPercent <= 0 && !hasName();
    }

    public ItemFilter copy() {
        ItemFilter c = new ItemFilter();
        if (enchants != null)
            for (EnchantReq r : enchants) c.enchants.add(new EnchantReq(r.id, r.minLevel));
        c.cmd                  = cmd;
        c.minDurabilityPercent = minDurabilityPercent;
        c.nameContains         = nameContains == null ? "" : nameContains;
        return c;
    }

    public boolean matches(ItemStack stack) {
        if (cmd != null && cmdOf(stack) != cmd) return false;
        if (enchants != null) {
            for (EnchantReq r : enchants)
                if (levelOf(stack, r.id) < r.minLevel) return false;
        }
        if (minDurabilityPercent > 0 && durabilityPercent(stack) < minDurabilityPercent) return false;
        if (hasName()) {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (!name.contains(nameContains.trim().toLowerCase(Locale.ROOT))) return false;
        }
        return true;
    }

    public int score(ItemStack stack) {
        int s = 0;
        if (enchants != null)
            for (EnchantReq r : enchants) s += levelOf(stack, r.id) * 100;
        return s + durabilityPercent(stack);
    }

    public static int cmdOf(ItemStack stack) {
        CustomModelDataComponent c = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (c == null) return NO_CMD;
        List<Float> floats = c.floats();
        return floats == null || floats.isEmpty() ? NO_CMD : Math.round(floats.get(0));
    }

    public static ItemFilter identityOf(ItemStack stack, boolean withEnchants) {
        ItemFilter f = new ItemFilter();
        int c = cmdOf(stack);
        if (c != NO_CMD) {
            f.cmd = c;
        } else if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            f.nameContains = stack.getName().getString().trim();
        } else {
            f.cmd = NO_CMD;
        }
        if (withEnchants) {
            collect(stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), f);
            collect(stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), f);
        }
        return f;
    }

    public static ItemFilter fromStack(ItemStack stack) {
        ItemFilter f = new ItemFilter();
        collect(stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), f);
        collect(stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), f);
        return f.enchants.isEmpty() ? null : f;
    }

    private static void collect(ItemEnchantmentsComponent comp, ItemFilter into) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> e : comp.getEnchantmentEntries()) {
            Optional<RegistryKey<Enchantment>> key = e.getKey().getKey();
            if (key.isPresent())
                into.enchants.add(new EnchantReq(key.get().getValue().toString(), e.getIntValue()));
        }
    }

    public String describe() {
        List<String> parts = new ArrayList<>();
        if (cmd != null) parts.add(cmd == NO_CMD ? ModTranslation.get("autoswap.filter.cmd_none") : "CMD " + cmd);
        if (enchants != null)
            for (EnchantReq r : enchants) parts.add(enchantName(r.id) + " " + levelText(r.minLevel));
        if (minDurabilityPercent > 0) parts.add("≥" + minDurabilityPercent + "%");
        if (hasName()) parts.add("\"" + nameContains.trim() + "\"");
        return String.join(", ", parts);
    }

    public static String enchantName(String id) {
        int i = id.indexOf(':');
        String ns   = i < 0 ? "minecraft" : id.substring(0, i);
        String path = i < 0 ? id : id.substring(i + 1);
        return Text.translatable("enchantment." + ns + "." + path).getString();
    }

    public static String levelText(int level) {
        return level >= 1 && level <= 10
                ? Text.translatable("enchantment.level." + level).getString()
                : String.valueOf(level);
    }

    private static int durabilityPercent(ItemStack stack) {
        if (!stack.isDamageable()) return 100;
        int max = stack.getMaxDamage();
        return max <= 0 ? 100 : (max - stack.getDamage()) * 100 / max;
    }

    private static int levelOf(ItemStack stack, String id) {
        int a = levelIn(stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), id);
        int b = levelIn(stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), id);
        return Math.max(a, b);
    }

    private static int levelIn(ItemEnchantmentsComponent comp, String id) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> e : comp.getEnchantmentEntries()) {
            Optional<RegistryKey<Enchantment>> key = e.getKey().getKey();
            if (key.isPresent() && key.get().getValue().toString().equals(id)) return e.getIntValue();
        }
        return 0;
    }
}
