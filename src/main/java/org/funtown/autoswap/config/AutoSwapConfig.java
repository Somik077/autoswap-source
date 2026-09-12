package org.funtown.autoswap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;

public class AutoSwapConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("autoswap.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static AutoSwapConfig instance = createDefault();

    public AutoSwapSettings settings     = new AutoSwapSettings();
    public java.util.List<Profile> profiles = new ArrayList<>();
    public int             activeProfile = 0;

    private static AutoSwapConfig createDefault() {
        AutoSwapConfig c = new AutoSwapConfig();
        c.profiles.add(new Profile("Default"));
        return c;
    }

    public static AutoSwapConfig getInstance() { return instance; }

    public Profile getActiveProfile() {
        if (profiles.isEmpty()) profiles.add(new Profile("Default"));
        if (activeProfile < 0 || activeProfile >= profiles.size()) activeProfile = 0;
        return profiles.get(activeProfile);
    }

    public java.util.List<SwapEntry> getEntries() {
        return getActiveProfile().entries;
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) { instance = createDefault(); return; }
        try (Reader r = new FileReader(file)) {
            AutoSwapConfig loaded = GSON.fromJson(r, AutoSwapConfig.class);
            instance = (loaded != null) ? loaded : createDefault();
            if (instance.profiles == null || instance.profiles.isEmpty())
                instance.profiles = new ArrayList<>();
            if (instance.profiles.isEmpty())
                instance.profiles.add(new Profile("Default"));
            if (instance.settings == null)
                instance.settings = new AutoSwapSettings();
        } catch (Exception e) {
            System.err.println("[AutoSwap] Failed to load config: " + e.getMessage());
            instance = createDefault();
        }
    }

    public static void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            System.err.println("[AutoSwap] Failed to save config: " + e.getMessage());
        }
    }
}