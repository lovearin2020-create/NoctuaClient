package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

/** Frame-rate oriented tweaks. All of them only skip client-side rendering work, on top of what Sodium does. */
public class Performance extends Module {
    private final Setting.Num entityDist = num("Mob Render Distance", 64, 16, 128, 8);
    private final Setting.Num playerDist = num("Player Render Distance", 96, 32, 160, 8);
    private final Setting.Num itemDist = num("Dropped Item Render Distance", 32, 8, 96, 8);
    private final Setting.Num standDist = num("Armor Stand Render Distance", 48, 8, 128, 8);
    private final Setting.Num tileDist = num("Chest / Sign / Skull Render Distance", 48, 16, 96, 8);
    private final Setting.Num particles = num("Particles %", 100, 0, 100, 10);
    private final Setting.Num unfocused = num("FPS While Unfocused", 15, 5, 60, 5);
    private final Random rng = new Random();

    public Performance() { super("Performance", "Render-distance culling and other FPS tweaks.", Category.PERFORMANCE); }

    public boolean cull(Entity e, double camX, double camY, double camZ) {
        if (e == mc.player || e == mc.getCameraEntity()) return false;
        double d = e instanceof Player ? playerDist.value
                : e instanceof ItemEntity || e instanceof ItemFrame ? itemDist.value
                : e instanceof ArmorStand ? standDist.value : entityDist.value;
        double x = e.getX() - camX, y = e.getY() - camY, z = e.getZ() - camZ;
        return x * x + y * y + z * z > d * d;
    }

    public boolean cullTile(double distSq) { return distSq > tileDist.value * tileDist.value; }

    public boolean dropParticle() {
        double p = particles.value;
        return p < 100 && rng.nextInt(100) >= p;
    }

    public int frameLimit(int original) {
        try { if (!mc.isWindowActive() && mc.level != null) return Math.min(original, unfocused.i()); } catch (Throwable ignored) {}
        return original;
    }
}
