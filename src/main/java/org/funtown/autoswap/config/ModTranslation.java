package org.funtown.autoswap.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.text.Text;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class ModTranslation {

    private static final Gson GSON = new Gson();
    private static Map<String, String> strings = new HashMap<>();

    public static void load(String language) {
        Map<String, String> loaded = readFile(language);
        if (loaded.isEmpty() && !"en_us".equals(language)) {
            loaded = readFile("en_us"); 
        }
        strings = loaded;
    }

    private static Map<String, String> readFile(String lang) {
        String path = "/assets/autoswap/lang/" + lang + ".json";
        try (InputStream is = ModTranslation.class.getResourceAsStream(path)) {
            if (is == null) return new HashMap<>();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> result = GSON.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8), type);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            System.err.println("[AutoSwap] Could not load lang/" + lang + ".json: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static String get(String key) {
        return strings.getOrDefault(key, key);
    }

    public static Text t(String key) {
        return Text.literal(get(key));
    }

    public static Text t(String key, Object... args) {
        return Text.literal(String.format(get(key), args));
    }
}