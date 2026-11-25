package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class FireOverlay extends Module {
    public final Setting.Num lower = num("Lower By", 0.25, 0, 0.5, 0.05);
    public FireOverlay() { super("Low Fire", "Lowers the fire overlay.", Category.VISUAL); }
}
