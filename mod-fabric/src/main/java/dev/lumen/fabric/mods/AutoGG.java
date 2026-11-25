package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

public class AutoGG extends Module {
    /** End-of-game lines used across Hypixel game modes. */
    private static final String[] TRIGGERS = {
            "1st Killer - ", "1st Place - ", "Winner: ", " - Damage Dealt - ", "Winning Team - ", "1st - ",
            "Winners: ", "Winner - ", "Winners - ", "WINNER!", " won the game!", "Top Seeker: ", "1st Place: ",
            "Last team standing!", "Winner #1 (", "Top Survivors", "Most Kills: ", "Your Overall Winstreak:",
            "Reward Summary", "Match Results"
    };
    private final Setting.Text message = text("Message", "gg", 60);
    private final Setting.Num delay = num("Delay (seconds)", 1, 0, 5, 0.5);
    private final Setting.Bool others = bool("Other Servers (plain chat)", false);
    private int ticksLeft = -1;
    private long lastSent;

    public AutoGG() { super("AutoGG", "Says gg when a game ends.", Category.HYPIXEL); }

    public boolean onChat(String raw) {
        if (ticksLeft >= 0) return false;
        if (!Servers.onHypixel() && !others.on()) return false;
        if (System.currentTimeMillis() - lastSent < 10000) return false;
        String msg = Servers.strip(raw);
        // Ignore player chat ("name: message") so nobody can trigger it by typing a trigger phrase.
        int colon = msg.indexOf(": ");
        boolean playerChat = colon > 0 && colon < 40 && !msg.startsWith(" ") && !msg.startsWith("Winner") && !msg.startsWith("Top Seeker") && !msg.startsWith("1st Place") && !msg.startsWith("Most Kills") && !msg.startsWith("Your Overall");
        if (playerChat || msg.startsWith("<")) return false;
        for (String t : TRIGGERS) if (msg.contains(t)) { ticksLeft = (int) (delay.value * 20); break; }
        return false;
    }

    public void onTick() {
        if (ticksLeft < 0 || ticksLeft-- > 0) return;
        ticksLeft = -1;
        lastSent = System.currentTimeMillis();
        String msg = message.value.trim();
        if (msg.isEmpty() || msg.startsWith("/") || mc.getConnection() == null) return; // never run commands from this box
        if (Servers.onHypixel()) mc.getConnection().sendCommand("ac " + msg); else mc.getConnection().sendChat(msg);
    }
}
