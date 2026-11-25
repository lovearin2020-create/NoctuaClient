package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Bed Wars: time until the next diamond and emerald. It reads the countdown hologram above a
 * generator whenever one is in view and keeps counting on its own after you walk away.
 */
public class ResourceTimer extends HudModule {
    /** Seconds per item for tier I, II, III. Only used until a hologram has been seen at that tier. */
    static final int[][] DEFAULTS = {{30, 23, 12}, {65, 50, 30}};
    private static final String[] NAMES = {"Diamond", "Emerald"}, ROMAN = {"I", "II", "III"};

    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool showTier = bool("Show Tier", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);

    private final long[] next = new long[2];
    private final int[] tier = new int[2], lastSeen = {-1, -1};
    private final int[][] learned = new int[2][3];
    private boolean active;
    private ItemStack[] icons;
    private int ticks;
    private Object world;

    public ResourceTimer() { super("Resource Timer", "Bed Wars: time until the next diamond and emerald.", Category.HYPIXEL, END, 4, CENTER, -76); }

    public float width() { return showTier.on() ? 66 : 50; }
    public float height() { return 36; }

    private int interval(int type) {
        int l = learned[type][tier[type]];
        return (l > 0 ? l : DEFAULTS[type][tier[type]]) * 1000;
    }

    private void start() {
        long now = System.currentTimeMillis();
        active = true;
        for (int t = 0; t < 2; t++) { tier[t] = 0; lastSeen[t] = -1; next[t] = now + interval(t); }
    }

    public boolean onChat(String raw) {
        if (!Servers.onHypixel()) return false;
        String msg = Servers.strip(raw);
        if (msg.indexOf(": ") > 0 && !msg.startsWith(" ")) return false;
        if (msg.contains("Protect your bed and destroy the enemy beds")) { start(); return false; }
        for (int t = 0; t < 2; t++) {
            if (!msg.contains(NAMES[t] + " Generators have been upgraded to Tier ")) continue;
            int nt = msg.contains("Tier III") ? 2 : 1;
            if (!active) start();
            tier[t] = nt; lastSeen[t] = -1;
            next[t] = System.currentTimeMillis() + interval(t);
        }
        return false;
    }

    private static String name(Entity e) { var n = e.getCustomName(); return n == null ? null : Servers.strip(n.getString()); }

    public void onTick() {
        if (mc.level != world) { world = mc.level; active = false; }
        if (mc.level == null || !Servers.onHypixel() || ++ticks % 5 != 0) return;
        List<Entity> stands = new ArrayList<>();
        for (Entity en : mc.level.entitiesForRendering()) if (en instanceof ArmorStand && en.getCustomName() != null) stands.add(en);
        long now = System.currentTimeMillis();
        boolean[] done = new boolean[2];
        for (Entity s : stands) {
            String n = name(s);
            int at = n.indexOf("Spawns in ");
            if (at < 0) continue;
            int secs;
            try { secs = Integer.parseInt(n.substring(at + 10).trim().split(" ")[0]); } catch (Exception ex) { continue; }
            int type = -1, seenTier = -1;
            for (Entity o : stands) {
                if (o == s || Math.abs(o.getX() - s.getX()) > 1.5 || Math.abs(o.getZ() - s.getZ()) > 1.5 || Math.abs(o.getY() - s.getY()) > 4) continue;
                String on = name(o).trim();
                if (on.equals("Diamond")) type = 0; else if (on.equals("Emerald")) type = 1;
                else if (on.startsWith("Tier ")) seenTier = on.endsWith("III") ? 2 : on.endsWith("II") ? 1 : 0;
            }
            if (type < 0 || done[type]) continue;
            done[type] = true;
            if (!active) start();
            if (seenTier >= 0 && seenTier != tier[type]) { tier[type] = seenTier; lastSeen[type] = -1; }
            if (secs != lastSeen[type]) {
                // The number just changed, so the next item is this many whole seconds away.
                if (lastSeen[type] >= 0 && secs > lastSeen[type] + 1) learned[type][tier[type]] = secs;
                if (lastSeen[type] >= 0 || Math.abs(next[type] - now - secs * 1000L) > 1000) next[type] = now + secs * 1000L;
                lastSeen[type] = secs;
            }
        }
        for (int t = 0; t < 2; t++) {
            if (!done[t]) lastSeen[t] = -1;
            while (active && next[t] <= now) next[t] += interval(t);
        }
    }

    public void draw(boolean editing) {
        if (!active && !editing) return;
        if (background.on()) Draw.roundRect(0, 0, width(), 36, 3, 0x70000000);
        long now = System.currentTimeMillis();
        for (int t = 0; t < 2; t++) {
            if (icons == null) icons = new ItemStack[]{new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD)};
            Draw.item(icons[t], 2, 1 + t * 17, false);
            int secs = editing && !active ? (t == 0 ? 12 : 41) : (int) Math.ceil(Math.max(0, next[t] - now) / 1000.0);
            Draw.text(secs + "s", 22, 6 + t * 17, secs <= 3 ? 0xFF4ADE80 : color.get(), true);
            if (showTier.on()) Draw.text(ROMAN[tier[t]], 24 + mc.font.width(secs + "s "), 6 + t * 17, 0xFFAAAAAA, true);
        }
    }
}
