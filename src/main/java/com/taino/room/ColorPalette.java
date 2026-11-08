package com.taino.room;

import java.util.List;
import java.util.Map;

public final class ColorPalette {

    private static final Map<String, List<String>> BY_THEME = Map.of(
            "newyear", List.of(
                    "ny-frost", "ny-pine", "ny-midnight", "ny-berry", "ny-snow",
                    "ny-plum", "ny-spark", "ny-north", "ny-cocoa", "ny-dawn"
            ),
            "autumn", List.of(
                    "au-pumpkin", "au-moss", "au-chestnut", "au-fog", "au-grape",
                    "au-honey", "au-smoke", "au-rowan", "au-olive", "au-sand"
            ),
            "birthday", List.of(
                    "bd-sugar", "bd-lemon", "bd-sky", "bd-mint", "bd-lilac",
                    "bd-coral", "bd-ink", "bd-peach", "bd-lime", "bd-fuchsia"
            )
    );

    private ColorPalette() {
    }

    public static List<String> forTheme(String theme) {
        return BY_THEME.getOrDefault(theme == null ? "" : theme, BY_THEME.get("newyear"));
    }

    public static boolean isKnownTheme(String theme) {
        return BY_THEME.containsKey(theme);
    }
}
