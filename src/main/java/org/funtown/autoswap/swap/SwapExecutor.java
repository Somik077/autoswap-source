package org.funtown.autoswap.swap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (client.player.currentScreenHandler != client.player.playerScreenHandler) return;

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

    public static void tick(MinecraftClient client) {
        if (state == State.IDLE || client.player == null) return;

        if (client.player.currentScreenHandler != client.player.playerScreenHandler) {
            abort();
            return;
        }

        if (--waitTicks > 0) return;
        stepSwap(client);
    }

    private static void stepSwap(MinecraftClient client) {
        if (step == 0) {
            SwapPair pair = queue.poll();
            if (pair == null) { finish(); return; }
            startPair(client, pair);
            if (step != 0) waitTicks = nextStepDelay();
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
            waitTicks = nextStepDelay();
            return;
        }
        if (step == 2) {
            client.interactionManager.clickSlot(syncId, dst, 0, SlotActionType.PICKUP, client.player);
            step = 3;
            waitTicks = nextStepDelay();
            return;
        }
        if (!handler.getCursorStack().isEmpty())
            client.interactionManager.clickSlot(syncId, src, 0, SlotActionType.PICKUP, client.player);
        recordHud();
        step = 0;
        if (queue.isEmpty()) { finish(); return; }
        waitTicks = nextStepDelay();
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
            Item item = Registries.ITEM.get(Identifier.of(id));
            return item == Items.AIR ? null : item;
        } catch (Exception e) { return null; }
    }
    private static String name(Item item) {
        return item.getName().getString();
    }
    private static int findSlot(PlayerInventory inv, Item item) {
        for (int i = 0; i < 36; i++) if (inv.getStack(i).isOf(item)) return i;
        return -1;
    }
    private static int invToScreen(int i) { return i < 9 ? 36 + i : i; }
}
