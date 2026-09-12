package org.funtown.autoswap.swap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
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
        MinecraftClient client = MinecraftClient.getInstance();
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

    public static void tick(MinecraftClient client) {
        if (state == State.IDLE || client.player == null) return;

        if (screenOpened && client.currentScreen == null) {
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

    private static void stepSwap(MinecraftClient client) {
        if (step == 0) {
            SwapPair pair = queue.poll();
            if (pair == null) { finish(client); return; }
            startPair(client, pair);
            return; 
        }

        PlayerScreenHandler handler = client.player.playerScreenHandler;
        int syncId = handler.syncId;

        if (step == 1) {
            if (offhandMode) {
                client.interactionManager.clickSlot(syncId, src, 40, SlotActionType.SWAP, client.player);
                recordHud();
                step = 0;
            } else {
                client.interactionManager.clickSlot(syncId, src, 0, SlotActionType.PICKUP, client.player);
                step = 2;
            }
            return;
        }
        if (step == 2) {
            client.interactionManager.clickSlot(syncId, dst, 0, SlotActionType.PICKUP, client.player);
            step = 3;
            return;
        }
        if (!handler.getCursorStack().isEmpty())
            client.interactionManager.clickSlot(syncId, src, 0, SlotActionType.PICKUP, client.player);
        recordHud();
        step = 0;
    }

    private static void startPair(MinecraftClient client, SwapPair pair) {
        Item itemA = resolve(pair.itemId);
        Item itemB = resolve(pair.itemId2);
        if (itemA == null && itemB == null) return;

        PlayerInventory inv     = client.player.getInventory();
        int             tgtSlot = pair.targetSlot.screenSlot;
        ItemStack       inSlot  = client.player.playerScreenHandler.getSlot(tgtSlot).getStack();

        boolean aOn = itemA != null && !inSlot.isEmpty() && inSlot.isOf(itemA);
        boolean bOn = itemB != null && !inSlot.isEmpty() && inSlot.isOf(itemB);

        int srcInv = -1;
        toEquip = null;

        if (aOn) {
            if (itemB == null) return;
            srcInv  = findSlot(inv, itemB);
            toEquip = itemB;
            if (srcInv == -1) { SwapHud.showNotFound(itemB.getName().getString()); return; }
        } else if (bOn) {
            if (itemA == null) return;
            srcInv  = findSlot(inv, itemA);
            toEquip = itemA;
            if (srcInv == -1) { SwapHud.showNotFound(itemA.getName().getString()); return; }
        } else {
            if (itemA != null) { srcInv = findSlot(inv, itemA); toEquip = itemA; }
            if (srcInv == -1 && itemB != null) { srcInv = findSlot(inv, itemB); toEquip = itemB; }
            if (srcInv == -1) {
                String missingName = itemA != null
                        ? itemA.getName().getString()
                        : itemB.getName().getString();
                SwapHud.showNotFound(missingName);
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
        hudNames.add(toEquip.getName().getString());
    }

    private static void finish(MinecraftClient client) {
        if (screenOpened && client.currentScreen != null) client.setScreen(null);
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
        try { return Registries.ITEM.get(Identifier.of(id)); } catch (Exception e) { return null; }
    }
    private static int findSlot(PlayerInventory inv, Item item) {
        for (int i = 0; i < 36; i++) if (inv.getStack(i).isOf(item)) return i;
        return -1;
    }
    private static int invToScreen(int i) { return i < 9 ? 36 + i : i; }
}
