package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import java.util.ArrayList;
import java.util.List;

/** Counts what you did since the game was started, read from Hypixel's own chat lines and titles. */
public class SessionStats extends HudModule {
    private final Setting.Bool showTime = bool("Session Time", true);
    private final Setting.Bool showKills = bool("Kills", true);
    private final Setting.Bool showFinals = bool("Final Kills", true);
    private final Setting.Bool showBeds = bool("Beds Broken", true);
    private final Setting.Bool showDeaths = bool("Deaths", true);
    private final Setting.Bool showWins = bool("Wins / Losses", true);
    private final Setting.Bool showFkdr = bool("FKDR", true);
    private final Setting.Bool showBblr = bool("BBLR", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);

    public int kills, finals, beds, deaths, wins, losses, finalDeaths, bedsLost;
    /** Everything here counts from when the game was started and is only cleared by closing the game - never by changing lobby, queue or world. */
    private static final long SESSION_START = System.currentTimeMillis();
    private long lastResult;
    private final List<String> lines = new ArrayList<>();

    public SessionStats() { super("Session Stats", "Kills, finals, beds, wins and game time this session.", Category.HYPIXEL, END, 4, CENTER, 42); }

    /** Works out what a system chat line means for the player called {@code me}. 0 none, 1 kill, 2 final kill, 3 bed broken, 4 death, 5 own bed lost, 6 final death. */
    public static int classify(String msg, String me) {
        if (msg.startsWith("BED DESTRUCTION") && msg.contains("Your Bed")) return 5; // this line does not name you
        if (me == null || me.isEmpty() || !msg.contains(me)) return 0;
        if (msg.startsWith(" ")) return 0;
        int colon = msg.indexOf(": ");
        if (colon > 0 && colon < 60 && !msg.startsWith("BED DESTRUCTION")) return 0; // player chat
        if (msg.startsWith("BED DESTRUCTION")) return msg.contains("Your Bed") ? 5 : msg.indexOf(me) > 0 ? 3 : 0;
        boolean fin = msg.endsWith("FINAL KILL!");
        String body = fin ? msg.substring(0, msg.length() - 11).trim() : msg;
        if (!body.endsWith(".")) return 0;
        if (body.contains(" joined") || body.contains(" quit") || body.contains("connected") || body.contains(" purchased ")) return 0;
        if (body.startsWith(me + " ")) return fin ? 6 : 4;
        return body.endsWith(" " + me + ".") || body.contains(" by " + me) || body.contains(me + "'s ") ? (fin ? 2 : 1) : 0;
    }

    public boolean onChat(String raw) {
        if (!Servers.onHypixel()) return false;
        switch (classify(Servers.strip(raw), mc.getUser().getName())) {
            case 2: finals++; // a final kill is also a kill
            case 1: kills++; break;
            case 3: beds++; break;
            case 6: finalDeaths++; // a final death is also a death
            case 4: deaths++; break;
            case 5: bedsLost++; break;
            default:
        }
        return false;
    }

    /** Called for every title the server shows. */
    public void onTitle(String title) {
        if (title == null || !Servers.onHypixel()) return;
        String t = Servers.strip(title).trim();
        long now = System.currentTimeMillis();
        if (now - lastResult < 15000) return;
        if (t.equals("VICTORY!")) { wins++; lastResult = now; }
        else if (t.equals("GAME OVER!") || t.equals("DEFEAT!") || t.equals("GAME END")) { losses++; lastResult = now; }
    }

    /** Ratio for this session; with nothing lost yet it is simply the count, as stat sites show it. */
    static String ratio(int won, int lost) { return String.format(java.util.Locale.ROOT, "%.2f", lost == 0 ? (float) won : won / (float) lost); }

    private long builtAt;

    /** The text only changes once a second at most, so it is not rebuilt every frame. */
    private void build() {
        long nowMs = System.currentTimeMillis();
        if (nowMs - builtAt < 250 && !lines.isEmpty()) return;
        builtAt = nowMs;
        lines.clear();
        if (showTime.on()) { long s = (System.currentTimeMillis() - SESSION_START) / 1000; lines.add(s >= 3600 ? String.format("Time: %d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("Time: %d:%02d", s / 60, s % 60)); }
        if (showKills.on()) lines.add("Kills: " + kills);
        if (showFinals.on()) lines.add("Finals: " + finals);
        if (showBeds.on()) lines.add("Beds: " + beds);
        if (showDeaths.on()) lines.add("Deaths: " + deaths);
        if (showWins.on()) lines.add("W/L: " + wins + " / " + losses);
        if (showFkdr.on()) lines.add("FKDR: " + ratio(finals, finalDeaths));
        if (showBblr.on()) lines.add("BBLR: " + ratio(beds, bedsLost));
    }

    public float width() {
        float w = 40;
        for (String s : lines) w = Math.max(w, mc.font.width(s));
        return w + 8;
    }
    public float height() { return Math.max(1, lines.size()) * 10 + 6; }

    public void draw(boolean editing) {
        build();
        if (lines.isEmpty()) return;
        if (background.on()) Draw.roundRect(0, 0, width(), height(), 3, 0x70000000);
        for (int i = 0; i < lines.size(); i++) Draw.text(lines.get(i), 4, 4 + i * 10, color.get(), true);
    }
}
