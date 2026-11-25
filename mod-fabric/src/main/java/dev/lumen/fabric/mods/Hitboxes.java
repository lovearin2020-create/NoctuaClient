package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Styled version of the vanilla F3+B hitboxes. Boxes are depth-tested and skipped for invisible
 * entities, exactly like vanilla, so nothing is revealed that you could not already see.
 */
public class Hitboxes extends Module {
    private final Setting.Bool players = bool("Players", true);
    private final Setting.Bool mobs = bool("Mobs", false);
    private final Setting.Bool items = bool("Dropped Items", false);
    private final Setting.Bool projectiles = bool("Projectiles", true);
    private final Setting.Col color = col("Color", 0xFFFFFFFF);
    private final Setting.Num width = num("Line Width", 1.5, 1, 4, 0.5);
    private final Setting.Num range = num("Range", 32, 8, 64, 4);

    public Hitboxes() { super("Hitboxes", "Styled entity hitboxes (like F3+B).", Category.VISUAL); }

    private boolean wanted(Entity e) {
        if (e instanceof Player) return players.on();
        if (e instanceof LivingEntity) return mobs.on();
        if (e instanceof ItemEntity) return items.on();
        return e instanceof Projectile && projectiles.on();
    }

    public void onFrame(float pt) {
        Entity view = mc.getCameraEntity();
        if (mc.level == null || view == null) return;
        double r2 = range.value * range.value;
        GizmoStyle style = GizmoStyle.stroke(0xFF000000 | color.get(), width.f());
        for (Entity en : mc.level.entitiesForRendering()) {
            if (en == view || en == mc.player || en.isInvisible() || !wanted(en) || en.distanceToSqr(view) > r2) continue;
            Vec3 d = en.getPosition(pt).subtract(en.position());
            Gizmos.cuboid(en.getBoundingBox().move(d), style);
        }
    }
}
