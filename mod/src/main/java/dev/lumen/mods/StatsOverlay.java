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
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lobby overlay for Bed Wars, Duels and SkyWars (it follows the game you are in). Bed Wars: stars, FKDR, win/loss, bed ratio and winstreak of everyone in your game, Urchin cheater and
 * sniper tags, who is nicked (and who they are when they kept their own skin), and a warning for dangerous lobbies.
 * Besides the players in the tab list it shows your party under their real names, anyone who says your name in chat
 * for a while, and whoever you look up with /nstats. In a game a player stays listed until their final death.
 */
public class StatsOverlay extends HudModule {
    private static final int LEFT = 180, ROW = 10;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Set<String> NICK_SKINS = new HashSet<String>();

    private final Setting.Text urchinKey = text("Urchin API Key", "", 80);
    private final Setting.Text hypixelKey = text("Hypixel API Key", "", 80);
    private final Setting.Mode show = mode("Show", "Always", "Always", "Tab Held", "Before Game Only");
    private final Setting.Mode sortBy = mode("Sort By", "Threat", "Threat", "Stars", "FKDR", "WLR", "BBLR", "Winstreak", "Finals", "Monthly FKDR", "Name");
    private final Setting.Bool onlyBedwars = bool("Only In Games", true);
    private final Setting.Bool inDuels = bool("Show In Duels", true), inSkywars = bool("Show In SkyWars", true);
    private final Setting.Bool denick = bool("Denick By Skin", true);
    private final Setting.Bool warn = bool("Threat Warnings", true);
    private final Setting.Num warnFkdr = num("Warn At FKDR", 5, 1, 30, 0.5);
    private final Setting.Num maxRows = num("Max Rows", 16, 4, 24, 1);
    private final Setting.Bool keep = bool("Keep Until Final Death", true);
    private final Setting.Bool showParty = bool("Show Party", true);
    private final Setting.Bool showMentions = bool("Show Mentions", true);
    private final Setting.Num mentionTime = num("Mention Time (s)", 60, 10, 300, 10);
    private final Setting.Bool cFkdr = bool("Column: FKDR", true), cWlr = bool("Column: WLR", true), cBblr = bool("Column: BBLR", true),
            cWs = bool("Column: Winstreak", true), cMfkdr = bool("Column: Monthly FKDR", true), cMbblr = bool("Column: Monthly BBLR", false),
            cFinals = bool("Column: Finals", true), cBeds = bool("Column: Beds Broken", false), cWins = bool("Column: Wins", false), cFpl = bool("Column: Finals / Level", true);
    private final Setting.Bool dWlr = bool("Duels: WLR", true), dKdr = bool("Duels: KDR", true), dWs = bool("Duels: Winstreak", true), dBest = bool("Duels: Best WS", true),
            dWins = bool("Duels: Wins", true), dLosses = bool("Duels: Losses", false), dKills = bool("Duels: Kills", false);
    private final Setting.Bool sKdr = bool("SkyWars: KDR", true), sWlr = bool("SkyWars: WLR", true), sWins = bool("SkyWars: Wins", true), sKills = bool("SkyWars: Kills", true),
            sDeaths = bool("SkyWars: Deaths", false), sLosses = bool("SkyWars: Losses", false);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col accent = col("Accent Color", 0xFF4C8DFF);

    private static final class Row { Entry e; String name; int color; String mark; boolean named; }
    /** A player of this game, remembered while the server takes them off the tab list between deaths. */
    private static final class Seen { UUID id; String name, skin; int color; }
    private final Map<UUID, Seen> roster = new LinkedHashMap<UUID, Seen>();
    private final Set<String> finalDead = new HashSet<String>();
    /** Lower-case name to the name as written, for the party, the looked-up players and (with an expiry) mentions. */
    private final Map<String, String> party = new LinkedHashMap<String, String>(), pins = new LinkedHashMap<String, String>(), mentionNames = new LinkedHashMap<String, String>();
    private final Map<String, Long> mentionUntil = new LinkedHashMap<String, Long>();
    private static StatsOverlay instance;
    private volatile List<Row> rows = new ArrayList<Row>();
    private final Set<UUID> warned = new HashSet<UUID>();
    private boolean bedwars, pregame, noSource;
    private int other;
    private int ticks;

    public StatsOverlay() { super("Stats Overlay", "Bed Wars: stats and tags of the players in your game.", Category.HYPIXEL, START, 4, CENTER, -50); instance = this; }

    private static final String[] LABELS = {"FKDR", "WLR", "BBLR", "WS", "MFKDR", "MBBLR", "FINALS", "BEDS", "WINS", "F/LVL"};
    private static final int[] WIDTHS = {28, 28, 28, 24, 32, 32, 36, 32, 32, 30};
    private static final String[] D_LABELS = {"WLR", "KDR", "WS", "BEST", "WINS", "LOSSES", "KILLS"}, S_LABELS = {"KDR", "WLR", "WINS", "KILLS", "DEATHS", "LOSSES"};
    private static final int[] D_WIDTHS = {28, 28, 24, 28, 34, 36, 34}, S_WIDTHS = {28, 28, 32, 34, 36, 36};
    /** Which game the list is for: 0 Bed Wars, 1 Duels, 2 SkyWars. */
    private volatile int game;
    private Setting.Bool[] columns() {
        return game == 1 ? new Setting.Bool[]{dWlr, dKdr, dWs, dBest, dWins, dLosses, dKills} : game == 2 ? new Setting.Bool[]{sKdr, sWlr, sWins, sKills, sDeaths, sLosses}
                : new Setting.Bool[]{cFkdr, cWlr, cBblr, cWs, cMfkdr, cMbblr, cFinals, cBeds, cWins, cFpl};
    }
    private String[] labels() { return game == 1 ? D_LABELS : game == 2 ? S_LABELS : LABELS; }
    private int[] widths() { return game == 1 ? D_WIDTHS : game == 2 ? S_WIDTHS : WIDTHS; }

    protected boolean hasLumenStyle() { return false; }
    public float width() { float w = LEFT; Setting.Bool[] c = columns(); int[] ws = widths(); for (int i = 0; i < c.length; i++) if (c[i].on()) w += ws[i]; return w + 6; }
    public float height() { return 18 + Math.max(1, rows.size()) * ROW + (noSource ? 11 : 3); }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { warned.clear(); rows = new ArrayList<Row>(); roster.clear(); finalDead.clear(); }

    private static final String RANK = "(?:\\[[^\\]]+\\] )?";
    private static final Pattern JOINED = Pattern.compile("^" + RANK + "(\\w{1,16}) joined the party\\.$"), JOINED_THEIRS = Pattern.compile("^You have joined " + RANK + "(\\w{1,16})'s party!$"),
            GONE = Pattern.compile("^" + RANK + "(\\w{1,16}) (?:has left the party|has been removed from the party|was removed from the party).*$"),
            KICKED = Pattern.compile("^Kicked " + RANK + "(\\w{1,16}) because.*$"), ELIMINATED = Pattern.compile("^TEAM ELIMINATED > (\\w+) Team has been eliminated!?$");

    /** The player names in a piece of text such as "[MVP+] Alice ● [VIP] Bob ●". */
    private static List<String> names(String s) {
        List<String> out = new ArrayList<String>();
        for (String t : s.replaceAll("\\[[^\\]]*\\]", " ").split("[\\s,]+")) if (NAME.matcher(t).matches()) out.add(t);
        return out;
    }

    private void partyAdd(String name) { if (mc.thePlayer == null || !name.equalsIgnoreCase(mc.thePlayer.getName())) party.put(name.toLowerCase(), name); }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Util.onHypixel() || mc.thePlayer == null) return;
        String msg = Util.strip(e.message.getUnformattedText()).trim();
        String me = mc.thePlayer.getName();
        // the /pl list, whose lines look like chat but come from the server
        if (msg.startsWith("Party Members (")) { party.clear(); return; }
        for (String head : new String[]{"Party Leader: ", "Party Moderators: ", "Party Members: ", "You'll be partying with: "}) {
            if (msg.startsWith(head)) { for (String n : names(msg.substring(head.length()))) partyAdd(n); return; }
        }
        int colon = msg.indexOf(": ");
        if (colon > 0 && !msg.startsWith(" ")) { // something a player typed
            String head = msg.substring(0, colon), body = msg.substring(colon + 2);
            List<String> who = names(head.replace("Party >", " ").replace("From ", " "));
            if (who.isEmpty()) return;
            String sender = who.get(who.size() - 1);
            if (sender.equalsIgnoreCase(me)) return;
            if (head.startsWith("Party >")) partyAdd(sender);
            boolean named = Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(me) + "(?![A-Za-z0-9_])").matcher(body).find();
            if (named || head.startsWith("From ")) {
                mentionNames.put(sender.toLowerCase(), sender);
                mentionUntil.put(sender.toLowerCase(), System.currentTimeMillis() + (long) (mentionTime.f() * 1000));
            }
            return;
        }
        if (msg.endsWith("FINAL KILL!")) {
            int sp = msg.indexOf(' ');
            if (sp > 0 && NAME.matcher(msg.substring(0, sp)).matches()) finalDead.add(msg.substring(0, sp).toLowerCase());
            return;
        }
        Matcher m;
        if ((m = ELIMINATED.matcher(msg)).matches()) {
            int t = java.util.Arrays.asList(dev.lumen.util.Bw.NAMES).indexOf(m.group(1));
            if (t >= 0) {
                int c = 0xFF000000 | mc.fontRendererObj.getColorCode(dev.lumen.util.Bw.CODES.charAt(t));
                for (Seen s : roster.values()) if (s.color == c) finalDead.add(s.name.toLowerCase());
            }
        } else if ((m = JOINED.matcher(msg)).matches()) partyAdd(m.group(1));
        else if ((m = JOINED_THEIRS.matcher(msg)).matches()) { party.clear(); partyAdd(m.group(1)); }
        else if ((m = GONE.matcher(msg)).matches() || (m = KICKED.matcher(msg)).matches()) party.remove(m.group(1).toLowerCase());
        else if (msg.startsWith("You left the party") || msg.startsWith("The party was disbanded") || msg.contains("has disbanded the party")
                || msg.startsWith("You have been kicked from the party") || msg.startsWith("You are not currently in a party") || msg.startsWith("You are not in a party")) party.clear();
    }

    /** Stats of a player in the tab list for other features, with the keys set here; null when there is nothing to ask with. */
    public static Entry lookup(UUID id, String name) {
        StatsOverlay o = instance;
        if (o == null || id == null || id.version() != 4) return null;
        return StatsService.get(id, name, null, o.hypixelKey.value, o.urchinKey.value);
    }

    /** /nstats: look players up by name and keep them on the overlay. */
    public static final class Command extends net.minecraft.command.CommandBase {
        public String getCommandName() { return "nstats"; }
        public String getCommandUsage(net.minecraft.command.ICommandSender s) { return "/nstats <player...> | remove <player> | clear"; }
        public int getRequiredPermissionLevel() { return 0; }
        public boolean canCommandSenderUseCommand(net.minecraft.command.ICommandSender s) { return true; }
        public void processCommand(net.minecraft.command.ICommandSender s, String[] a) {
            StatsOverlay o = instance;
            String say;
            if (o == null || !o.isEnabled()) say = "\u00a7cTurn Stats Overlay on first (Right Shift > Hypixel).";
            else if (a.length == 0) say = "\u00a77" + getCommandUsage(s) + (o.pins.isEmpty() ? "" : "  \u00a78|\u00a77  shown: \u00a7f" + o.pins.values());
            else if (a[0].equalsIgnoreCase("clear")) { o.pins.clear(); say = "\u00a77Looked-up players cleared."; }
            else if (a[0].equalsIgnoreCase("remove") && a.length > 1) { for (int i = 1; i < a.length; i++) o.pins.remove(a[i].toLowerCase()); say = "\u00a77Removed."; }
            else {
                List<String> added = new ArrayList<String>();
                for (String n : a) if (NAME.matcher(n).matches() && o.pins.size() < 12) { o.pins.put(n.toLowerCase(), n); added.add(n); }
                say = added.isEmpty() ? "\u00a7cThat is not a player name." : "\u00a77Showing \u00a7f" + added + "\u00a77 on the overlay. \u00a78/nstats clear\u00a77 removes them.";
            }
            s.addChatMessage(new ChatComponentText("\u00a78[\u00a7bNoctua\u00a78] " + say));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) return;
        if (++ticks % 10 != 0) return;
        boolean hypixel = Util.onHypixel();
        if (hypixel) readSidebar(); else { bedwars = false; pregame = false; other = 0; }
        game = bedwars ? 0 : other;
        final List<Row> out = new ArrayList<Row>();
        boolean none = false;
        if (hypixel && (bedwars || other != 0 || !onlyBedwars.on())) {
            boolean playing = keep.on() && bedwars && !pregame;
            if (!playing) roster.clear();
            List<Seen> seen = new ArrayList<Seen>();
            for (NetworkPlayerInfo info : new ArrayList<NetworkPlayerInfo>(mc.getNetHandler().getPlayerInfoMap())) {
                GameProfile gp = info.getGameProfile();
                if (gp == null || gp.getId() == null || gp.getName() == null || !NAME.matcher(gp.getName()).matches()) continue;
                int v = gp.getId().version();
                if (v != 4 && v != 1) continue; // the lobby's NPCs
                Seen s = playing ? roster.get(gp.getId()) : null;
                if (s == null) { s = new Seen(); s.id = gp.getId(); if (playing && roster.size() < 40) roster.put(s.id, s); }
                s.name = gp.getName(); s.color = teamColor(info);
                s.skin = denick.on() && v == 1 ? skinOwner(gp) : null;
                seen.add(s);
            }
            for (Seen s : playing ? new ArrayList<Seen>(roster.values()) : seen) {
                if (playing && finalDead.contains(s.name.toLowerCase())) continue;
                if (out.size() >= 40) break;
                Row r = new Row();
                r.name = s.name; r.color = s.color;
                r.e = StatsService.get(s.id, s.name, s.skin, hypixelKey.value, urchinKey.value);
                if (r.e.state == StatsService.NO_SOURCE) none = true;
                out.add(r);
                if (warn.on() && bedwars && pregame) warn(r);
            }
        }
        Collections.sort(out, new Comparator<Row>() { public int compare(Row a, Row b) {
            if (sortBy.is("Name")) return a.name.compareToIgnoreCase(b.name);
            int c = Double.compare(key(b.e), key(a.e)); // highest first; players without that number yet go last
            return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
        } });
        while (out.size() > maxRows.i()) out.remove(out.size() - 1);
        // the players you asked for, on top: looked up, then mentions, then your party
        List<Row> top = new ArrayList<Row>();
        extras(pins, "PIN", out, top);
        if (hypixel) {
            long now = System.currentTimeMillis();
            for (Iterator<Map.Entry<String, Long>> it = mentionUntil.entrySet().iterator(); it.hasNext(); ) { Map.Entry<String, Long> m = it.next(); if (m.getValue() < now) { mentionNames.remove(m.getKey()); it.remove(); } }
            if (showMentions.on()) extras(mentionNames, "MENTION", out, top);
            if (showParty.on()) extras(party, "PARTY", out, top);
        }
        for (Row r : top) if (r.e.state == StatsService.NO_SOURCE) none = true;
        top.addAll(out);
        noSource = none;
        rows = top;
    }

    /** Marks the listed players who are already on the overlay and adds the others by name. */
    private void extras(Map<String, String> names, String mark, List<Row> lobby, List<Row> top) {
        for (String name : new ArrayList<String>(names.values())) {
            Entry e = StatsService.byName(name, hypixelKey.value, urchinKey.value);
            Row found = null;
            for (Row r : lobby) if (r.name.equalsIgnoreCase(name) || r.e.id.equals(e.id) || name.equalsIgnoreCase(r.e.realName)) found = r;
            for (Row r : top) if (r.name.equalsIgnoreCase(name)) found = r;
            if (found != null) { if (found.mark == null) found.mark = mark; continue; }
            Row r = new Row();
            r.name = e.state == StatsService.PENDING || e.state == StatsService.FAILED ? name : e.name;
            r.color = 0xFFFFFFFF; r.e = e; r.mark = mark; r.named = true;
            top.add(r);
        }
    }

    /** The number the list is ordered by. */
    private double key(Entry e) {
        if (game != 0) { // the other games have their own numbers; anything that has no match there sorts by threat
            if (e.state != StatsService.OK) return e.threat(game);
            if (sortBy.is("WLR")) return game == 1 ? e.dWlr() : e.sWlr();
            if (sortBy.is("Winstreak") && game == 1) return e.dWs;
            if (sortBy.is("Stars") && game == 2) return e.sLevel;
            return e.threat(game);
        }
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
            String title = Util.strip(o.getDisplayName()).toUpperCase();
            bedwars = title.contains("BED WARS");
            other = title.contains("DUELS") && inDuels.on() ? 1 : title.contains("SKYWARS") && inSkywars.on() ? 2 : 0;
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
        Fnt.draw(game == 1 ? "" : game == 2 ? "LVL" : "STAR", 40, 4, 6, head);
        Fnt.draw("PLAYER", 66, 4, 6, head);
        Setting.Bool[] cols = columns();
        String[] LABELS = labels(); int[] WIDTHS = widths();
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
            boolean nick = !r.named && e.id.version() != 4 || e.state == StatsService.NICKED;
            boolean urchin = !e.tag.isEmpty() && (r.mark == null || StatsService.dangerous(e.tag)); // a harmless note gives way to PIN, PARTY and MENTION
            String tag = urchin ? tagLabel(e.tag) : r.mark != null ? r.mark : nick ? "NICK" : null;
            if (tag != null) {
                // the chip sits on the middle of the row's capitals, and its own letters on the middle of the chip
                R.roundRect(5, y + 1.25f, 31, 8, 4, urchin ? tagColor(e.tag) : "PIN".equals(r.mark) ? 0xFF3D7BE0 : "PARTY".equals(r.mark) ? 0xFF2FA36B : "MENTION".equals(r.mark) ? 0xFFC9457F : 0xFF8E5BD9);
                Fnt.centerB(tag, 20.5f, y + 1.6f, 5, 0xFFFFFFFF);
            }
            String name = r.name + (e.realName != null ? " (" + e.realName + ")" : "");
            Fnt.draw(Fnt.fit(name, 7, 108), 66, y + 0.6f, 7, r.color);
            boolean ok = e.state == StatsService.OK;
            if (ok && game == 0) right(String.valueOf(e.star), 58, y + 0.6f, starColor(e.star));
            if (ok && game == 2) right(String.valueOf(e.sLevel), 58, y + 0.6f, starColor(e.sLevel * 20));
            String wait = e.state == StatsService.PENDING ? "..." : "-";
            for (int i = 0; i < cols.length; i++) {
                if (!cols[i].on()) continue;
                String v = wait; int c = dim;
                if (game == 1) {
                    if (ok) switch (i) {
                        case 0: v = String.format("%.2f", e.dWlr()); c = ratioColor(e.dWlr(), 0.5); break;
                        case 1: v = String.format("%.2f", e.dKdr()); c = ratioColor(e.dKdr(), 1); break;
                        case 2: v = e.dWs < 0 ? "?" : String.valueOf(e.dWs); c = e.dWs >= 10 ? 0xFFFF5A5A : e.dWs >= 4 ? 0xFFFFD75E : 0xFFAAB2C5; break;
                        case 3: v = e.dBestWs < 0 ? "?" : String.valueOf(e.dBestWs); c = e.dBestWs >= 30 ? 0xFFFF5A5A : e.dBestWs >= 10 ? 0xFFFFD75E : 0xFFAAB2C5; break;
                        case 4: v = count(e.dWins); c = e.dWins >= 5000 ? 0xFFFF5A5A : e.dWins >= 1000 ? 0xFFFFD75E : 0xFFDDE3F0; break;
                        case 5: v = count(e.dLosses); c = 0xFFDDE3F0; break;
                        default: v = count(e.dKills); c = 0xFFDDE3F0;
                    }
                } else if (game == 2) {
                    if (ok) switch (i) {
                        case 0: v = String.format("%.2f", e.sKdr()); c = ratioColor(e.sKdr(), 1); break;
                        case 1: v = String.format("%.2f", e.sWlr()); c = ratioColor(e.sWlr(), 0.12); break;
                        case 2: v = count(e.sWins); c = 0xFFDDE3F0; break;
                        case 3: v = count(e.sKills); c = 0xFFDDE3F0; break;
                        case 4: v = count(e.sDeaths); c = 0xFFDDE3F0; break;
                        default: v = count(e.sLosses); c = 0xFFDDE3F0;
                    }
                } else if (i == 4 || i == 5) { // Urchin's, with or without Hypixel's
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
