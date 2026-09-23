package org.funtown.autoswap.swap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.config.SwapPair;
import org.funtown.autoswap.hud.SwapHud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SwapExecutor {

    private enum State { IDLE, SWAPPING }

    private static final Random RANDOM = new Random();

    private static State state = State.IDLE;

    private static final ArrayDeque<SwapPair> queue = new ArrayDeque<>();
    private static int             waitTicks    = 0;
    private static long            lastSwapTime = 0L;

    private static int  step        = 0;
    private static int  src         = -1;
    private static int  dst         = -1;
    private static Item toEquip     = null;
    private static boolean offhandMode = false;

    private static final List<ItemStack> hudIcons = new ArrayList<>();
    private static final List<String>    hudNames = new ArrayList<>();

    public static void schedule(List<SwapEntry> entries) {
        if (state != State.IDLE || entries.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (client.player.containerMenu != client.player.inventoryMenu) return;

        if (System.currentTimeMillis() - lastSwapTime < nextCooldown()) return;

        queue.clear();
        for (SwapEntry entry : entries)
            for (SwapPair pair : entry.pairs) queue.add(pair);
        hudIcons.clear();
        hudNames.clear();
        step = 0;
        waitTicks = nextStepDelay();
        state = State.SWAPPING;
    }

    public static void tick(Minecraft client) {
        if (state == State.IDLE || client.player == null) return;

        if (client.player.containerMenu != client.player.inventoryMenu) {
            abort();
            return;
        }

        if (--waitTicks > 0) return;
        stepSwap(client);
    }

    private static void stepSwap(Minecraft client) {
        if (step == 0) {
            SwapPair pair = queue.poll();
            if (pair == null) { finish(); return; }
            startPair(client, pair);
            if (step != 0) waitTicks = nextStepDelay();
            return;
        }

        InventoryMenu handler = client.player.inventoryMenu;
        int containerId = handler.containerId;

        if (step == 1) {
            if (offhandMode) {
                client.gameMode.handleContainerInput(containerId, src, 40, ContainerInput.SWAP, client.player);
                recordHud();
                step = 0;
            } else {
                client.gameMode.handleContainerInput(containerId, src, 0, ContainerInput.PICKUP, client.player);
                step = 2;
            }
            waitTicks = nextStepDelay();
            return;
        }
        if (step == 2) {
            client.gameMode.handleContainerInput(containerId, dst, 0, ContainerInput.PICKUP, client.player);
            step = 3;
            waitTicks = nextStepDelay();
            return;
        }
        if (!handler.getCarried().isEmpty())
            client.gameMode.handleContainerInput(containerId, src, 0, ContainerInput.PICKUP, client.player);
        recordHud();
        step = 0;
        if (queue.isEmpty()) { finish(); return; }
        waitTicks = nextStepDelay();
    }

    private static void startPair(Minecraft client, SwapPair pair) {
        Item itemA = resolve(pair.itemId);
        Item itemB = resolve(pair.itemId2);
        if (itemA == null && itemB == null) return;

        Inventory inv     = client.player.getInventory();
        int       tgtSlot = pair.targetSlot.screenSlot;
        ItemStack inSlot  = client.player.inventoryMenu.getSlot(tgtSlot).getItem();

        boolean aOn = itemA != null && !inSlot.isEmpty() && inSlot.getItem() == itemA;
        boolean bOn = itemB != null && !inSlot.isEmpty() && inSlot.getItem() == itemB;

        int srcInv = -1;
        toEquip = null;

        if (aOn) {
            if (itemB == null) return;
            srcInv  = findSlot(inv, itemB);
            toEquip = itemB;
            if (srcInv == -1) { SwapHud.showNotFound(name(itemB)); return; }
        } else if (bOn) {
            if (itemA == null) return;
            srcInv  = findSlot(inv, itemA);
            toEquip = itemA;
            if (srcInv == -1) { SwapHud.showNotFound(name(itemA)); return; }
        } else {
            if (itemA != null) { srcInv = findSlot(inv, itemA); toEquip = itemA; }
            if (srcInv == -1 && itemB != null) { srcInv = findSlot(inv, itemB); toEquip = itemB; }
            if (srcInv == -1) {
                SwapHud.showNotFound(name(itemA != null ? itemA : itemB));
                return;
            }
        }

        src = invToScreen(srcInv);
        dst = tgtSlot;
        offhandMode = pair.targetSlot == TargetSlot.OFFHAND;
        step = 1;
    }

    private static void recordHud() {
        hudIcons.add(new ItemStack(toEquip));
        hudNames.add(name(toEquip));
    }

    private static void finish() {
        if (!hudIcons.isEmpty() && AutoSwapConfig.getInstance().settings.showActionBar)
            SwapHud.showSuccess(hudIcons, hudNames);
        queue.clear();
        step = 0;
        lastSwapTime = System.currentTimeMillis();
        state = State.IDLE;
    }

    private static void abort() {
        queue.clear();
        hudIcons.clear();
        hudNames.clear();
        step = 0;
        state = State.IDLE;
    }

    private static int nextStepDelay() {
        int base = Math.max(1, AutoSwapConfig.getInstance().settings.stepDelayTicks);
        return base + RANDOM.nextInt(2);
    }

    private static long nextCooldown() {
        int ms = Math.max(0, AutoSwapConfig.getInstance().settings.swapCooldownMs);
        return ms == 0 ? 0L : ms + RANDOM.nextInt(Math.max(1, ms / 2));
    }

    private static Item resolve(String id) {
        if (id == null || id.isEmpty() || "minecraft:air".equals(id)) return null;
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            return item == Items.AIR ? null : item;
        } catch (Exception e) { return null; }
    }
    private static String name(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }
    private static int findSlot(Inventory inv, Item item) {
        for (int i = 0; i < 36; i++) if (inv.getItem(i).getItem() == item) return i;
        return -1;
    }
    private static int invToScreen(int i) { return i < 9 ? 36 + i : i; }
}
