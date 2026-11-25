package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/** Counts what you did since the game was started, read from Hypixel's own chat lines and titles. */
public class SessionStats extends HudModule {
    public static SessionStats INSTANCE;
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
    private final List<String> lines = new ArrayList<String>();

    public SessionStats() { super("Session Stats", "Kills, finals, beds, wins and game time this session.", dev.lumen.core.Category.HYPIXEL, END, 4, CENTER, 42); INSTANCE = this; }

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

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || mc.thePlayer == null || !Util.onHypixel()) return;
        switch (classify(Util.strip(e.message.getUnformattedText()), mc.thePlayer.getName())) {
            case 2: finals++; // a final kill is also a kill
            case 1: kills++; break;
            case 3: beds++; break;
            case 6: finalDeaths++; // a final death is also a death
            case 4: deaths++; break;
            case 5: bedsLost++; break;
            default:
        }
    }

    /** Called for every title the server shows. */
    public void onTitle(String title) {
        if (title == null || !Util.onHypixel()) return;
        String t = Util.strip(title).trim();
        long now = System.currentTimeMillis();
        if (now - lastResult < 15000) return;
        if (t.equals("VICTORY!")) { wins++; lastResult = now; }
        else if (t.equals("GAME OVER!") || t.equals("DEFEAT!") || t.equals("GAME END") ) { losses++; lastResult = now; }
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
        if (lumen()) return 132;
        float w = 40;
        for (String s : lines) w = Math.max(w, mc.fontRendererObj.getStringWidth(s));
        return w + 8;
    }
    public float height() { return lumen() ? lumenHeight() : Math.max(1, lines.size()) * 10 + 6; }

    public void draw(boolean editing) {
        build();
        if (lines.isEmpty()) return;
        if (lumen()) { drawLumen(); return; }
        if (background.on()) R.roundRect(0, 0, width(), height(), 3, 0x70000000);
        for (int i = 0; i < lines.size(); i++) R.mcText(lines.get(i), 4, 4 + i * 10, color.get(), true);
    }

    private boolean big() { return showFinals.on() || showBeds.on() || showWins.on(); }
    private int small() { return (showKills.on() ? 1 : 0) + (showDeaths.on() ? 1 : 0) + (showFkdr.on() ? 1 : 0) + (showBblr.on() ? 1 : 0); }
    private float lumenHeight() { return 19 + (big() ? 27 : 0) + (small() > 0 ? 4 + small() * 10 : 0) + 3; }

    private void drawLumen() {
        float w = 132;
        if (background.on()) panel(0, 0, w, lumenHeight());
        int dim = 0xFF8891A5;
        dev.lumen.util.Fnt.draw("BED WARS SESSION", 7, 5, 6, dev.lumen.util.Fnt.REGULAR, 0.5f, dim, dim);
        if (showTime.on()) {
            long s = (System.currentTimeMillis() - SESSION_START) / 1000;
            String t = s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
            dev.lumen.util.Fnt.draw(t, w - 7 - dev.lumen.util.Fnt.width(t, 6), 5, 6, dim);
        }
        R.rect(6, 14, w - 12, 1, R.alpha(R.ACCENT, 0.75f));
        float y = 18;
        if (big()) {
            List<String[]> cells = new ArrayList<String[]>();
            if (showFinals.on()) cells.add(new String[]{String.valueOf(finals), "FINALS"});
            if (showBeds.on()) cells.add(new String[]{String.valueOf(beds), "BEDS"});
            if (showWins.on()) cells.add(new String[]{wins + " / " + losses, "W / L"});
            float cw = (w - 12) / cells.size();
            for (int i = 0; i < cells.size(); i++) {
                float cx = 6 + cw * i + cw / 2f;
                dev.lumen.util.Fnt.centerB(cells.get(i)[0], cx, y, 12, color.get());
                dev.lumen.util.Fnt.center(cells.get(i)[1], cx, y + 15, 5, dim);
            }
            y += 27;
        }
        if (small() > 0) {
            if (big()) R.rect(6, y - 1, w - 12, 1, R.alpha(R.ACCENT, 0.3f));
            y += 3;
            String[][] rows = {showKills.on() ? new String[]{"Kills", String.valueOf(kills)} : null, showDeaths.on() ? new String[]{"Deaths", String.valueOf(deaths)} : null,
                    showFkdr.on() ? new String[]{"FKDR", ratio(finals, finalDeaths)} : null, showBblr.on() ? new String[]{"BBLR", ratio(beds, bedsLost)} : null};
            for (String[] r : rows) {
                if (r == null) continue;
                dev.lumen.util.Fnt.draw(r[0], 7, y, 7, dim);
                dev.lumen.util.Fnt.drawB(r[1], w - 7 - dev.lumen.util.Fnt.widthB(r[1], 7), y, 7, color.get());
                y += 10;
            }
        }
    }
}
