package org.funtown.autoswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.funtown.autoswap.config.AutoSwapConfig;
import org.funtown.autoswap.config.AutoSwapSettings;
import org.funtown.autoswap.config.ModTranslation;
import org.funtown.autoswap.config.SwapEntry;
import org.funtown.autoswap.hud.SwapHud;
import org.funtown.autoswap.swap.SwapExecutor;

import java.util.*;

@Environment(EnvType.CLIENT)
public class AutoSwapClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AutoSwapConfig.load();

        AutoSwapSettings settings = AutoSwapConfig.getInstance().settings;
        if (settings.language == null || settings.language.isEmpty()) {
            settings.language = detectMcLanguage();
            AutoSwapConfig.save();
        }
        ModTranslation.load(settings.language);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            SwapExecutor.tick(client);
            SwapHud.tick();

            if (client.currentScreen != null) return;

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

        HudRenderCallback.EVENT.register(SwapHud::render);
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