package dev.lumen.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Session;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Rubies, the cosmetics store, Lumen+ and what other Lumen players are wearing.
 * All network calls run on one background thread; the game thread only reads the fields below.
 */
public final class Online {
    public enum Status { SIGNED_OUT, SIGNING_IN, READY, FAILED }

    public static final class Item {
        public final String id, slot, name;
        public final int price;
        /** 0 common, 1 rare, 2 epic, 3 legendary. */
        public final int rarity;
        Item(String id, String slot, String name, int price, int rarity) { this.id = id; this.slot = slot; this.name = name; this.price = price; this.rarity = rarity; }
        public boolean animated() { return dev.lumen.cosmetics.CapeAnim.is(id); }
    }

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) { Thread t = new Thread(r, "Noctua-Online"); t.setDaemon(true); return t; }
    });
    private static final long STALE_MS = 90000, RETRY_MS = 60000;

    public static volatile Status status = Status.SIGNED_OUT;
    /** Why signing in failed, or the result of the last action; shown in the store. */
    public static volatile String notice = "";
    public static volatile boolean busy;

    // ---- own account
    private static volatile String token;
    public static volatile int rubies, dailyRubies = 100;
    public static volatile boolean plus, dailyAvailable;
    public static volatile String plusUntil = "", contact = "", plusPriceText = "";
    public static volatile char nameColor;
    public static final String[] RARITY = {"Common", "Rare", "Epic", "Legendary"};
    public static final int[] RARITY_RGB = {0xFF9AA3B5, 0xFF4C8DFF, 0xFFA66BFF, 0xFFF5C542};
    public static final String KILL_ITEM = "killmsg_custom";
    /** Your own custom kill message ("" none). {victim} and {killer} stand for the two names. */
    public static volatile String killMsg = "";
    public static volatile List<Item> items = Collections.emptyList();
    public static volatile Set<String> owned = Collections.emptySet();
    public static volatile Map<String, String> equipped = Collections.emptyMap();
    /** {rubies, price in won} */
    public static volatile List<int[]> packages = Collections.emptyList();

    // ---- everyone else
    private static final Map<UUID, Outfit> outfits = new ConcurrentHashMap<UUID, Outfit>();
    /** Player name -> colour code, for Lumen+ members currently known. Replaced as a whole, never edited in place. */
    private static volatile Map<String, Character> nameColors = Collections.emptyMap();
    /** Store preview: slot -> item shown on your own player instead of what is equipped (item "" = nothing). */
    public static final Map<String, String> preview = new ConcurrentHashMap<String, String>();
    public static volatile boolean previewing;

    private static long lastSignIn, lastRefresh;
    private static int ticks;
    private static volatile boolean fetching;

    private Online() {}

    public static void start() { MinecraftForge.EVENT_BUS.register(new Online()); }

    // ------------------------------------------------------------------ what the renderers ask

    /** Item worn by this player in a slot, or null. */
    public static String wear(AbstractClientPlayer p, String slot) {
        if (p == mc.thePlayer) {
            if (previewing) {
                String pv = preview.get(slot);
                if (pv != null) return pv.isEmpty() ? null : pv;
            }
            return status == Status.READY ? equipped.get(slot) : null;
        }
        Outfit o = outfits.get(p.getUniqueID());
        return o == null ? null : o.get(slot);
    }

    /** A reserved character that is drawn as the Lumen mark; it is put in front of Lumen users' names. */
    public static final char MARK = '\uE0F1';
    public static final float MARK_WIDTH = 9f;

    /**
     * Marks the names of Lumen users with the Lumen icon and colours those of Lumen+ members, wherever text is
     * drawn, keeping whatever formatting was active around the name.
     */
    public static String colorize(String s) {
        Map<String, Character> map = nameColors;
        if (map.isEmpty() || s == null || s.length() < 3 || s.indexOf(MARK) >= 0) return s;
        boolean icon = dev.lumen.core.Client.nameIcon.on();
        for (Map.Entry<String, Character> e : map.entrySet()) {
            String name = e.getKey();
            int at = s.indexOf(name);
            if (at < 0) continue;
            StringBuilder out = new StringBuilder(s.length() + 8);
            int from = 0;
            while (at >= 0) {
                int end = at + name.length();
                boolean whole = (at == 0 || !word(s.charAt(at - 1))) && (end == s.length() || !word(s.charAt(end)));
                if (whole) {
                    out.append(s, from, at);
                    if (icon) out.append(MARK);
                    char color = e.getValue().charValue();
                    if (color != 0) out.append('§').append(color).append(name).append(formatAt(s, at)); else out.append(name);
                    from = end;
                }
                at = s.indexOf(name, end);
            }
            if (from > 0) s = out.append(s, from, s.length()).toString();
        }
        return s;
    }

    private static boolean word(char c) { return c == '_' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }

    /** The colour and style codes in effect just before position {@code at}, so they can be put back after a coloured name. */
    private static String formatAt(String s, int at) {
        String color = "§r", style = "";
        for (int i = 0; i + 1 < at; i++) {
            if (s.charAt(i) != '§') continue;
            char c = Character.toLowerCase(s.charAt(i + 1));
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) { color = "§" + c; style = ""; }
            else if (c == 'r') { color = "§r"; style = ""; }
            else if (c >= 'k' && c <= 'o') style += "§" + c;
            i++;
        }
        return color + style;
    }

    // ------------------------------------------------------------------ actions (store buttons)

    public static void signIn() {
        if (status == Status.SIGNING_IN) return;
        status = Status.SIGNING_IN;
        notice = "";
        lastSignIn = System.currentTimeMillis();
        worker.execute(new Runnable() {
            public void run() {
                try {
                    Session s = mc.getSession();
                    String access = s == null ? null : s.getToken();
                    boolean skipJoin = Boolean.getBoolean("lumen.api.skipJoin"); // local test backend only; the real one always asks Mojang
                    if (!skipJoin && (access == null || access.length() < 20)) { fail("Sign in with a Minecraft account to use the store."); return; }
                    // The same handshake as joining a server: Mojang records "this account is joining <id>", the backend then asks Mojang.
                    // The access token goes to Mojang only.
                    byte[] rnd = new byte[20];
                    new SecureRandom().nextBytes(rnd);
                    StringBuilder id = new StringBuilder();
                    for (byte b : rnd) id.append(String.format("%02x", b));
                    if (!skipJoin) mc.getSessionService().joinServer(s.getProfile(), access, id.toString());
                    JsonObject a = new JsonObject();
                    a.addProperty("p_username", s.getUsername());
                    a.addProperty("p_server_id", id.toString());
                    JsonObject r = Api.rpc("lumen_login", a).getAsJsonObject();
                    token = r.get("token").getAsString();
                    apply(r.getAsJsonObject("state"));
                    status = Status.READY;
                } catch (Api.Refused e) {
                    fail(message(e.getMessage()));
                } catch (Throwable t) {
                    fail("Could not reach the Noctua servers.");
                }
            }
        });
    }

    private static void fail(String why) { token = null; notice = why; status = Status.FAILED; }

    public static void refresh() { call("lumen_state", new JsonObject(), null); }
    public static void buy(String item) { JsonObject a = new JsonObject(); a.addProperty("p_item", item); call("lumen_buy", a, "Purchased!"); }
    public static void equip(String slot, String item) {
        JsonObject a = new JsonObject();
        a.addProperty("p_slot", slot);
        if (item == null) a.add("p_item", com.google.gson.JsonNull.INSTANCE); else a.addProperty("p_item", item);
        call("lumen_equip", a, null);
    }
    public static void setKillMsg(String text) { JsonObject a = new JsonObject(); a.addProperty("p_text", text); call("lumen_set_killmsg", a, "Kill message saved!"); }

    /** Text from the server or another player, made safe to print: no formatting codes, no control characters, 200 at most. */
    static String cleanKill(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) if (c >= 32 && c != 127 && c != '\u00a7') b.append(c);
        String out = b.toString().trim();
        return out.length() > 200 ? out.substring(0, 200) : out;
    }

    /** The colour Hypixel prints the #4,019 in. */
    public static final String KILL_NUMBER_COLOR = "e";

    /**
     * The saved text holds up to four parts joined with "||": kill, final kill, bed, and the colour code of the number.
     * Always returns four; the lines may be empty, the colour never is.
     */
    public static String[] killParts(String saved) {
        String[] out = {"", "", "", KILL_NUMBER_COLOR};
        String[] in = saved.split("\\|\\|", -1);
        for (int i = 0; i < 3 && i < in.length; i++) { String t = in[i].trim(); out[i] = t.length() > 64 ? t.substring(0, 64) : t; }
        if (in.length > 3 && in[3].trim().length() == 1 && "0123456789abcdef".indexOf(in[3].trim().charAt(0)) >= 0) out[3] = in[3].trim();
        return out;
    }
    public static String killJoin(String[] parts) {
        String k = parts[0].trim(), f = parts[1].trim(), b = parts[2].trim(), c = parts[3];
        if (!c.equals(KILL_NUMBER_COLOR)) return k + "||" + f + "||" + b + "||" + c;
        return f.isEmpty() && b.isEmpty() ? k : k + "||" + f + "||" + b;
    }

    /** The lines this killer has bought and switched on, or null. */
    static String[] killTemplate(String killer) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return null;
        if (killer.equalsIgnoreCase(mc.thePlayer.getName())) return equipped.containsKey("killmsg") && !killMsg.isEmpty() ? killParts(killMsg) : null;
        net.minecraft.client.network.NetworkPlayerInfo i = tabEntry(killer);
        Outfit o = i == null ? null : outfits.get(i.getGameProfile().getId());
        return o == null || o.kill == null || o.kill.isEmpty() ? null : killParts(o.kill);
    }

    private static net.minecraft.client.network.NetworkPlayerInfo tabEntry(String name) {
        for (net.minecraft.client.network.NetworkPlayerInfo i : mc.getNetHandler().getPlayerInfoMap()) {
            if (i.getGameProfile() != null && i.getGameProfile().getId() != null && name.equalsIgnoreCase(i.getGameProfile().getName())) return i;
        }
        return null;
    }

    /** A running count for one player: what their stats said when last fetched, plus what has been seen happen since. */
    private static final class Tally { Object from; int finals, beds; int lastFinal = -1, lastBed = -1; }
    private static final Map<String, Tally> tallies = new ConcurrentHashMap<String, Tally>();

    /**
     * Which final kill (bed = false) or broken bed this is for the player, counting this one; -1 when it cannot be known.
     * A number the server printed itself is taken as it is; otherwise it is their Hypixel total plus what happened since.
     */
    static int nextCount(String player, boolean bed, int printed) {
        Tally t = tallies.get(player.toLowerCase());
        if (t == null) { t = new Tally(); tallies.put(player.toLowerCase(), t); if (tallies.size() > 500) tallies.clear(); }
        if (printed > 0) { if (bed) t.lastBed = printed; else t.lastFinal = printed; return printed; }
        int last = bed ? t.lastBed : t.lastFinal;
        if (last > 0) { if (bed) t.lastBed = last + 1; else t.lastFinal = last + 1; return last + 1; }
        net.minecraft.client.network.NetworkPlayerInfo i = mc.getNetHandler() == null ? null : tabEntry(player);
        dev.lumen.stats.StatsService.Entry e = i == null ? null : dev.lumen.mods.StatsOverlay.lookup(i.getGameProfile().getId(), i.getGameProfile().getName());
        if (e == null || e.state != dev.lumen.stats.StatsService.OK) return -1;
        if (t.from != e) { t.from = e; t.finals = 0; t.beds = 0; }
        return bed ? e.beds + ++t.beds : e.finals + ++t.finals;
    }

    private static String hash(int n, String color) { return n > 0 ? "\u00a7" + color + "#" + java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(n) + "\u00a77" : ""; }
    private static int number(String s) { try { return s == null ? -1 : Integer.parseInt(s.replace(",", "")); } catch (Exception e) { return -1; } }
    /** Tidies what is left when a placeholder turned out empty. */
    private static String tidy(String s) { return s.replaceAll(" {2,}", " ").replaceAll(" ((?:\u00a7.)*)([.!,])", "$1$2").trim(); }

    /** Closes a line the way the server's own ends, unless the wording already ends in punctuation. */
    private static String end(String s, char mark) {
        String bare = s.replaceAll("\u00a7.", "").trim();
        return bare.isEmpty() || ".!?".indexOf(bare.charAt(bare.length() - 1)) >= 0 ? s : s + mark;
    }

    private static final String C = "((?:\u00a7.| )*)", N = "(\\w{1,16})";
    private static final java.util.regex.Pattern KILL = java.util.regex.Pattern.compile(
            "^((?:\u00a7.)*)" + N + C + "(?:was killed by|was knocked into the void by|was shot by)" + C + N + "((?:\u00a7.)*)\\.(.*)$"),
            FINAL_NO = java.util.regex.Pattern.compile("^((?:\u00a7.)*)" + N + C + "was" + C + N + "((?:\u00a7.)*)'s" + C + "final" + C + "#([\\d,]+)((?:\u00a7.)*)\\.(.*)$"),
            BED = java.util.regex.Pattern.compile("^(.*BED DESTRUCTION(?:\u00a7.| )*>(?:\u00a7.| )*?)((?:\u00a7.)*\\w+ Bed)" + C + "was(?: bed #([\\d,]+))?" + C + "destroyed by" + C + N + "((?:\u00a7.)*)!(.*)$");

    /** A default Bed Wars kill, final kill or bed line rewritten with the killer's own wording, or null when it is not one or they have none. */
    public static String rewriteKill(String formatted) {
        java.util.regex.Matcher m = BED.matcher(formatted);
        if (m.matches()) {
            String who = m.group(7);
            int n = nextCount(who, true, number(m.group(4))); // counted even when the line itself is left alone
            String[] tpl = killTemplate(who);
            if (tpl == null || tpl[2].isEmpty() || (n < 0 && tpl[2].contains("{beds}"))) return null; // no count to show: the server's line stands
            String kc = m.group(6).replace(" ", "");
            String t = tpl[2].contains("{bed}") ? tpl[2] : "{bed} " + tpl[2];
            boolean named = t.contains("{killer}");
            String out = t.replace("{bed}", m.group(2) + "\u00a77").replace("{killer}", kc + who + "\u00a77").replace("{beds}", hash(n, tpl[3])).replace("{victim}", m.group(2) + "\u00a77").replace("{finals}", "");
            return m.group(1) + end(tidy("\u00a77" + out), '!') + (named ? "" : " \u00a78(" + kc + who + "\u00a78)") + m.group(9);
        }
        String vc, victim, kc, killer, rest; int printed = -1;
        if ((m = FINAL_NO.matcher(formatted)).matches()) { vc = m.group(1); victim = m.group(2); kc = m.group(4).replace(" ", ""); killer = m.group(5); printed = number(m.group(9)); rest = m.group(11); }
        else if ((m = KILL.matcher(formatted)).matches()) { vc = m.group(1); victim = m.group(2); kc = m.group(4).replace(" ", ""); killer = m.group(5); rest = m.group(7); }
        else return null;
        boolean fin = printed > 0 || rest.contains("FINAL KILL");
        int n = fin ? nextCount(killer, false, printed) : -1;
        String[] tpl = killTemplate(killer);
        if (tpl == null) return null;
        // a final line that wants its number shown falls back to the kill line when the number is not known
        String t = fin && !tpl[1].isEmpty() && (n > 0 || !tpl[1].contains("{finals}")) ? tpl[1] : tpl[0];
        if (t.isEmpty()) return null;
        if (!t.contains("{victim}")) t = "{victim} " + t;
        boolean named = t.contains("{killer}");
        String out = t.replace("{victim}", vc + victim + "\u00a77").replace("{killer}", kc + killer + "\u00a77").replace("{finals}", hash(n, tpl[3])).replace("{beds}", "").replace("{bed}", "");
        return end(tidy("\u00a77" + out), '.') + (named ? "" : " \u00a78(" + kc + killer + "\u00a78)") + rest; // who did it must stay readable
    }

    /** Last of all, so everything else that reads kill lines still sees the server's own wording. */
    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public void onKillChat(net.minecraftforge.client.event.ClientChatReceivedEvent e) {
        if (e.type == 2 || e.isCanceled() || !dev.lumen.util.Util.onHypixel()) return;
        try {
            String raw = e.message.getFormattedText().replaceAll("\u00a7r", "");
            if (dev.lumen.util.Bw.playerChat(dev.lumen.util.Util.strip(raw))) return;
            String out = rewriteKill(raw);
            if (out != null) e.message = new net.minecraft.util.ChatComponentText(out);
        } catch (Throwable ignored) {}
    }

    public static void claimDaily() { call("lumen_daily", new JsonObject(), "Daily Rubies claimed!"); }
    public static void setColor(char c) {
        JsonObject a = new JsonObject();
        if (c == 0) a.add("p_color", com.google.gson.JsonNull.INSTANCE); else a.addProperty("p_color", String.valueOf(c));
        call("lumen_set_color", a, null);
    }

    private static void call(final String fn, final JsonObject args, final String done) {
        if (status != Status.READY || token == null) return;
        busy = true;
        lastRefresh = System.currentTimeMillis();
        args.addProperty("p_token", token);
        worker.execute(new Runnable() {
            public void run() {
                try {
                    apply(Api.rpc(fn, args).getAsJsonObject());
                    if (done != null) notice = done; else if (!fn.equals("lumen_state")) notice = "";
                } catch (Api.Refused e) {
                    if ("LUMEN_AUTH".equals(e.getMessage())) { status = Status.SIGNED_OUT; token = null; signIn(); }
                    else notice = message(e.getMessage());
                } catch (Throwable t) {
                    notice = "Could not reach the Noctua servers.";
                } finally {
                    busy = false;
                }
            }
        });
    }

    private static String message(String code) {
        if ("LUMEN_NOT_ENOUGH".equals(code)) return "Not enough Rubies.";
        if ("LUMEN_ALREADY_OWNED".equals(code)) return "You already own this.";
        if ("LUMEN_NOT_OWNED".equals(code)) return "You do not own this.";
        if ("LUMEN_DAILY_UNAVAILABLE".equals(code)) return "Daily Rubies are not available right now.";
        if ("LUMEN_PLUS_REQUIRED".equals(code)) return "This needs Noctua+.";
        if ("LUMEN_NOT_VERIFIED".equals(code)) return "Mojang could not confirm your account. Restart the game and try again.";
        if ("LUMEN_SLOW_DOWN".equals(code)) return "Too many attempts. Try again in a few minutes.";
        if ("LUMEN_NO_ITEM".equals(code)) return "That item is no longer sold.";
        return "The Noctua servers answered: " + code;
    }

    private static String str(JsonObject o, String k) { JsonElement e = o.get(k); return e == null || e.isJsonNull() ? "" : e.getAsString(); }

    private static void apply(JsonObject st) {
        rubies = st.get("rubies").getAsInt();
        plus = st.get("plus").getAsBoolean();
        plusUntil = str(st, "plus_until");
        String c = str(st, "name_color");
        nameColor = c.isEmpty() ? 0 : c.charAt(0);
        dailyAvailable = st.get("daily_available").getAsBoolean();
        try { dailyRubies = st.get("daily_rubies").getAsInt(); } catch (Throwable ignored) {}
        contact = str(st, "contact");
        killMsg = cleanKill(str(st, "kill_msg"));
        plusPriceText = str(st, "plus_price_text");
        List<Item> its = new ArrayList<Item>();
        for (JsonElement e : st.getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            int price = o.get("price").getAsInt();
            // servers from before rarity existed: go by the price, as the server itself does
            int rar = java.util.Arrays.asList("common", "rare", "epic", "legendary").indexOf(str(o, "rarity"));
            if (rar < 0) rar = price >= 5000 ? 3 : price >= 900 ? 2 : price >= 300 ? 1 : 0;
            its.add(new Item(o.get("id").getAsString(), o.get("slot").getAsString(), o.get("name").getAsString(), price, rar));
        }
        items = its;
        Set<String> own = new HashSet<String>();
        for (JsonElement e : st.getAsJsonArray("owned")) own.add(e.getAsString());
        owned = own;
        Map<String, String> eq = new HashMap<String, String>();
        for (Map.Entry<String, JsonElement> e : st.getAsJsonObject("equipped").entrySet()) eq.put(e.getKey(), e.getValue().getAsString());
        equipped = eq;
        List<int[]> pk = new ArrayList<int[]>();
        try {
            for (JsonElement e : st.getAsJsonArray("ruby_packages")) {
                JsonObject o = e.getAsJsonObject();
                pk.add(new int[]{o.get("rubies").getAsInt(), o.get("krw").getAsInt()});
            }
        } catch (Throwable ignored) {}
        packages = pk;
        rebuildColors();
    }

    private static void rebuildColors() {
        Map<String, Character> m = new HashMap<String, Character>();
        // Everyone the backend knows is a Lumen user and gets the mark; colour 0 means no Lumen+ colour.
        for (EntityPlayerName n : known.values()) if (n.name != null && n.name.length() >= 3) m.put(n.name, n.color);
        if (status == Status.READY && mc.getSession() != null) m.put(mc.getSession().getUsername(), plus ? nameColor : (char) 0);
        nameColors = m;
    }

    private static final class EntityPlayerName {
        final String name; final char color;
        EntityPlayerName(String name, char color) { this.name = name; this.color = color; }
    }
    private static final Map<UUID, EntityPlayerName> known = new ConcurrentHashMap<UUID, EntityPlayerName>();

    // ------------------------------------------------------------------ background upkeep

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++ticks % 40 != 0) return;
        long now = System.currentTimeMillis();
        if (status == Status.SIGNED_OUT || (status == Status.FAILED && now - lastSignIn > RETRY_MS * 5 && token == null && hasAccount())) {
            if (hasAccount() || Boolean.getBoolean("lumen.api.skipJoin")) signIn(); else if (status == Status.SIGNED_OUT) fail("Sign in with a Minecraft account to use the store.");
        }
        if (status == Status.READY && now - lastRefresh > 300000) refresh(); // picks up Rubies the admin granted meanwhile
        if (mc.theWorld == null || fetching) return;
        final List<UUID> ask = new ArrayList<UUID>();
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer) continue;
            UUID id = p.getUniqueID();
            if (id == null || id.version() != 4) continue; // NPCs and offline players have no account to look up
            Outfit o = outfits.get(id);
            if (o == null || now - o.fetched > STALE_MS) ask.add(id);
            if (ask.size() >= 150) break;
        }
        // on Hypixel also the players in the tab list, so a far-off killer's message is known when it is needed
        if (dev.lumen.util.Util.onHypixel() && mc.getNetHandler() != null) {
            for (net.minecraft.client.network.NetworkPlayerInfo i : mc.getNetHandler().getPlayerInfoMap()) {
                UUID id = i.getGameProfile() == null ? null : i.getGameProfile().getId();
                if (ask.size() >= 150 || id == null || id.version() != 4 || ask.contains(id) || id.equals(mc.thePlayer == null ? null : mc.thePlayer.getUniqueID())) continue;
                Outfit o = outfits.get(id);
                if (o == null || now - o.fetched > STALE_MS) ask.add(id);
            }
        }
        if (ask.isEmpty()) return;
        fetching = true;
        worker.execute(new Runnable() {
            public void run() {
                try {
                    JsonObject a = new JsonObject();
                    JsonArray arr = new JsonArray();
                    for (UUID id : ask) arr.add(new com.google.gson.JsonPrimitive(id.toString()));
                    a.add("p_uuids", arr);
                    JsonObject r = Api.rpc("lumen_players", a).getAsJsonObject();
                    long t = System.currentTimeMillis();
                    for (UUID id : ask) {
                        Outfit o = new Outfit();
                        o.fetched = t;
                        JsonElement pe = r.get(id.toString());
                        if (pe != null && pe.isJsonObject()) {
                            JsonObject po = pe.getAsJsonObject();
                            for (Map.Entry<String, JsonElement> en : po.getAsJsonObject("e").entrySet()) o.set(en.getKey(), en.getValue().getAsString());
                            String c = str(po, "c");
                            o.color = c.isEmpty() ? 0 : c.charAt(0);
                            String k = cleanKill(str(po, "k"));
                            o.kill = k.isEmpty() ? null : k;
                            known.put(id, new EntityPlayerName(str(po, "n"), o.color));
                        } else known.remove(id);
                        outfits.put(id, o);
                    }
                    if (outfits.size() > 2000) { outfits.clear(); known.clear(); }
                    rebuildColors();
                } catch (Throwable t) {
                    // Offline or backend down: mark them as checked so this is retried at the normal pace, not every two seconds.
                    long tm = System.currentTimeMillis();
                    for (UUID id : ask) if (!outfits.containsKey(id)) { Outfit o = new Outfit(); o.fetched = tm; outfits.put(id, o); }
                } finally {
                    fetching = false;
                }
            }
        });
    }

    private static boolean hasAccount() {
        Session s = mc.getSession();
        return s != null && s.getToken() != null && s.getToken().length() >= 20;
    }
}
