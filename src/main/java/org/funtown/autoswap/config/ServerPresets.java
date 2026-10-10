package org.funtown.autoswap.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class ServerPresets {

    public record Preset(String group, String name) {}

    private static final List<Preset> ALL = new ArrayList<>();

    static {
        
        String ft = "FunTime";
        for (String n : new String[]{"Крушителя", "Карателя", "Раздора", "Тирана", "Ярости", "Вихря", "Мрака", "Демона"})
            ALL.add(new Preset(ft, "Талисман " + n));
        for (String n : new String[]{"Афины", "Титана", "Хаоса", "Сатира", "Бестии", "Ареса", "Гидры", "Икара", "Эрида"})
            ALL.add(new Preset(ft, "Сфера " + n));

        
        
        String hw = "HolyWorld";
        for (String n : new String[]{"Цербера", "Флеша", "Армоталити", "Имморталити",
                "Инфинити", "Этернити", "Стингера", "Сатиры"})
            ALL.add(new Preset(hw, n));
    }

    private ServerPresets() {}

    public static List<Preset> all() {
        return Collections.unmodifiableList(ALL);
    }
}
