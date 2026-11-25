package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

/**
 * Releases unused memory when you leave a world. (The 1.8.9 version also de-duplicated model data;
 * current versions of the game already store models compactly, so that part is not needed here.)
 */
public class MemoryOptimizer extends Module {
    private boolean wasInWorld;
    private long lastGc;

    public MemoryOptimizer() { super("Memory Optimizer", "Frees memory when you leave a world.", Category.PERFORMANCE); }

    /** Called every client tick, also outside a world. */
    public void anyTick() {
        boolean in = mc.level != null;
        if (wasInWorld && !in && System.currentTimeMillis() - lastGc > 30000) {
            lastGc = System.currentTimeMillis();
            Thread t = new Thread(System::gc, "Lumen memory release");
            t.setDaemon(true);
            t.start();
        }
        wasInWorld = in;
    }
}
