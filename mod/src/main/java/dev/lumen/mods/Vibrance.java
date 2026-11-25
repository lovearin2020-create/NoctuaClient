package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

/** Richer colours for the world, like the Digital Vibrance slider of a graphics driver. The HUD keeps its own colours. */
public class Vibrance extends Module {
    public static Vibrance INSTANCE;
    public final Setting.Num amount = num("Amount (%)", 50, 5, 100, 5);

    public Vibrance() {
        super("Vibrance", "More vivid colours. Needs OptiFine Fast Render off.", Category.VISUAL);
        INSTANCE = this;
    }
}
