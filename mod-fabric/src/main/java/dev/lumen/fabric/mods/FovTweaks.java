package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class FovTweaks extends Module {
    private final Setting.Num strength = num("Dynamic FOV Strength %", 0, 0, 100, 5);
    public FovTweaks() { super("Static FOV", "Limits sprint, speed and bow FOV changes.", Category.VISUAL); }
    public float apply(float modifier) { return 1f + (modifier - 1f) * (strength.f() / 100f); }
}
