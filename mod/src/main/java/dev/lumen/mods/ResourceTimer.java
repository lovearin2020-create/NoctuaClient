package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
    private static final ItemStack[] ICONS = {new ItemStack(Items.diamond), new ItemStack(Items.emerald)};

    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool showTier = bool("Show Tier", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);

    private final long[] next = new long[2];
    private final int[] tier = new int[2], lastSeen = {-1, -1};
    private final int[][] learned = new int[2][3];
    private boolean active;
    private int ticks;

    public ResourceTimer() { super("Resource Timer", "Bed Wars: time until the next diamond and emerald.", dev.lumen.core.Category.HYPIXEL, END, 4, CENTER, -76); }

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

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { active = false; }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Util.onHypixel()) return;
        String msg = Util.strip(e.message.getUnformattedText());
        if (msg.indexOf(": ") > 0 && !msg.startsWith(" ")) return;
        if (msg.contains("Protect your bed and destroy the enemy beds")) { start(); return; }
        for (int t = 0; t < 2; t++) {
            if (!msg.contains(NAMES[t] + " Generators have been upgraded to Tier ")) continue;
            int nt = msg.contains("Tier III") ? 2 : 1;
            if (!active) start();
            tier[t] = nt; lastSeen[t] = -1;
            next[t] = System.currentTimeMillis() + interval(t);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.theWorld == null || !Util.onHypixel() || ++ticks % 5 != 0) return;
        List<EntityArmorStand> stands = new ArrayList<EntityArmorStand>();
        for (Entity en : mc.theWorld.loadedEntityList) if (en instanceof EntityArmorStand && en.hasCustomName()) stands.add((EntityArmorStand) en);
        long now = System.currentTimeMillis();
        boolean[] done = new boolean[2];
        for (EntityArmorStand s : stands) {
            String n = Util.strip(s.getCustomNameTag());
            int at = n.indexOf("Spawns in ");
            if (at < 0) continue;
            int secs;
            try { secs = Integer.parseInt(n.substring(at + 10).trim().split(" ")[0]); } catch (Exception ex) { continue; }
            int type = -1, seenTier = -1;
            for (EntityArmorStand o : stands) {
                if (o == s || Math.abs(o.posX - s.posX) > 1.5 || Math.abs(o.posZ - s.posZ) > 1.5 || Math.abs(o.posY - s.posY) > 4) continue;
                String on = Util.strip(o.getCustomNameTag()).trim();
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
        float w = width();
        if (background.on()) box(0, 0, w, 36);
        long now = System.currentTimeMillis();
        for (int t = 0; t < 2; t++) {
            ArmorStatus.drawItem(ICONS[t], 2, 1 + t * 17);
            int secs = editing && !active ? (t == 0 ? 12 : 41) : (int) Math.ceil(Math.max(0, next[t] - now) / 1000.0);
            String s = secs + "s" + (showTier.on() ? " §7" + ROMAN[tier[t]] : "");
            R.mcText(s, 22, 6 + t * 17, secs <= 3 ? 0xFF4ADE80 : color.get(), true);
        }
    }
}
