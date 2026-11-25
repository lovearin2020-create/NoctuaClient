package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class Speedometer extends TextHud {
    private double speed;
    public Speedometer() { super("Speedometer", "Shows your horizontal speed in blocks per second.", START, 4, START, 144); }
    public void onTick() {
        double dx = mc.player.getX() - mc.player.xo, dz = mc.player.getZ() - mc.player.zo;
        speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
    }
    protected String text(boolean editing) { return String.format("%.2f b/s", speed); }
}
