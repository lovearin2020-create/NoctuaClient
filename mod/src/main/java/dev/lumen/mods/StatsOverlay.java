package dev.lumen.mods;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.lumen.core.Category;
import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.stats.StatsService;
import dev.lumen.stats.StatsService.Entry;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Bed Wars lobby overlay: stars, FKDR, win/loss, bed ratio and winstreak of everyone in your game, Urchin cheater and
 * sniper tags, who is nicked (and who they are when they kept their own skin), and a warning for dangerous lobbies.
 */
public class StatsOverlay extends HudModule {
    private static final int LEFT = 180, ROW = 10;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Set<String> NICK_SKINS = new HashSet<String>();

    private final Setting.Text urchinKey = text("Urchin API Key", "", 80);
    private final Setting.Text hypixelKey = text("Hypixel API Key", "", 80);
    private final Setting.Mode show = mode("Show", "Always", "Always", "Tab Held", "Before Game Only");
    private final Setting.Mode sortBy = mode("Sort By", "Threat", "Threat", "Stars", "FKDR", "WLR", "BBLR", "Winstreak", "Finals", "Monthly FKDR", "Name");
    private final Setting.Bool onlyBedwars = bool("Only In Bed Wars", true);
    private final Setting.Bool denick = bool("Denick By Skin", true);
    private final Setting.Bool warn = bool("Threat Warnings", true);
    private final Setting.Num warnFkdr = num("Warn At FKDR", 5, 1, 30, 0.5);
    private final Setting.Num maxRows = num("Max Rows", 16, 4, 24, 1);
    private final Setting.Bool cFkdr = bool("Column: FKDR", true), cWlr = bool("Column: WLR", true), cBblr = bool("Column: BBLR", true),
            cWs = bool("Column: Winstreak", true), cMfkdr = bool("Column: Monthly FKDR", true), cMbblr = bool("Column: Monthly BBLR", false),
            cFinals = bool("Column: Finals", true), cBeds = bool("Column: Beds Broken", false), cWins = bool("Column: Wins", false), cFpl = bool("Column: Finals / Level", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col accent = col("Accent Color", 0xFF4C8DFF);

    private static final class Row { Entry e; String name; int color; }
    private volatile List<Row> rows = new ArrayList<Row>();
    private final Set<UUID> warned = new HashSet<UUID>();
    private boolean bedwars, pregame, noSource;
    private int ticks;

    public StatsOverlay() { super("Stats Overlay", "Bed Wars: stats and tags of the players in your game.", Category.HYPIXEL, START, 4, CENTER, -50); }

    private static final String[] LABELS = {"FKDR", "WLR", "BBLR", "WS", "MFKDR", "MBBLR", "FINALS", "BEDS", "WINS", "F/LVL"};
    private static final int[] WIDTHS = {28, 28, 28, 24, 32, 32, 36, 32, 32, 30};
    private Setting.Bool[] columns() { return new Setting.Bool[]{cFkdr, cWlr, cBblr, cWs, cMfkdr, cMbblr, cFinals, cBeds, cWins, cFpl}; }

    protected boolean hasLumenStyle() { return false; }
    public float width() { float w = LEFT; Setting.Bool[] c = columns(); for (int i = 0; i < c.length; i++) if (c[i].on()) w += WIDTHS[i]; return w + 6; }
    public float height() { return 18 + Math.max(1, rows.size()) * ROW + (noSource ? 11 : 3); }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { warned.clear(); rows = new ArrayList<Row>(); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) return;
        if (++ticks % 10 != 0) return;
        if (!Util.onHypixel()) { rows = new ArrayList<Row>(); return; }
        readSidebar();
        if (onlyBedwars.on() && !bedwars) { rows = new ArrayList<Row>(); return; }

        List<Row> out = new ArrayList<Row>();
        boolean none = false;
        for (NetworkPlayerInfo info : new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap())) {
            GameProfile gp = info.getGameProfile();
            if (gp == null || gp.getId() == null || gp.getName() == null || !NAME.matcher(gp.getName()).matches()) continue;
            int v = gp.getId().version();
            if (v != 4 && v != 1) continue; // the lobby's NPCs
            if (out.size() >= 40) break;
            Row r = new Row();
            r.name = gp.getName();
            r.color = teamColor(info);
            r.e = StatsService.get(gp.getId(), gp.getName(), denick.on() && v == 1 ? skinOwner(gp) : null, hypixelKey.value, urchinKey.value);
            if (r.e.state == StatsService.NO_SOURCE) none = true;
            out.add(r);
            if (warn.on() && (pregame || !onlyBedwars.on())) warn(r);
        }
        Collections.sort(out, new Comparator<Row>() { public int compare(Row a, Row b) {
            if (sortBy.is("Name")) return a.name.compareToIgnoreCase(b.name);
            int c = Double.compare(key(b.e), key(a.e)); // highest first; players without that number yet go last
            return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
        } });
        while (out.size() > maxRows.i()) out.remove(out.size() - 1);
        noSource = none;
        rows = out;
    }

    /** The number the list is ordered by. */
    private double key(Entry e) {
        if (sortBy.is("Threat")) return e.threat();
        if (sortBy.is("Monthly FKDR")) return e.mfkdr;
        if (e.state != StatsService.OK) return -1;
        if (sortBy.is("Stars")) return e.star;
        if (sortBy.is("FKDR")) return e.fkdr;
        if (sortBy.is("WLR")) return e.wlr;
        if (sortBy.is("BBLR")) return e.bblr;
        if (sortBy.is("Winstreak")) return e.winstreak;
        return e.finals;
    }

    private void readSidebar() {
        bedwars = false; pregame = false;
        try {
            Scoreboard sb = mc.theWorld.getScoreboard();
            ScoreObjective o = sb.getObjectiveInDisplaySlot(1);
            if (o == null) return;
            bedwars = Util.strip(o.getDisplayName()).toUpperCase().contains("BED WARS");
            for (Score s : sb.getSortedScores(o)) {
                String line = Util.strip(ScorePlayerTeam.formatPlayerName(sb.getPlayersTeam(s.getPlayerName()), s.getPlayerName()));
                if (line.contains("Waiting") || line.contains("Starting in")) pregame = true;
            }
        } catch (Throwable ignored) {}
    }

    private void warn(Row r) {
        Entry e = r.e;
        if (e.state == StatsService.PENDING || e.id.equals(mc.thePlayer.getUniqueID()) || warned.contains(e.id)) return;
        String why = null;
        if (StatsService.dangerous(e.tag)) why = "§c" + e.tag.replace('_', ' ') + (e.tagReason.isEmpty() ? "" : " §7(" + e.tagReason + ")");
        else if (e.state == StatsService.OK && e.fkdr >= warnFkdr.f()) why = "§e" + e.star + " stars§7, FKDR §c" + String.format("%.2f", e.fkdr);
        else if (e.state == StatsService.NICKED) why = "§dnicked";
        if (why == null) { if (e.state == StatsService.OK) warned.add(e.id); return; }
        warned.add(e.id);
        String who = r.name + (e.realName != null ? " §7(" + e.realName + ")" : "");
        mc.thePlayer.addChatMessage(new ChatComponentText("§8[§bNoctua§8] §cThreat: §f" + who + " §8- " + why));
        mc.thePlayer.playSound("note.pling", 0.7f, 1.6f);
    }

    private int teamColor(NetworkPlayerInfo info) {
        try {
            ScorePlayerTeam t = info.getPlayerTeam();
            String p = t == null ? "" : t.getColorPrefix();
            for (int i = p.length() - 2; i >= 0; i--) {
                if (p.charAt(i) != '§') continue;
                int idx = "0123456789abcdef".indexOf(Character.toLowerCase(p.charAt(i + 1)));
                if (idx >= 0) return 0xFF000000 | mc.fontRendererObj.getColorCode(p.charAt(i + 1));
            }
        } catch (Throwable ignored) {}
        return 0xFFFFFFFF;
    }

    /** The account a skin belongs to. Hypixel's stock nick skins are nobody's. */
    private static String skinOwner(GameProfile gp) {
        try {
            for (Property p : gp.getProperties().get("textures")) {
                String json = new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(p.getValue()), "UTF-8");
                JsonObject root = new JsonParser().parse(json).getAsJsonObject();
                String url = root.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                String hash = url.substring(url.lastIndexOf('/') + 1);
                if (nickSkins().contains(hash)) return null;
                String owner = root.get("profileName").getAsString();
                return NAME.matcher(owner).matches() ? owner : null;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Set<String> nickSkins() {
        if (NICK_SKINS.isEmpty()) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(StatsOverlay.class.getResourceAsStream("/assets/lumen/nick_skins.txt"), "UTF-8"));
                for (String l; (l = in.readLine()) != null; ) if (!l.trim().isEmpty()) NICK_SKINS.add(l.trim());
                in.close();
            } catch (Throwable ignored) {}
            NICK_SKINS.add("-");
        }
        return NICK_SKINS;
    }

    protected boolean showing(boolean editing) {
        if (editing) return true;
        if (rows.isEmpty()) return false;
        if (show.is("Tab Held")) return mc.gameSettings.keyBindPlayerList.isKeyDown();
        if (show.is("Before Game Only")) return pregame || !bedwars;
        return true;
    }

    private static final int[] PRESTIGE = {0xFFAAAAAA, 0xFFFFFFFF, 0xFFFFAA00, 0xFF55FFFF, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFFF55FF, 0xFF5555FF, 0xFFAA00AA};
    private static int starColor(int star) { return star >= 1000 ? 0xFFFFE066 : PRESTIGE[Math.max(0, star / 100)]; }
    private static int ratioColor(double v, double scale) {
        v /= scale;
        return v < 1 ? 0xFFAAB2C5 : v < 2 ? 0xFFFFFFFF : v < 4 ? 0xFFFFD75E : v < 7 ? 0xFFFF9F43 : v < 10 ? 0xFFFF5A5A : 0xFFD46BFF;
    }
    private static void right(String s, float x, float y, int color) { Fnt.draw(s, x - Fnt.width(s, 7), y, 7, color); }

    private static String tagLabel(String tag) {
        String t = tag.toLowerCase();
        return t.contains("sniper") ? "SNIPER" : t.contains("cheater") ? "CHEAT" : t.equals("caution") ? "CAUTION" : t.equals("account") ? "ACCOUNT" : "INFO";
    }
    private static int tagColor(String tag) {
        String t = tag.toLowerCase();
        return t.contains("blatant") || t.contains("confirmed") || t.equals("sniper") ? 0xFFE5484D : t.contains("sniper") || t.contains("cheater") ? 0xFFF08A3C : t.equals("caution") ? 0xFFD9A21B : 0xFF5B6B8C;
    }

    public void draw(boolean editing) {
        List<Row> list = rows;
        if (editing && list.isEmpty()) list = sample();
        if (!showing(editing) || list.isEmpty()) return;
        float h = 18 + list.size() * ROW + (noSource ? 11 : 3), W = width();
        int ac = accent.get();
        if (background.on()) {
            R.roundRect(0, 0, W, h, 6, 0x78000000);
            R.gradient(4, 1, W - 8, 9, 0x14FFFFFF, 0x00FFFFFF);
        }
        int head = 0xFF8891A5, dim = 0xFF6F778A;
        Fnt.draw("TAG", 7, 4, 6, head);
        Fnt.draw("STAR", 40, 4, 6, head);
        Fnt.draw("PLAYER", 66, 4, 6, head);
        Setting.Bool[] cols = columns();
        float[] cx = new float[cols.length];
        float x = LEFT;
        for (int i = 0; i < cols.length; i++) {
            if (!cols[i].on()) continue;
            x += WIDTHS[i]; cx[i] = x;
            Fnt.draw(LABELS[i], x - Fnt.width(LABELS[i], 6), 4, 6, head);
        }
        R.rect(6, 14, W - 12, 1, R.alpha(ac, 0.75f));

        float y = 17;
        for (Row r : list) {
            Entry e = r.e;
            boolean nick = e.id.version() != 4 || e.state == StatsService.NICKED;
            String tag = !e.tag.isEmpty() ? tagLabel(e.tag) : nick ? "NICK" : null;
            if (tag != null) {
                R.roundRect(5, y, 31, 8, 4, !e.tag.isEmpty() ? tagColor(e.tag) : 0xFF8E5BD9);
                Fnt.centerB(tag, 20.5f, y + 0.6f, 5, 0xFFFFFFFF);
            }
            String name = r.name + (e.realName != null ? " (" + e.realName + ")" : "");
            Fnt.draw(Fnt.fit(name, 7, 108), 66, y + 0.6f, 7, r.color);
            boolean ok = e.state == StatsService.OK;
            if (ok) right(String.valueOf(e.star), 58, y + 0.6f, starColor(e.star));
            String wait = e.state == StatsService.PENDING ? "..." : "-";
            for (int i = 0; i < cols.length; i++) {
                if (!cols[i].on()) continue;
                String v = wait; int c = dim;
                if (i == 4 || i == 5) { // Urchin's, with or without Hypixel's
                    double m = i == 4 ? e.mfkdr : e.mbblr;
                    if (m >= 0) { v = String.format("%.2f", m); c = ratioColor(m, i == 4 ? 1 : 0.7); } else if (ok) v = "-";
                } else if (ok) {
                    switch (i) {
                        case 0: v = String.format("%.2f", e.fkdr); c = ratioColor(e.fkdr, 1); break;
                        case 1: v = String.format("%.2f", e.wlr); c = ratioColor(e.wlr, 0.5); break;
                        case 2: v = String.format("%.2f", e.bblr); c = ratioColor(e.bblr, 0.7); break;
                        case 3: v = e.winstreak < 0 ? "?" : (e.winstreakGuess ? "~" : "") + e.winstreak; c = e.winstreak >= 10 ? 0xFFFF5A5A : e.winstreak >= 4 ? 0xFFFFD75E : 0xFFAAB2C5; break;
                        case 6: v = count(e.finals); c = 0xFFDDE3F0; break;
                        case 7: v = count(e.beds); c = 0xFFDDE3F0; break;
                        case 8: v = count(e.wins); c = 0xFFDDE3F0; break;
                        default: double f = e.finals / (double) Math.max(1, e.star); v = String.format("%.1f", f); c = ratioColor(f, 5);
                    }
                }
                right(v, cx[i], y + 0.6f, c);
            }
            y += ROW;
        }
        if (noSource) Fnt.draw("No stats source: set a Hypixel API key in this module's settings.", 7, y + 2, 6, 0xFFFFB454);
    }

    private static String count(int n) { return n >= 100000 ? String.format("%.0fk", n / 1000.0) : n >= 10000 ? String.format("%.1fk", n / 1000.0) : String.valueOf(n); }

    private List<Row> sample() {
        List<Row> out = new ArrayList<Row>();
        Object[][] d = {{"Technoblade", 0xFFFF5555, 1247, 14.82, 9.1, 6.4, 37, "sniper"}, {"xNestorio", 0xFF5555FF, 612, 6.31, 3.2, 2.9, 8, ""},
                {"Noctua_User", 0xFF55FF55, 284, 2.47, 1.3, 1.6, 2, ""}, {"Steve", 0xFFFFFF55, 37, 0.62, 0.4, 0.5, 0, ""}};
        for (Object[] o : d) {
            Row r = new Row(); r.name = (String) o[0]; r.color = (Integer) o[1];
            Entry e = StatsService.sample(r.name, (Integer) o[2], (Double) o[3], (Double) o[4], (Double) o[5], (Integer) o[6], (String) o[7]);
            r.e = e; out.add(r);
        }
        return out;
    }
}
