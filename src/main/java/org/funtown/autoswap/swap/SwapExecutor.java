package org.funtown.autoswap.swap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.config.SwapPair;
import org.funtown.autoswap.hud.SwapHud;
import org.funtown.autoswap.screen.SilentInventoryScreen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class SwapExecutor {

    private enum State { IDLE, OPEN_SCREEN, WAITING, SWAPPING }

    private static State state = State.IDLE;

    private static final ArrayDeque<SwapPair> queue = new ArrayDeque<>();
    private static int             ticksWaited  = 0;
    private static long            lastSwapTime = 0L;
    private static boolean         screenOpened = false;
    private static boolean         wasSprinting = false;

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
        if (System.currentTimeMillis() - lastSwapTime
                < AutoSwapConfig.getInstance().settings.swapCooldownMs) return;
        queue.clear();
        for (SwapEntry entry : entries)
            for (SwapPair pair : entry.pairs) queue.add(pair);
        ticksWaited = 0;
        screenOpened = false;
        state = State.OPEN_SCREEN;
    }

    public static void tick(Minecraft client) {
        if (state == State.IDLE || client.player == null) return;

        if (screenOpened && client.screen == null) {
            finish(client);
            return;
        }

        switch (state) {
            case OPEN_SCREEN -> {
                wasSprinting = client.player.isSprinting();
                client.player.setSprinting(false);
                client.setScreen(new SilentInventoryScreen(client.player));
                screenOpened = true;
                ticksWaited = 0;
                state = State.WAITING;
            }
            case WAITING -> {
                int delay = Math.max(1, AutoSwapConfig.getInstance().settings.inventoryOpenDelayTicks);
                if (++ticksWaited < delay) return;
                hudIcons.clear();
                hudNames.clear();
                step = 0;
                state = State.SWAPPING;
            }
            case SWAPPING -> stepSwap(client);
        }
    }

    private static void stepSwap(Minecraft client) {
        if (step == 0) {
            SwapPair pair = queue.poll();
            if (pair == null) { finish(client); return; }
            startPair(client, pair);
            return;
        }

        InventoryMenu handler = client.player.inventoryMenu;
        int syncId = handler.containerId;

        if (step == 1) {
            if (offhandMode) {
                client.gameMode.handleContainerInput(syncId, src, 40, ContainerInput.SWAP, client.player);
                recordHud();
                step = 0;
            } else {
                client.gameMode.handleContainerInput(syncId, src, 0, ContainerInput.PICKUP, client.player);
                step = 2;
            }
            return;
        }
        if (step == 2) {
            client.gameMode.handleContainerInput(syncId, dst, 0, ContainerInput.PICKUP, client.player);
            step = 3;
            return;
        }
        if (!handler.getCarried().isEmpty())
            client.gameMode.handleContainerInput(syncId, src, 0, ContainerInput.PICKUP, client.player);
        recordHud();
        step = 0;
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

    private static void finish(Minecraft client) {
        if (screenOpened && client.screen != null) client.setScreen(null);
        if (wasSprinting) client.player.setSprinting(true);
        wasSprinting = false;
        if (!hudIcons.isEmpty() && AutoSwapConfig.getInstance().settings.showActionBar)
            SwapHud.showSuccess(hudIcons, hudNames);
        queue.clear();
        step = 0;
        screenOpened = false;
        lastSwapTime = System.currentTimeMillis();
        state = State.IDLE;
    }

    private static Item resolve(String id) {
        if (id == null || id.isEmpty() || "minecraft:air".equals(id)) return null;
        try { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); } catch (Exception e) { return null; }
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
