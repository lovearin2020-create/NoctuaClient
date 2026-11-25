package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class AutoTip extends Module {
    private final Setting.Num interval = num("Interval (minutes)", 15, 5, 60, 5);
    private final Setting.Bool hide = bool("Hide Tip Messages", true);
    private long nextTip;
    private boolean wasOnHypixel;

    public AutoTip() { super("AutoTip", "Runs /tip all on a timer (Hypixel only).", Category.HYPIXEL); }

    public void onTick() {
        boolean on = Servers.onHypixel() && mc.getConnection() != null;
        long now = System.currentTimeMillis();
        if (on && !wasOnHypixel) nextTip = now + 15000;
        wasOnHypixel = on;
        if (on && now >= nextTip) {
            nextTip = now + (long) (interval.value * 60000);
            mc.getConnection().sendCommand("tip all");
        }
    }

    public boolean onChat(String raw) {
        if (!hide.on() || !Servers.onHypixel()) return false;
        String m = Servers.strip(raw);
        return m.startsWith("You tipped ") || m.startsWith("You already tipped everyone") || m.startsWith("No one has a network booster active")
                || m.startsWith("You've already tipped someone in the past hour");
    }
}
