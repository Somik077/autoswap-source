package org.funtown.autoswap.config;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;


public class RadialSlot {

    public String     itemId = "";
    public ItemFilter filter = null;
    
    public String     name   = "";
    
    public JsonElement stack = null;

    private transient ItemStack cachedStack;
    private transient boolean   decodeTried;

    public boolean isEmpty() {
        return itemId == null || itemId.isEmpty() || "minecraft:air".equals(itemId);
    }

    public void clear() {
        itemId = "";
        filter = null;
        name   = "";
        setStack(null);
    }

    
    public void setStack(ItemStack source) {
        stack       = null;
        cachedStack = null;
        decodeTried = false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (source == null || source.isEmpty() || client.world == null) return;
        try {
            ItemStack one = source.copyWithCount(1);
            RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, client.world.getRegistryManager());
            stack       = ItemStack.CODEC.encodeStart(ops, one).result().orElse(null);
            cachedStack = one;
            decodeTried = true;
        } catch (Exception ignored) {
            stack = null;
        }
    }

    
    public ItemStack displayStack() {
        if (cachedStack != null) return cachedStack;
        Item item = item();
        if (item == null) return ItemStack.EMPTY;
        MinecraftClient client = MinecraftClient.getInstance();
        if (stack != null && !decodeTried && client.world != null) {
            decodeTried = true;
            try {
                RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, client.world.getRegistryManager());
                cachedStack = ItemStack.CODEC.parse(ops, stack).result().orElse(null);
            } catch (Exception ignored) {
                cachedStack = null;
            }
        }
        return cachedStack != null ? cachedStack : new ItemStack(item);
    }

    public String displayName() {
        if (name != null && !name.isEmpty()) return name;
        Item item = item();
        return item == null ? "" : item.getName().getString();
    }

    
    public int cmd() {
        return filter != null && filter.cmd != null ? filter.cmd : ItemFilter.NO_CMD;
    }

    public Item item() {
        if (isEmpty()) return null;
        try {
            Item item = Registries.ITEM.get(Identifier.of(itemId));
            return item == Items.AIR ? null : item;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasFilter() {
        return filter != null && !filter.isEmpty() && !filter.isOnlyPlain();
    }
}
