package org.funtown.autoswap.radial;

import net.minecraft.client.MinecraftClient;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.RadialConfig;
import org.funtown.autoswap.config.RadialSlot;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.config.SwapPair;
import org.funtown.autoswap.screen.RadialAssignScreen;
import org.funtown.autoswap.swap.SwapExecutor;
import org.funtown.autoswap.swap.TargetSlot;
import org.lwjgl.glfw.GLFW;

import java.util.List;


public final class RadialMenu {

    public static final int R_OUT = 84;
    public static final int R_IN  = 36;
    public static final int DEAD  = 18;

    private static final double CURSOR_MAX = 64.0;
    
    private static final double SENS = 1.2;

    private static boolean open;
    private static boolean prevKey, prevLmb, prevRmb;
    private static double  curX, curY;
    private static int     hovered = -1;

    private RadialMenu() {}

    public static boolean isOpen()      { return open; }
    public static int     hovered()     { return hovered; }
    public static double  cursorX()     { return curX; }
    public static double  cursorY()     { return curY; }

    
    public static void addDelta(double dx, double dy) {
        curX += dx * SENS;
        curY += dy * SENS;
        double len = Math.hypot(curX, curY);
        if (len > CURSOR_MAX) {
            curX *= CURSOR_MAX / len;
            curY *= CURSOR_MAX / len;
        }
    }

    public static void tick(MinecraftClient client) {
        RadialConfig cfg = AutoSwapConfig.getInstance().radial;
        if (client.player == null || cfg == null) {
            open = false;
            prevKey = false;
            return;
        }

        boolean keyDown = cfg.isKeyDown(client);

        
        if (client.currentScreen != null) {
            open = false;
            prevKey = keyDown;
            return;
        }

        if (!open) {
            if (keyDown && !prevKey && cfg.isKeyBound() && !SwapExecutor.isSwapActive()) {
                open = true;
                curX = 0;
                curY = 0;
                hovered = -1;
                prevLmb = mouseDown(client, GLFW.GLFW_MOUSE_BUTTON_LEFT);
                prevRmb = mouseDown(client, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            }
            prevKey = keyDown;
            return;
        }

        hovered = computeHovered(cfg.sectors);

        boolean lmb = mouseDown(client, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        boolean rmb = mouseDown(client, GLFW.GLFW_MOUSE_BUTTON_RIGHT);

        if (lmb && !prevLmb && hovered >= 0) {
            int idx = hovered;
            open = false;
            prevKey = keyDown;
            client.setScreen(new RadialAssignScreen(idx));
            return;
        }
        if (rmb && !prevRmb && hovered >= 0) {
            cfg.slots.get(hovered).clear();
            AutoSwapConfig.save();
        }
        prevLmb = lmb;
        prevRmb = rmb;

        if (!keyDown) {
            open = false;
            prevKey = false;
            if (hovered >= 0) fire(cfg.slots.get(hovered));
            return;
        }
        prevKey = true;
    }

    private static void fire(RadialSlot slot) {
        if (slot.isEmpty()) return;

        SwapPair pair   = new SwapPair();
        pair.itemId     = slot.itemId;
        pair.itemId2    = "";
        pair.targetSlot = TargetSlot.OFFHAND;
        pair.filterA    = slot.filter;

        SwapEntry entry = new SwapEntry();
        entry.pairs.add(pair);
        SwapExecutor.schedule(List.of(entry));
    }

    private static int computeHovered(int sectors) {
        if (Math.hypot(curX, curY) < DEAD) return -1;
        double angle = Math.atan2(curX, -curY);
        if (angle < 0) angle += Math.PI * 2;
        double step = Math.PI * 2 / sectors;
        return (int) Math.floor(((angle + step / 2) % (Math.PI * 2)) / step) % sectors;
    }

    private static boolean mouseDown(MinecraftClient client, int button) {
        return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), button) == GLFW.GLFW_PRESS;
    }
}
