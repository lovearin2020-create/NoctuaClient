package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class HitColor extends Module {
    private final Setting.Col color = col("Color", 0xFFFF0000);
    private final Setting.Num opacity = num("Opacity %", 30, 5, 80, 5);
    public HitColor() { super("Hit Color", "Color of the tint on damaged entities.", Category.VISUAL); }
    /** Overlay texel: the game blends towards this colour by (1 - alpha). */
    public int pixel() { return Math.round((1f - opacity.f() / 100f) * 255f) << 24 | (color.get() & 0xFFFFFF); }
}
