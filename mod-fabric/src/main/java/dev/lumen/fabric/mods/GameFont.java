package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.Category;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.core.Setting;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Draws the game's own text (chat, HUD, menus) with a PvP pack font, without the pack: the letters, digits and
 * punctuation come from the font's sheet, everything else (Hangul, symbols) from the game's usual font.
 */
public class GameFont extends Module {
    private static final Identifier DEFAULT = Identifier.withDefaultNamespace("default");
    private static final String[] NAMES = {"Circle", "Smooth"};
    private final Setting.Mode font = mode("Font", "Circle", NAMES);
    private final FontDescription[] fonts = new FontDescription[NAMES.length];

    public GameFont() { super("Game Font", "PvP pack letters for chat and HUD.", Category.VISUAL); }

    public FontDescription replace(FontDescription asked) {
        if (!(asked instanceof FontDescription.Resource r) || !r.id().equals(DEFAULT)) return asked;
        int i = Math.max(0, Math.min(NAMES.length - 1, font.index));
        FontDescription f = fonts[i];
        if (f == null) f = fonts[i] = new FontDescription.Resource(Identifier.fromNamespaceAndPath("lumen", "game_" + NAMES[i].toLowerCase()));
        return f;
    }
}
