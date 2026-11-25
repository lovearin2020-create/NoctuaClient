package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.entity.Entity;

public class Reach extends TextHud {
    private double last;
    private long at;
    public Reach() { super("Reach Display", "Shows the distance of your last hit.", CENTER, -28, CENTER, 50); }
    public void onAttack(Entity target) {
        last = Math.sqrt(target.getBoundingBox().distanceToSqr(mc.player.getEyePosition()));
        at = System.currentTimeMillis();
    }
    protected String text(boolean editing) {
        if (System.currentTimeMillis() - at > 3000) return editing ? "2.87 blocks" : "No hit";
        return String.format("%.2f blocks", last);
    }
}
