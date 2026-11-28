package dev.lumen.stats;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumen.online.Api;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Looks up Bed Wars stats and Urchin tags for the players in your game, a few at a time, and remembers them for a while.
 * Stats come from Hypixel's own API: with your key if you set one, otherwise through the Lumen server.
 * Tags come from Urchin with your Urchin key.
 */
public final class StatsService {
    public static final int PENDING = 0, OK = 1, NICKED = 2, NO_SOURCE = 3, FAILED = 4;

    public static final class Entry {
        public final UUID id;
        public final String name;
        public volatile int state = PENDING;
        public volatile int star, finals, wins, beds, winstreak = -1;
        public volatile double fkdr, wlr, bblr;
        /** Duels, all modes together. */
        public volatile int dWins, dLosses, dKills, dDeaths, dWs = -1, dBestWs = -1;
        /** SkyWars, all modes together. */
        public volatile int sLevel, sWins, sLosses, sKills, sDeaths;
        public double dWlr() { return dWins / (double) Math.max(1, dLosses); }
        public double dKdr() { return dKills / (double) Math.max(1, dDeaths); }
        public double sWlr() { return sWins / (double) Math.max(1, sLosses); }
        public double sKdr() { return sKills / (double) Math.max(1, sDeaths); }
        /** How dangerous they look in Duels (1) or SkyWars (2). */
        public double threat(int game) {
            if (game == 0) return threat();
            double t = state == NICKED ? 1e7 : state != OK ? 0 : game == 1 ? dWlr() * dWlr() * Math.sqrt(dWins) : sLevel * sKdr() * sKdr() / 10.0;
            return t + tagWeight(tag) * 1e8;
        }
        /** The last month's ratios (Urchin), -1 while unknown. */
        public volatile double mfkdr = -1, mbblr = -1;
        /** The winstreak is Urchin's estimate, because Hypixel hides this player's. */
        public volatile boolean winstreakGuess;
        /** Urchin tag ("" none) and its reason. */
        public volatile String tag = "", tagReason = "";
        /** Who a nicked player seems to be, from the skin they kept. */
        public volatile String realName;
        volatile long expires;
        Entry(UUID id, String name) { this.id = id; this.name = name; }
        /** One number to sort a lobby by: how dangerous they look. */
        public double threat() {
            double t = state == OK ? star * fkdr * fkdr / 10.0 : state == NICKED ? 1e7 : 0;
            return t + tagWeight(tag) * 1e8;
        }
    }

    private static final String HYPIXEL = System.getProperty("lumen.stats.hypixel", "https://api.hypixel.net");
    private static final String URCHIN = System.getProperty("lumen.stats.urchin", "https://api.urchin.gg/v3");
    private static final String MOJANG = System.getProperty("lumen.stats.mojang", "https://api.mojang.com");
    private static final long KEEP = TimeUnit.MINUTES.toMillis(30), RETRY = TimeUnit.SECONDS.toMillis(45);

    private static final ConcurrentHashMap<UUID, Entry> CACHE = new ConcurrentHashMap<UUID, Entry>();
    private static final ExecutorService POOL = new ThreadPoolExecutor(8, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        public Thread newThread(Runnable r) { Thread t = new Thread(r, "Noctua stats"); t.setDaemon(true); return t; }
    });
    static { ((ThreadPoolExecutor) POOL).allowCoreThreadTimeOut(true); }
    /** The Lumen server has no stats function (or refused): not asked again for a while. */
    private static volatile long serverOffUntil;
    /** Why the last lookup failed, for the overlay's footer. */
    public static volatile String problem = "";

    private StatsService() {}

    /** A made-up row for the HUD editor. */
    public static Entry sample(String name, int star, double fkdr, double wlr, double bblr, int ws, String tag) {
        Entry e = new Entry(UUID.nameUUIDFromBytes(name.getBytes()), name);
        e.star = star; e.fkdr = fkdr; e.wlr = wlr; e.bblr = bblr; e.winstreak = ws; e.tag = tag; e.state = OK;
        e.dWins = star * 9; e.dLosses = (int) (star * 9 / Math.max(0.3, wlr)); e.dKills = star * 10; e.dDeaths = e.dLosses; e.dWs = ws; e.dBestWs = ws * 2 + 3;
        e.sLevel = Math.max(1, star / 20); e.sWins = star * 2; e.sLosses = (int) (star * 2 / Math.max(0.1, wlr / 4)); e.sKills = star * 11; e.sDeaths = (int) (star * 11 / Math.max(0.3, fkdr / 3));
        e.mfkdr = fkdr * 1.2; e.mbblr = bblr * 1.1; e.finals = (int) (star * fkdr * 9); e.beds = (int) (star * bblr * 6); e.wins = (int) (star * wlr * 4);
        return e;
    }

    public static void clear() { CACHE.clear(); NAMES.clear(); serverOffUntil = 0; problem = ""; }

    /** A name being turned into an account, for players who are not in your tab list. */
    private static final class Named { volatile UUID id; volatile String proper; volatile long retryAt; Entry waiting; }
    private static final ConcurrentHashMap<String, Named> NAMES = new ConcurrentHashMap<String, Named>();

    /** Stats of a player known only by name. Never blocks; an unknown name ends up as FAILED. */
    public static Entry byName(final String name, String hypixelKey, String urchinKey) {
        String key = name.toLowerCase();
        Named n = NAMES.get(key);
        if (n == null || (n.id == null && n.retryAt != 0 && System.currentTimeMillis() > n.retryAt)) {
            final Named fresh = new Named();
            fresh.waiting = new Entry(UUID.nameUUIDFromBytes(("name:" + key).getBytes()), name);
            NAMES.put(key, fresh);
            n = fresh;
            POOL.execute(new Runnable() { public void run() {
                try {
                    JsonObject o = json(http(MOJANG + "/users/profiles/minecraft/" + URLEncoder.encode(name, "UTF-8"), null, null));
                    String raw = o.get("id").getAsString().replace("-", "");
                    fresh.proper = o.get("name").getAsString();
                    fresh.id = new UUID(Long.parseUnsignedLong(raw.substring(0, 16), 16), Long.parseUnsignedLong(raw.substring(16), 16));
                } catch (Throwable t) {
                    fresh.waiting.state = FAILED;
                    fresh.retryAt = System.currentTimeMillis() + RETRY;
                }
            } });
        }
        return n.id != null ? get(n.id, n.proper, null, hypixelKey, urchinKey) : n.waiting;
    }

    /** Never blocks: returns what is known and starts a lookup when there is nothing fresh. */
    public static Entry get(final UUID id, final String name, final String skinOwner, final String hypixelKey, final String urchinKey) {
        long now = System.currentTimeMillis();
        Entry e = CACHE.get(id);
        if (e != null && (e.state == PENDING || e.expires > now)) return e;
        final Entry fresh = new Entry(id, name);
        if (e != null) { fresh.tag = e.tag; fresh.tagReason = e.tagReason; fresh.mfkdr = e.mfkdr; fresh.mbblr = e.mbblr; }
        CACHE.put(id, fresh);
        POOL.execute(new Runnable() { public void run() {
            try { load(fresh, skinOwner, hypixelKey.trim(), urchinKey.trim()); }
            catch (NoSource n) { fresh.state = NO_SOURCE; }
            catch (Throwable t) { fresh.state = FAILED; problem = String.valueOf(t.getMessage()); }
            fresh.expires = System.currentTimeMillis() + (fresh.state == OK || fresh.state == NICKED ? KEEP : RETRY);
        } });
        return fresh;
    }

    private static void load(Entry e, String skinOwner, String hypixelKey, String urchinKey) throws Exception {
        String uuid = e.id.toString().replace("-", "");
        // Hypixel hands nicked players a made-up (version 1) id; real accounts are version 4.
        boolean nick = e.id.version() != 4;
        if (nick) {
            String real = null;
            if (skinOwner != null && !skinOwner.equalsIgnoreCase(e.name)) {
                try {
                    JsonObject o = json(http(MOJANG + "/users/profiles/minecraft/" + URLEncoder.encode(skinOwner, "UTF-8"), null, null));
                    real = o.get("id").getAsString();
                    e.realName = o.get("name").getAsString();
                } catch (Throwable ignored) {}
            }
            if (real == null) { e.state = NICKED; return; }
            uuid = real;
        }
        if (!urchinKey.isEmpty()) { // beside the stats lookup, not before it
            final Entry fe = e; final String fu = uuid, fk0 = urchinKey;
            POOL.execute(new Runnable() { public void run() { try { urchin(fe, fu, fk0); } catch (Throwable ignored) {} } });
            POOL.execute(new Runnable() { public void run() { try { monthly(fe, fu, fk0); } catch (Throwable ignored) {} } });
        }
        JsonObject player = player(uuid, hypixelKey);
        if (player == null) { if (e.state == PENDING) e.state = NICKED; return; }
        JsonObject bw = obj(obj(player, "stats"), "Bedwars");
        e.star = (int) num(obj(player, "achievements"), "bedwars_level");
        double fk = num(bw, "final_kills_bedwars"), fd = num(bw, "final_deaths_bedwars");
        double w = num(bw, "wins_bedwars"), l = num(bw, "losses_bedwars");
        double bb = num(bw, "beds_broken_bedwars"), bl = num(bw, "beds_lost_bedwars");
        e.finals = (int) fk; e.wins = (int) w; e.beds = (int) bb;
        e.fkdr = fk / Math.max(1, fd); e.wlr = w / Math.max(1, l); e.bblr = bb / Math.max(1, bl);
        e.winstreak = bw != null && bw.has("winstreak") ? (int) num(bw, "winstreak") : -1;
        JsonObject du = obj(obj(player, "stats"), "Duels"), sw = obj(obj(player, "stats"), "SkyWars");
        e.dWins = (int) num(du, "wins"); e.dLosses = (int) num(du, "losses"); e.dKills = (int) num(du, "kills"); e.dDeaths = (int) num(du, "deaths");
        e.dWs = du != null && du.has("current_winstreak") ? (int) num(du, "current_winstreak") : -1;
        e.dBestWs = du != null && du.has("best_overall_winstreak") ? (int) num(du, "best_overall_winstreak") : -1;
        e.sWins = (int) num(sw, "wins"); e.sLosses = (int) num(sw, "losses"); e.sKills = (int) num(sw, "kills"); e.sDeaths = (int) num(sw, "deaths");
        String lvl = str(sw, "levelFormatted").replaceAll("\u00a7.", "").replaceAll("[^0-9]", "");
        e.sLevel = lvl.isEmpty() || lvl.length() > 4 ? 0 : Integer.parseInt(lvl);
        e.state = OK;
        problem = "";
        if (e.winstreak < 0 && !urchinKey.isEmpty()) { try { winstreak(e, uuid, urchinKey); } catch (Throwable ignored) {} }
    }

    /** The player object of Hypixel's API, null when Hypixel has no such player (a nick). */
    private static JsonObject player(String uuid, String hypixelKey) throws Exception {
        String body;
        if (!hypixelKey.isEmpty()) {
            body = http(HYPIXEL + "/v2/player?uuid=" + uuid, "API-Key", hypixelKey);
        } else {
            if (System.currentTimeMillis() < serverOffUntil) throw new NoSource();
            try { body = http(Api.function("stats") + "?uuid=" + uuid, null, null); }
            catch (Status s) {
                if (s.code == 404 || s.code == 401 || s.code == 403 || s.code >= 500) { serverOffUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5); throw new NoSource(); }
                throw s;
            }
        }
        JsonObject root = json(body);
        JsonElement p = root.get("player");
        return p == null || p.isJsonNull() ? null : p.getAsJsonObject();
    }

    private static void urchin(Entry e, String uuid, String key) throws Exception {
        JsonObject root = json(http(URCHIN + "/player/tags?player=" + uuid, "X-API-Key", key));
        JsonElement tags = root.get("tags");
        String best = "", reason = "";
        if (tags != null && tags.isJsonArray()) {
            for (JsonElement t : (JsonArray) tags) {
                if (!t.isJsonObject()) continue;
                JsonObject o = t.getAsJsonObject();
                String type = str(o, "tag_type"); if (type.isEmpty()) type = str(o, "type");
                if (tagWeight(type) > tagWeight(best)) { best = type; reason = str(o, "reason"); }
            }
        }
        e.tag = best; e.tagReason = reason;
    }

    /** What changed in the last month, from Urchin's own record of the player. */
    private static void monthly(Entry e, String uuid, String key) throws Exception {
        JsonObject d = obj(json(http(URCHIN + "/player/sessions/monthly?player=" + uuid, "X-API-Key", key)), "delta");
        if (d == null) return;
        if (obj(d, "Bedwars") != null) d = obj(d, "Bedwars");
        if (!d.has("final_kills_bedwars") && !d.has("beds_broken_bedwars")) return;
        e.mfkdr = num(d, "final_kills_bedwars") / Math.max(1, num(d, "final_deaths_bedwars"));
        e.mbblr = num(d, "beds_broken_bedwars") / Math.max(1, num(d, "beds_lost_bedwars"));
    }

    private static void winstreak(Entry e, String uuid, String key) throws Exception {
        JsonObject modes = obj(json(http(URCHIN + "/player/winstreaks?player=" + uuid, "X-API-Key", key)), "modes");
        JsonElement list = modes == null ? null : modes.get("overall");
        if (list == null || !list.isJsonArray() || ((JsonArray) list).size() == 0) return;
        JsonObject best = null;
        for (JsonElement x : (JsonArray) list) { // the newest estimate
            if (x.isJsonObject() && (best == null || num(x.getAsJsonObject(), "timestamp") >= num(best, "timestamp"))) best = x.getAsJsonObject();
        }
        if (best != null && best.has("value")) { e.winstreak = (int) num(best, "value"); e.winstreakGuess = true; }
    }

    private static final String[] TAG_ORDER = {"info", "account", "caution", "legit_sniper", "possible_sniper", "closet_cheater", "confirmed_cheater", "blatant_cheater", "sniper"};
    public static int tagWeight(String type) {
        if (type == null || type.isEmpty()) return 0;
        String t = type.toLowerCase().replace(' ', '_');
        for (int i = 0; i < TAG_ORDER.length; i++) if (TAG_ORDER[i].equals(t)) return i + 1;
        return 1;
    }
    /** Whether a tag is one to be warned about (a cheater or a sniper), not just a note. */
    public static boolean dangerous(String type) { return tagWeight(type) >= 4; }

    static final class NoSource extends Exception { NoSource() { super("no stats source"); } }
    static final class Status extends Exception { final int code; Status(int code) { super("HTTP " + code); this.code = code; } }

    private static String http(String url, String header, String value) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(4000);
            c.setReadTimeout(7000);
            c.setRequestProperty("User-Agent", "NoctuaClient");
            c.setRequestProperty("Accept", "application/json");
            if (header != null) c.setRequestProperty(header, value); else if (url.startsWith(Api.function(""))) Api.sign(c);
            int code = c.getResponseCode();
            if (code >= 400) throw new Status(code);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) bo.write(buf, 0, n);
            in.close();
            return new String(bo.toByteArray(), "UTF-8");
        } finally { c.disconnect(); }
    }

    private static JsonObject json(String s) { return new JsonParser().parse(s).getAsJsonObject(); }
    private static JsonObject obj(JsonObject o, String k) { JsonElement e = o == null ? null : o.get(k); return e != null && e.isJsonObject() ? e.getAsJsonObject() : null; }
    private static double num(JsonObject o, String k) { try { return o.get(k).getAsDouble(); } catch (Throwable t) { return 0; } }
    private static String str(JsonObject o, String k) { try { return o.get(k).getAsString(); } catch (Throwable t) { return ""; } }
}
