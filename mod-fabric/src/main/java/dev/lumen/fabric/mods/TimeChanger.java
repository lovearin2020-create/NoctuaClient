package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class TimeChanger extends Module {
    private final Setting.Mode preset = mode("Time", "Day", "Day", "Sunset", "Night", "Custom");
    private final Setting.Num custom = num("Custom Time", 6000, 0, 24000, 500);
    public TimeChanger() { super("Time Changer", "Client-side time of day.", Category.VISUAL); }
    /** Clock value shown to the renderer (ticks into the day). Only your own sky and lighting change. */
    public long time() { return preset.is("Day") ? 6000 : preset.is("Sunset") ? 12500 : preset.is("Night") ? 18000 : (long) custom.value; }
}
