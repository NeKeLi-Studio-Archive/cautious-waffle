package pl.mapthio.menus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;

import java.util.Locale;

/** Pomocnicze metody do tekstów (kolory &amp; oraz hex &amp;#rrggbb). */
public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final Component PREFIX = Component.text("[Mapthio] ", NamedTextColor.GOLD);

    private Text() {
    }

    /** Tekst z kolorami (&a, &#ff8800), domyślnie bez kursywy. */
    public static Component parse(String raw) {
        return LEGACY.deserialize(raw == null ? "" : raw)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Zwykły tekst w kolorze, bez kursywy (do lore). */
    public static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    public static Component info(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.GRAY));
    }

    public static Component ok(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.GREEN));
    }

    public static Component error(String text) {
        return PREFIX.append(Component.text(text, NamedTextColor.RED));
    }

    /** Zamienia wpisaną nazwę (np. "diamond block") na przedmiot lub null. */
    public static Material parseMaterial(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String key = input.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        Material material = Material.matchMaterial(key);
        if (material == null || !material.isItem() || material.isAir()) {
            return null;
        }
        return material;
    }
}
