package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Titles extends Module {
    public final Setting.Bool hide = bool("Hide Titles", false);
    public final Setting.Num scale = num("Scale", 0.7, 0.3, 1.5, 0.05);
    public Titles() { super("Titles", "Resize or hide big on-screen titles.", Category.HUD); }
}
