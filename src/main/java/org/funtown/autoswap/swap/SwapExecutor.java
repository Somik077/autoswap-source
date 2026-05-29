package org.funtown.autoswap.swap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.AutoSwapSettings;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.config.SwapPair;
import org.funtown.autoswap.hud.SwapHud;
import org.funtown.autoswap.screen.SilentInventoryScreen;

import java.util.ArrayList;
import java.util.List;

public class SwapExecutor {

    private enum State { IDLE, OPEN_SCREEN, WAITING, EXECUTE }

    private static State           state        = State.IDLE;
    private static List<SwapEntry> pending      = new ArrayList<>();
    private static int             ticksWaited  = 0;
    private static long            lastSwapTime = 0L;
    public static void schedule(List<SwapEntry> entries) {
        if (state != State.IDLE || entries.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        AutoSwapSettings s = AutoSwapConfig.getInstance().settings;
        if (System.currentTimeMillis() - lastSwapTime < s.swapCooldownMs) return;
        pending = new ArrayList<>(entries);
        state   = State.OPEN_SCREEN;
    }

    public static void tick(MinecraftClient client) {
        if (state == State.IDLE || client.player == null) return;

        switch (state) {
            case OPEN_SCREEN -> {
                if (isSourceInHotbar(client, pending)) {
                    client.setScreen(new SilentInventoryScreen(client.player));
                } else {
                    client.setScreen(new InventoryScreen(client.player));
                }
                ticksWaited = 0;
                state = State.WAITING;
            }
            case WAITING -> {
                int delay = AutoSwapConfig.getInstance().settings.inventoryOpenDelayTicks;
                if (++ticksWaited < Math.max(1, delay)) return;
                state = State.EXECUTE;
            }
            case EXECUTE -> {
                try { executePending(client); }
                finally {
                    client.setScreen(null);
                    pending.clear();
                    lastSwapTime = System.currentTimeMillis();
                    state = State.IDLE;
                }
            }
        }
    }

    private static boolean isSourceInHotbar(MinecraftClient client,
                                            List<SwapEntry> entries) {
        PlayerInventory     inv     = client.player.getInventory();
        PlayerScreenHandler handler = client.player.playerScreenHandler;

        for (SwapEntry entry : entries) {
            for (SwapPair pair : entry.pairs) {
                Item itemA = resolve(pair.itemId);
                Item itemB = resolve(pair.itemId2);
                if (itemA == null && itemB == null) continue;

                ItemStack inSlot = handler.getSlot(pair.targetSlot.screenSlot).getStack();
                boolean   aOn    = itemA != null && !inSlot.isEmpty() && inSlot.isOf(itemA);
                boolean   bOn    = itemB != null && !inSlot.isEmpty() && inSlot.isOf(itemB);

                int srcInv = -1;
                if (aOn)      { if (itemB != null) srcInv = findSlot(inv, itemB); }
                else if (bOn) { if (itemA != null) srcInv = findSlot(inv, itemA); }
                else {
                    if (itemA != null) srcInv = findSlot(inv, itemA);
                    if (srcInv == -1 && itemB != null) srcInv = findSlot(inv, itemB);
                }

                if (srcInv >= 9) return false;
            }
        }
        return true;
    }

    private static void executePending(MinecraftClient client) {
        PlayerScreenHandler handler = client.player.playerScreenHandler;
        int syncId = handler.syncId;

        List<ItemStack> equippedIcons = new ArrayList<>();
        List<String>    equippedNames = new ArrayList<>();

        for (SwapEntry entry : pending) {
            for (SwapPair pair : entry.pairs) {
                PairResult result = processPair(client, pair, handler, syncId);
                if (result != null) {
                    equippedIcons.add(result.icon());
                    equippedNames.add(result.name());
                }
            }
        }

        if (!equippedIcons.isEmpty()
                && AutoSwapConfig.getInstance().settings.showActionBar) {
            SwapHud.showSuccess(equippedIcons, equippedNames);
        }
    }

    private record PairResult(ItemStack icon, String name) {}

    private static PairResult processPair(MinecraftClient client, SwapPair pair,
                                          PlayerScreenHandler handler, int syncId) {
        Item itemA = resolve(pair.itemId);
        Item itemB = resolve(pair.itemId2);
        if (itemA == null && itemB == null) return null;

        PlayerInventory inv     = client.player.getInventory();
        int             tgtSlot = pair.targetSlot.screenSlot;
        ItemStack       inSlot  = handler.getSlot(tgtSlot).getStack();

        boolean aOn = itemA != null && !inSlot.isEmpty() && inSlot.isOf(itemA);
        boolean bOn = itemB != null && !inSlot.isEmpty() && inSlot.isOf(itemB);

        int  srcInv  = -1;
        Item toEquip = null;

        if (aOn) {
            if (itemB == null) return null;
            srcInv  = findSlot(inv, itemB);
            toEquip = itemB;
            if (srcInv == -1) {
                SwapHud.showNotFound(itemB.getName().getString());
                return null;
            }

        } else if (bOn) {
            if (itemA == null) return null;
            srcInv  = findSlot(inv, itemA);
            toEquip = itemA;
            if (srcInv == -1) {
                SwapHud.showNotFound(itemA.getName().getString());
                return null;
            }

        } else {
            if (itemA != null) { srcInv = findSlot(inv, itemA); toEquip = itemA; }
            if (srcInv == -1 && itemB != null) { srcInv = findSlot(inv, itemB); toEquip = itemB; }
            if (srcInv == -1) {
                String missingName = itemA != null
                        ? itemA.getName().getString()
                        : itemB.getName().getString();
                SwapHud.showNotFound(missingName);
                return null;
            }
        }

        int srcScreen = invToScreen(srcInv);

        if (pair.targetSlot == TargetSlot.OFFHAND) {
            client.interactionManager.clickSlot(syncId, srcScreen, 40,
                    SlotActionType.SWAP, client.player);
        } else {
            doSwap(client, syncId, srcScreen, tgtSlot, handler);
        }

        return new PairResult(new ItemStack(toEquip), toEquip.getName().getString());
    }

    private static void doSwap(MinecraftClient c, int syncId, int src, int dst,
                               PlayerScreenHandler h) {
        c.interactionManager.clickSlot(syncId, src, 0, SlotActionType.PICKUP, c.player);
        c.interactionManager.clickSlot(syncId, dst, 0, SlotActionType.PICKUP, c.player);
        if (!h.getCursorStack().isEmpty())
            c.interactionManager.clickSlot(syncId, src, 0, SlotActionType.PICKUP, c.player);
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