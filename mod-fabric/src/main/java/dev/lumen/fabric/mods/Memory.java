package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Memory extends TextHud {
    public Memory() { super("Memory", "Shows how much memory the game is using.", START, 64, START, 144); }
    protected String text(boolean editing) {
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
        return "Mem: " + (max > 0 ? used * 100 / max : 0) + "%  " + used + "MB";
    }
}
