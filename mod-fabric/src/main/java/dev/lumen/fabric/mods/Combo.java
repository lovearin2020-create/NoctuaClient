package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Combo extends TextHud {
    private int combo, pendingId = -1, lastHurt;
    private long lastHit;
    public Combo() { super("Combo Counter", "Counts hits you land in a row.", CENTER, -28, CENTER, 30); }
    public void onAttack(Entity target) { pendingId = target.getId(); }
    public void onTick() {
        if (pendingId != -1 && mc.level != null) {
            Entity e = mc.level.getEntity(pendingId);
            if (e instanceof LivingEntity l && l.hurtTime > 0 && l.hurtTime >= l.hurtDuration - 2) { combo++; lastHit = System.currentTimeMillis(); pendingId = -1; }
        }
        int h = mc.player.hurtTime;
        if (h > lastHurt) combo = 0;
        lastHurt = h;
        if (combo > 0 && System.currentTimeMillis() - lastHit > 3000) combo = 0;
    }
    protected String text(boolean editing) { return combo == 0 ? (editing ? "3 Combo" : "No Combo") : combo + " Combo"; }
}
