package org.funtown.autoswap.swap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.funtown.autoswap.AutoSwapMod;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.ItemFilter;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.config.SwapPair;
import org.funtown.autoswap.hud.SwapHud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SwapExecutor {

    private enum State { IDLE, SWAPPING }

    
    private enum Mode { OFFHAND, TO_HOTBAR, DIRECT, VIA_HOTBAR }

    private static final Random RANDOM = new Random();

    private static final int  STEP_CLOSE         = 99;
    private static final int  OFFHAND_BUTTON     = 40;
    private static final int  TEMP_HOTBAR        = 8;
    private static final int  MIN_STEP_DELAY     = 2;
    private static final int  MIN_COOLDOWN_MS    = 150;
    private static final long RECENT_ACTION_MS   = 250L;
    private static final int  RECENT_ACTION_WAIT = 3;
    private static final int  USING_ITEM_WAIT    = 2;

    private static State state = State.IDLE;
    private static Mode  mode  = Mode.OFFHAND;

    private static final ArrayDeque<SwapPair> queue = new ArrayDeque<>();
    private static int  waitTicks    = 0;
    private static long lastSwapTime = 0L;
    private static volatile long lastActionMs = 0L;

    private static int  step   = 0;
    private static int  src    = -1;
    private static int  dst    = -1;
    private static int  clicks = 0;
    private static Item toEquip = null;

    private static final List<ItemStack> hudIcons = new ArrayList<>();
    private static final List<String>    hudNames = new ArrayList<>();

    public static boolean isSwapActive() {
        return state != State.IDLE;
    }

    
    public static void noteAction() {
        lastActionMs = System.currentTimeMillis();
    }

    private static boolean silencedReported = false;

    public static boolean reportSilenced(Object before) {
        if (silencedReported) return false;
        silencedReported = true;
        return true;
    }

    public static void schedule(List<SwapEntry> entries) {
        if (state != State.IDLE || entries.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;

        if (client.player.currentScreenHandler != client.player.playerScreenHandler) return;

        long now = System.currentTimeMillis();
        if (now - lastSwapTime < nextCooldown()) return;

        silencedReported = false;
        AutoSwapMod.LOGGER.info("[AutoSwap] swap start");

        queue.clear();
        for (SwapEntry entry : entries)
            for (SwapPair pair : entry.pairs) queue.add(pair);
        hudIcons.clear();
        hudNames.clear();
        step   = 0;
        clicks = 0;

        int extra = 0;

        
        if (client.player.isUsingItem()) {
            client.interactionManager.stopUsingItem(client.player);
            extra += USING_ITEM_WAIT;
        }

        
        if (now - lastActionMs < RECENT_ACTION_MS) {
            extra += RECENT_ACTION_WAIT;
        }

        waitTicks = nextStepDelay() + extra;
        state = State.SWAPPING;
    }

    public static void tick(MinecraftClient client) {
        if (state == State.IDLE || client.player == null || client.interactionManager == null) return;

        if (client.player.currentScreenHandler != client.player.playerScreenHandler) {
            abort(client);
            return;
        }

        if (--waitTicks > 0) return;
        stepSwap(client);
    }

    private static void stepSwap(MinecraftClient client) {
        if (step == STEP_CLOSE) {
            finish(client);
            return;
        }

        if (step == 0) {
            SwapPair pair = queue.poll();
            if (pair == null) { finish(client); return; }
            startPair(client, pair);
            if (step == 0) return;
        }

        int syncId = client.player.playerScreenHandler.syncId;

        switch (mode) {
            case OFFHAND -> {
                click(client, syncId, src, OFFHAND_BUTTON);
                pairDone();
            }
            case TO_HOTBAR -> {
                click(client, syncId, src, dst - 36);
                pairDone();
            }
            case DIRECT -> {
                click(client, syncId, dst, src - 36);
                pairDone();
            }
            case VIA_HOTBAR -> {
                if (step == 1) {
                    click(client, syncId, src, TEMP_HOTBAR);
                    step = 2;
                } else if (step == 2) {
                    click(client, syncId, dst, TEMP_HOTBAR);
                    step = 3;
                } else {
                    click(client, syncId, src, TEMP_HOTBAR);
                    pairDone();
                }
            }
        }

        waitTicks = nextStepDelay();
    }

    private static void click(MinecraftClient client, int syncId, int slot, int button) {
        client.interactionManager.clickSlot(syncId, slot, button, SlotActionType.SWAP, client.player);
        clicks++;
    }

    private static void pairDone() {
        recordHud();
        step = queue.isEmpty() ? STEP_CLOSE : 0;
    }

    private static void startPair(MinecraftClient client, SwapPair pair) {
        Item itemA = resolve(pair.itemId);
        Item itemB = resolve(pair.itemId2);
        if (itemA == null && itemB == null) return;

        PlayerInventory inv     = client.player.getInventory();
        int             tgtSlot = resolveTargetSlot(client, pair.targetSlot);
        ItemStack       inSlot  = client.player.playerScreenHandler.getSlot(tgtSlot).getStack();

        ItemFilter fA = pair.filterA, fB = pair.filterB;
        int excludeInv = (tgtSlot >= 36 && tgtSlot <= 44) ? tgtSlot - 36 : -1;

        boolean aOn = ItemMatcher.matches(inSlot, itemA, fA);
        boolean bOn = ItemMatcher.matches(inSlot, itemB, fB);

        
        int candA = itemA == null ? -1 : ItemMatcher.findBest(inv, itemA, fA, excludeInv);
        int candB = itemB == null ? -1 : ItemMatcher.findBest(inv, itemB, fB, excludeInv);

        
        
        boolean wantB;
        if (aOn && bOn)  wantB = candB != -1 || candA == -1;
        else if (aOn)    wantB = true;
        else if (bOn)    wantB = false;
        else             wantB = candA == -1 && candB != -1;

        Item want   = wantB ? itemB : itemA;
        int  srcInv = wantB ? candB : candA;
        toEquip = null;

        if (want == null) {
            
            if (!aOn && !bOn) {
                Item any = itemA != null ? itemA : itemB;
                if (any != null) SwapHud.showNotFound(name(any));
            }
            return;
        }
        toEquip = want;
        if (srcInv == -1) {
            if ((aOn || bOn) && itemA == itemB) return;
            SwapHud.showNotFound(name(want));
            return;
        }

        src = invToScreen(srcInv);
        dst = tgtSlot;
        if (src == dst) return;

        if (pair.targetSlot == TargetSlot.OFFHAND) {
            mode = Mode.OFFHAND;
        } else if (dst >= 36 && dst <= 44) {
            mode = Mode.TO_HOTBAR;
        } else if (src >= 36 && src <= 44) {
            mode = Mode.DIRECT;
        } else {
            mode = Mode.VIA_HOTBAR;
        }
        step = 1;
    }

    private static int resolveTargetSlot(MinecraftClient client, TargetSlot target) {
        if (target == TargetSlot.HELD) return 36 + client.player.getInventory().selectedSlot;
        return target.screenSlot;
    }

    private static void recordHud() {
        hudIcons.add(new ItemStack(toEquip));
        hudNames.add(name(toEquip));
    }

    private static void finish(MinecraftClient client) {
        
        if (clicks > 0 && client.player != null) {
            client.player.networkHandler.sendPacket(
                    new CloseHandledScreenC2SPacket(client.player.playerScreenHandler.syncId));
        }
        if (!hudIcons.isEmpty() && AutoSwapConfig.getInstance().settings.showActionBar)
            SwapHud.showSuccess(hudIcons, hudNames);
        AutoSwapMod.LOGGER.info("[AutoSwap] swap done, {} click(s)", clicks);
        queue.clear();
        step   = 0;
        clicks = 0;
        lastSwapTime = System.currentTimeMillis();
        state = State.IDLE;
    }

    private static void abort(MinecraftClient client) {
        queue.clear();
        hudIcons.clear();
        hudNames.clear();
        step   = 0;
        clicks = 0;
        state  = State.IDLE;
    }

    private static int nextStepDelay() {
        int base = Math.max(MIN_STEP_DELAY, AutoSwapConfig.getInstance().settings.stepDelayTicks);
        return base + RANDOM.nextInt(3);
    }

    private static long nextCooldown() {
        int ms = Math.max(MIN_COOLDOWN_MS, AutoSwapConfig.getInstance().settings.swapCooldownMs);
        return ms + RANDOM.nextInt(Math.max(1, ms / 2));
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

    private static int invToScreen(int i) { return i < 9 ? 36 + i : i; }
}
