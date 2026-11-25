package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;

public class Particles extends Module {
    private final Setting.Num multiplier = num("Hit Particle Multiplier", 2, 1, 10, 1);
    private final Setting.Bool alwaysCrit = bool("Always Show Crit Particles", false);
    private final Setting.Bool alwaysSharp = bool("Always Show Sharpness Particles", true);

    public Particles() { super("Particles", "More crit and sharpness particles on hit.", Category.VISUAL); }

    public void onAttack(Entity target) {
        var p = mc.player;
        if (p == null || mc.particleEngine == null) return;
        boolean crit = p.fallDistance > 0 && !p.onGround() && !p.onClimbable() && !p.isInWater() && !p.isPassenger();
        int extra = multiplier.i() - 1;
        if (crit || alwaysCrit.on()) for (int i = 0; i < (crit ? extra : extra + 1); i++) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
        if (alwaysSharp.on()) for (int i = 0; i < extra + 1; i++) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
    }
}
