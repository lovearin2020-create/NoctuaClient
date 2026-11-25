package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

public class TntTimer extends Module {
    private final Setting.Bool bedwars = bool("Hypixel Bed Wars Fuse Fix", true);
    public TntTimer() { super("TNT Timer", "Countdown above primed TNT.", Category.VISUAL); }

    /** Hypixel Bed Wars TNT explodes after 2.6s, but the client is told the vanilla 4s fuse. */
    private int offset() {
        if (!bedwars.on() || !Servers.onHypixel()) return 0;
        try {
            Objective o = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
            if (o != null && Servers.strip(o.getDisplayName().getString()).toUpperCase().contains("BED WARS")) return 28;
        } catch (Throwable ignored) {}
        return 0;
    }

    public void onFrame(float pt) {
        if (mc.level == null) return;
        int off = offset();
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof PrimedTnt tnt)) continue;
            float sec = Math.max(0, (tnt.getFuse() - off - pt) / 20f);
            int color = sec > 2 ? 0xFF55FF55 : sec > 1 ? 0xFFFFFF55 : 0xFFFF5555;
            Vec3 p = tnt.getPosition(pt).add(0, 1.35, 0);
            Gizmos.billboardText(String.format("%.1f", sec), p, TextGizmo.Style.forColorAndCentered(color).withScale(0.6f));
        }
    }
}
