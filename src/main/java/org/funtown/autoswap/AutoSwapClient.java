package org.funtown.autoswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.AutoSwapSettings;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.hud.SwapHud;
import org.funtown.autoswap.radial.RadialMenu;
import org.funtown.autoswap.radial.RadialRenderer;
import org.funtown.autoswap.swap.SwapExecutor;

import java.util.*;

@Environment(EnvType.CLIENT)
public class AutoSwapClient implements ClientModInitializer {

    private static boolean handBlocked() {
        return SwapExecutor.isSwapActive() || RadialMenu.isOpen();
    }

    @Override
    public void onInitializeClient() {
        AutoSwapConfig.load();

        AutoSwapSettings settings = AutoSwapConfig.getInstance().settings;
        if (settings.language == null || settings.language.isEmpty()) {
            settings.language = detectMcLanguage();
            AutoSwapConfig.save();
        }
        ModTranslation.load(settings.language);

        
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (handBlocked()) return true;
            SwapExecutor.noteAction();
            return false;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (handBlocked()) return ActionResult.FAIL;
            SwapExecutor.noteAction();
            return ActionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (handBlocked()) return ActionResult.FAIL;
            SwapExecutor.noteAction();
            return ActionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (handBlocked()) return ActionResult.FAIL;
            SwapExecutor.noteAction();
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            SwapExecutor.tick(client);
            SwapHud.tick();
            RadialMenu.tick(client);

            if (client.currentScreen != null) return;
            if (RadialMenu.isOpen()) return;

            List<SwapEntry> entries = AutoSwapConfig.getInstance().getEntries();

            Set<String> pressedKeys = new LinkedHashSet<>();
            for (SwapEntry entry : entries) {
                if (entry.wasJustPressed(client)) pressedKeys.add(entry.keyName);
            }
            if (pressedKeys.isEmpty()) return;

            String key = pressedKeys.iterator().next();
            List<SwapEntry> toFire = new ArrayList<>();
            for (SwapEntry entry : entries) {
                if (entry.keyName.equals(key) && !entry.pairs.isEmpty()) toFire.add(entry);
            }
            if (!toFire.isEmpty()) SwapExecutor.schedule(toFire);
        });

        HudElementRegistry.addLast(Identifier.of(AutoSwapMod.MOD_ID, "swap_hud"), SwapHud::render);
        HudElementRegistry.addLast(Identifier.of(AutoSwapMod.MOD_ID, "radial_hud"), RadialRenderer::render);
    }

    private static String detectMcLanguage() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null)
                return "ru_ru".equalsIgnoreCase(mc.options.language) ? "ru_ru" : "en_us";
        } catch (Exception ignored) {}
        return "en_us";
    }
}
