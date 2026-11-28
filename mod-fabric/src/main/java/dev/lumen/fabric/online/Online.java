package dev.lumen.fabric.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.player.AbstractClientPlayer;

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
        public boolean animated() { return dev.lumen.fabric.cosmetics.CapeAnim.is(id); }
    }

    public static final String[] RARITY = {"Common", "Rare", "Epic", "Legendary"};
    public static final int[] RARITY_RGB = {0xFF9AA3B5, 0xFF4C8DFF, 0xFFA66BFF, 0xFFF5C542};
    private static final Minecraft mc = Minecraft.getInstance();
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

    // ------------------------------------------------------------------ what the renderers ask

    /** Item worn by this player in a slot, or null. */
    public static String wear(Object p, String slot) {
        if (!(p instanceof AbstractClientPlayer player)) return null;
        if (p == mc.player) {
            if (previewing) {
                String pv = preview.get(slot);
                if (pv != null) return pv.isEmpty() ? null : pv;
            }
            return status == Status.READY ? equipped.get(slot) : null;
        }
        Outfit o = outfits.get(player.getUUID());
        return o == null ? null : o.get(slot);
    }

    /** A reserved character that is drawn as the Lumen mark; it is put in front of Lumen users' names. */
    public static final char MARK = '\uE0F1';

    /** True while there is any name to mark or colour (so text that cannot contain one is left alone cheaply). */
    public static boolean marking() { return !nameColors.isEmpty(); }

    /**
     * Marks the names of Lumen users with the Lumen icon and colours those of Lumen+ members, wherever text is
     * drawn, keeping whatever formatting was active around the name.
     */
    public static String colorize(String s) {
        Map<String, Character> map = nameColors;
        if (map.isEmpty() || s == null || s.length() < 3 || s.indexOf(MARK) >= 0) return s;
        boolean icon = dev.lumen.fabric.core.Client.nameIcon.on();
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
                    User s = mc.getUser();
                    String access = s == null ? null : s.getAccessToken();
                    boolean skipJoin = Boolean.getBoolean("lumen.api.skipJoin"); // local test backend only; the real one always asks Mojang
                    if (!skipJoin && (access == null || access.length() < 20)) { fail("Sign in with a Minecraft account to use the store."); return; }
                    // The same handshake as joining a server: Mojang records "this account is joining <id>", the backend then asks Mojang.
                    // The access token goes to Mojang only.
                    byte[] rnd = new byte[20];
                    new SecureRandom().nextBytes(rnd);
                    StringBuilder id = new StringBuilder();
                    for (byte b : rnd) id.append(String.format("%02x", b));
                    if (!skipJoin) mc.services().sessionService().joinServer(s.getProfileId(), access, id.toString());
                    JsonObject a = new JsonObject();
                    a.addProperty("p_username", s.getName());
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
        plusPriceText = str(st, "plus_price_text");
        List<Item> its = new ArrayList<Item>();
        for (JsonElement e : st.getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            int price = o.get("price").getAsInt();
            // servers from before rarity existed: go by the price, as the server itself does
            int rar = java.util.Arrays.asList("common", "rare", "epic", "legendary").indexOf(str(o, "rarity"));
            if (rar < 0) rar = price >= 5000 ? 3 : price >= 900 ? 2 : price >= 300 ? 1 : 0;
            String slot = o.get("slot").getAsString();
            if (!java.util.Arrays.asList("cape", "wings", "hat", "shield").contains(slot)) continue; // 1.8.9-only extras
            its.add(new Item(o.get("id").getAsString(), slot, o.get("name").getAsString(), price, rar));
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
        if (status == Status.READY && mc.getUser() != null) m.put(mc.getUser().getName(), plus ? nameColor : (char) 0);
        nameColors = m;
    }

    private static final class EntityPlayerName {
        final String name; final char color;
        EntityPlayerName(String name, char color) { this.name = name; this.color = color; }
    }
    private static final Map<UUID, EntityPlayerName> known = new ConcurrentHashMap<UUID, EntityPlayerName>();

    // ------------------------------------------------------------------ background upkeep

    /** Called once per client tick. */
    public static void tick() {
        if (++ticks % 40 != 0) return;
        long now = System.currentTimeMillis();
        if (status == Status.SIGNED_OUT || (status == Status.FAILED && now - lastSignIn > RETRY_MS * 5 && token == null && hasAccount())) {
            if (hasAccount() || Boolean.getBoolean("lumen.api.skipJoin")) signIn(); else if (status == Status.SIGNED_OUT) fail("Sign in with a Minecraft account to use the store.");
        }
        if (status == Status.READY && now - lastRefresh > 300000) refresh(); // picks up Rubies the admin granted meanwhile
        if (mc.level == null || fetching) return;
        final List<UUID> ask = new ArrayList<UUID>();
        for (AbstractClientPlayer p : mc.level.players()) {
            if (p == mc.player) continue;
            UUID id = p.getUUID();
            if (id == null || id.version() != 4) continue; // NPCs and offline players have no account to look up
            Outfit o = outfits.get(id);
            if (o == null || now - o.fetched > STALE_MS) ask.add(id);
            if (ask.size() >= 150) break;
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
        User s = mc.getUser();
        return s != null && s.getAccessToken() != null && s.getAccessToken().length() >= 20;
    }
}
