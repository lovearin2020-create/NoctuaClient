package dev.lumen.gui;

import dev.lumen.online.Online;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The Store tab of the menu: cosmetics by slot, Lumen+ and how to get Rubies. Drawn inside the menu's content area. */
final class StorePage {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String[] TABS = {"Capes", "Wings", "Hats", "Shields", "Extras"};
    private static final String[] SLOTS = {"cape", "wings", "hat", "shield", "killmsg"};
    private static final String[] SLOT_NAME = {"Cape", "Wings", "Hat", "Shield", "Kill message"};
    /** Tabs 0-4 are item types; Noctua+ and Rubies are pages opened from the buttons beside the balance. */
    private static final int EXTRAS = 4, PLUS = 6, RUBIES = 7;
    /** Width of the detail panel on the left, and the thumbnail grid beside it. */
    private static final int PANEL_W = 150, COLS = 4, TH = 60, TGAP = 5;
    private static boolean ownedOnly;
    /** The item picked in each tab (null: what you wear there, else the first). */
    private static final String[] picked = new String[5];
    /** The purchase window: the item being bought, and once it went through, the same item for the "purchased" view. */
    private static Online.Item buying, bought;
    private static long boughtAt;
    private static boolean buySent;
    /** The kill message being typed, and whether its box has the keyboard. */
    private static String[] killEdit = {"", "", "", Online.KILL_NUMBER_COLOR};
    private static String killLoaded;
    /** Which of the three boxes has the keyboard, -1 for none. */
    private static int killFocus = -1;
    private static final String[] KILL_ROWS = {"KILL", "FINAL", "BED"};
    private static final String[] KILL_HINT = {"{victim} was sent to the lobby by {killer}", "{victim} was {killer}'s final {finals}", "{bed} was bed {beds} destroyed by {killer}"};
    /** Ready-made sets: name, kill, final kill, bed. "Counter" is Hypixel's own numbered wording. */
    private static final String[][] PRESETS = {
            {"Counter", "{victim} was killed by {killer}", "{victim} was {killer}'s final {finals}", "{bed} was bed {beds} destroyed by {killer}"},
            {"Clapped", "{victim} got clapped by {killer}", "{victim} got clapped for good by {killer} {finals}", "{bed} got deleted by {killer} {beds}"},
            {"Pirate", "{victim} was sent to Davy Jones' locker by {killer}", "{victim} walked {killer}'s plank {finals}", "{bed} was plundered by {killer} {beds}"},
            {"Western", "{victim} was outdrawn by {killer}", "{victim} met {killer} at high noon {finals}", "{bed} was robbed by {killer} {beds}"},
            {"Chef", "{victim} was served by {killer}", "{victim} was cooked by {killer} {finals}", "{bed} was scrambled by {killer} {beds}"},
            {"Noctua", "{victim} was caught in the dark by {killer}", "{victim} never saw {killer} coming {finals}", "{bed} was eclipsed by {killer} {beds}"}};
    private static final int TEXT = 0xFFF2F4F8, MUTED = 0xFF8891A5, GREEN = 0xFF2FBF71, RUBY = 0xFFFF3B55, GOLD = 0xFFF5C542;
    private static final int CARD_H = TH, GAP = TGAP, PREVIEW_W = -PANEL_W;
    private static final String COLORS = "0123456789abcdef";
    private static final int[] COLOR_RGB = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private static int tab;
    private static float scroll, scrollTo;
    private static String armed = "";
    private static long armedAt;
    private static String hovered, hoveredSlot;

    private StorePage() {}

    static String num(int n) { return NumberFormat.getIntegerInstance(Locale.US).format(n); }

    /** Where to put text of this size so its capitals sit in the middle of a box this tall (the font keeps room above them). */
    private static float mid(float h, int size) { return h / 2f - size * 0.69f; }

    /** A brilliant-cut gem seen from the side: the Ruby symbol. Drawn from facets so it stays sharp at any size. */
    static void gem(float x, float y, float s, int color) {
        float l = x, r = x + s, cx = x + s / 2f, top = y + s * 0.06f, gird = y + s * 0.36f, tip = y + s * 0.98f;
        float tl = x + s * 0.22f, tr = x + s * 0.78f, il = x + s * 0.36f, ir = x + s * 0.64f;
        int light = R.mix(color, 0xFFFFFFFF, 0.45f), pale = R.mix(color, 0xFFFFFFFF, 0.22f), dark = R.mix(color, 0xFF000000, 0.22f), deep = R.mix(color, 0xFF000000, 0.42f);
        R.poly(pale, tl, top, il, gird, l, gird);                 // crown, left
        R.poly(light, tl, top, tr, top, ir, gird, il, gird);      // table
        R.poly(color, tr, top, r, gird, ir, gird);                // crown, right
        R.poly(color, l, gird, il, gird, cx, tip);                // pavilion, left
        R.poly(dark, il, gird, ir, gird, cx, tip);                // pavilion, middle
        R.poly(deep, ir, gird, r, gird, cx, tip);                 // pavilion, right
        R.poly(0xCCFFFFFF, tl + s * 0.08f, top + s * 0.07f, tl + s * 0.26f, top + s * 0.07f, tl + s * 0.2f, top + s * 0.17f, tl + s * 0.05f, top + s * 0.17f); // glint
    }

    static void opened() { scroll = scrollTo = 0; armed = ""; buying = bought = null; killFocus = -1; killLoaded = null; if (Online.status == Online.Status.READY) Online.refresh(); else if (Online.status != Online.Status.SIGNING_IN) Online.signIn(); }
    static void closed() { Online.previewing = false; Online.preview.clear(); killFocus = -1; buying = bought = null; }

    static void wheel(float step) { scrollTo += step; }

    /** Typing into the kill message boxes. True when the key was used. */
    static boolean key(char c, int key) {
        if (buying != null || bought != null) { // the purchase window takes the keyboard: Esc backs out, Enter confirms
            if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) { if (!Online.busy) buying = bought = null; }
            else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) { if (bought != null) bought = null; else confirmBuy(); }
            return true;
        }
        if (killFocus < 0) return false;
        String v = killEdit[killFocus];
        if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) killFocus = -1;
        else if (key == org.lwjgl.input.Keyboard.KEY_TAB) killFocus = (killFocus + 1) % 3;
        else if (key == org.lwjgl.input.Keyboard.KEY_RETURN) { killFocus = -1; saveKill(); }
        else if (key == org.lwjgl.input.Keyboard.KEY_BACK) { if (!v.isEmpty()) killEdit[killFocus] = v.substring(0, v.length() - 1); }
        else if (key == org.lwjgl.input.Keyboard.KEY_V && net.minecraft.client.gui.GuiScreen.isCtrlKeyDown()) {
            String clip = net.minecraft.util.ChatAllowedCharacters.filterAllowedCharacters(net.minecraft.client.gui.GuiScreen.getClipboardString()).replace("\u00a7", "").replace("|", "");
            killEdit[killFocus] = (v + clip).length() > 64 ? (v + clip).substring(0, 64) : v + clip;
        } else if (net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(c) && c != '\u00a7' && c != '|' && v.length() < 64) killEdit[killFocus] = v + c;
        return true;
    }

    private static boolean killChanged() { return !Online.killJoin(killEdit).equals(Online.killJoin(Online.killParts(Online.killMsg))); }
    private static void saveKill() { if (killChanged()) { killLoaded = null; Online.setKillMsg(Online.killJoin(killEdit)); } }

    /** Where the kill message editor sits on the Extras tab: x, y, width. */
    private static float[] killBox(int x0, int by, int w) { return new float[]{x0 + PANEL_W + 8, by + TH + TGAP + 6, w - PANEL_W - 8}; }
    private static float presetX(int i, float x) { float px = x + 42; for (int k = 0; k < i; k++) px += Fnt.width(PRESETS[k][0], 7) + 12; return px; }

    private static String fitTail(String s, int width) {
        String back = mc.fontRendererObj.trimStringToWidth(new StringBuilder(s).reverse().toString(), width);
        return back.length() < s.length() ? new StringBuilder(back).reverse().toString() : s;
    }

    private static void drawKill(int mx, int my, int x0, int by, int w) {
        float[] b = killBox(x0, by, w);
        float x = b[0], y = b[1], bw = b[2];
        boolean own = Online.owned.contains(Online.KILL_ITEM);
        Fnt.drawB("Your kill messages", x, y, 9, TEXT);
        if (!own) { Fnt.draw("Buy it to write the lines shown for your kills, final kills and beds.", x, y + 14, 7, MUTED); return; }
        // what the server holds goes into the boxes whenever it changes, never over something being typed
        if (!Online.killMsg.equals(killLoaded) && !Online.busy) { killLoaded = Online.killMsg; killEdit = Online.killParts(Online.killMsg); }
        Fnt.draw("PRESETS", x, y + 16, 6, MUTED);
        for (int i = 0; i < PRESETS.length; i++) {
            float px = presetX(i, x), pw = Fnt.width(PRESETS[i][0], 7) + 9;
            boolean hov = R.in(mx, my, px, y + 13, pw, 12), on = PRESETS[i][1].equals(killEdit[0]) && PRESETS[i][2].equals(killEdit[1]) && PRESETS[i][3].equals(killEdit[2]);
            R.roundRect(px, y + 13, pw, 12, 5, on ? R.ACCENT : hov ? Theme.ctlHover : Theme.card);
            Fnt.center(PRESETS[i][0], px + pw / 2f, y + 13 + mid(12, 7), 7, on || hov ? TEXT : MUTED);
        }
        for (int i = 0; i < 3; i++) {
            float fy = y + 30 + i * 19;
            Fnt.draw(KILL_ROWS[i], x, fy + mid(16, 6), 6, MUTED);
            R.roundRect(x + 30, fy, bw - 30, 16, 5, Theme.card);
            if (killFocus == i) R.roundOutline(x + 30, fy, bw - 30, 16, 5, 1f, R.ACCENT);
            boolean empty = killEdit[i].isEmpty() && killFocus != i;
            String shown = empty ? (i == 0 ? KILL_HINT[0] : "same as above - e.g. " + KILL_HINT[i]) : killEdit[i] + (killFocus == i && System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
            if (i == 2 && empty) shown = "left as the server writes it - e.g. " + KILL_HINT[2];
            R.mcText(fitTail(shown, (int) (bw - 40)), x + 35, fy + 4, empty ? 0xFF596074 : TEXT, false);
        }
        float hy = y + 89;
        Fnt.draw("{victim} {killer} {finals} {bed} {beds}", x + bw - Fnt.width("{victim} {killer} {finals} {bed} {beds}", 6), y + 2, 6, MUTED);
        Fnt.draw("NUMBER COLOR", x, hy + 3, 6, MUTED);
        for (int i = 0; i < 16; i++) {
            float sx = x + 62 + i * 11;
            boolean on = killEdit[3].charAt(0) == COLORS.charAt(i);
            R.roundRect(sx, hy + 1.5f, 9, 9, 2.5f, 0xFF000000 | COLOR_RGB[i]);
            if (on) R.roundOutline(sx - 1.5f, hy, 12, 12, 3.5f, 1f, TEXT); else if (i == 0) R.roundOutline(sx, hy + 1.5f, 9, 9, 2.5f, 0.6f, 0x55FFFFFF);
        }
        boolean changed = killChanged(), hov = R.in(mx, my, x + bw - 42, hy, 42, 13);
        R.roundRect(x + bw - 42, hy, 42, 13, 5, changed ? (hov ? 0xFF6AA0FF : R.ACCENT) : Theme.bar);
        Fnt.track("SAVE", x + bw - 21, hy + mid(13, 7), 7, Fnt.BOLD, 1.2f, changed ? TEXT : MUTED);
        // how the three lines will read
        String me = mc.getSession() == null ? "You" : mc.getSession().getUsername();
        String k = killEdit[0].trim().isEmpty() ? KILL_HINT[0] : killEdit[0].trim(), f = killEdit[1].trim().isEmpty() ? k : killEdit[1].trim(), bd = killEdit[2].trim();
        String nc = "\u00a7" + killEdit[3];
        String[] lines = {sample(k, me, "", "") + ".", sample(f, me, nc + "#4,019\u00a77", "") + ". \u00a7b\u00a7lFINAL KILL!",
                "\u00a7fBED > " + (bd.isEmpty() ? "\u00a7cRed Bed \u00a77was destroyed by \u00a79" + me + "\u00a77!" : sample(bd.contains("{bed}") ? bd : "{bed} " + bd, me, "", nc + "#812\u00a77") + "!")};
        R.roundRect(x, y + 106, bw, 37, 5, 0x66000000);
        for (int i = 0; i < 3; i++) R.mcText(mc.fontRendererObj.trimStringToWidth(lines[i], (int) (bw - 10)), x + 5, y + 109.5f + i * 11, 0xFFFFFFFF, true);
    }

    private static String sample(String t, String me, String finals, String beds) {
        if (!t.contains("{victim}") && !t.contains("{bed}")) t = "{victim} " + t;
        boolean named = t.contains("{killer}");
        String out = "\u00a77" + t.replace("{victim}", "\u00a7cSteve\u00a77").replace("{bed}", "\u00a7cRed Bed\u00a77").replace("{killer}", "\u00a79" + me + "\u00a77").replace("{finals}", finals).replace("{beds}", beds);
        return out.replaceAll(" {2,}", " ").trim() + (named ? "" : " \u00a78(\u00a79" + me + "\u00a78)");
    }

    private static List<Online.Item> list() {
        List<Online.Item> out = new ArrayList<Online.Item>();
        if (tab > EXTRAS) return out;
        for (Online.Item i : Online.items) if (i.slot.equals(SLOTS[tab]) && (!ownedOnly || Online.owned.contains(i.id))) out.add(i);
        return out;
    }

    private static Online.Item item(String id) { for (Online.Item i : Online.items) if (i.id.equals(id)) return i; return null; }

    /** The item the detail panel is about: the one picked, else the one worn in this slot, else the first. */
    private static Online.Item current(List<Online.Item> list) {
        Online.Item it = picked[tab] == null ? null : item(picked[tab]);
        if (it != null && list.contains(it)) return it;
        it = item(String.valueOf(Online.equipped.get(SLOTS[tab])));
        if (it != null && list.contains(it)) return it;
        return list.isEmpty() ? null : list.get(0);
    }

    private static void confirmBuy() {
        if (buying == null || buySent || Online.busy || Online.rubies < buying.price) return;
        buySent = true;
        Online.notice = "";
        Online.buy(buying.id);
    }

    /** A check mark about s wide, centred on (cx, cy). */
    private static void tick(float cx, float cy, float s, int color) {
        float t = s * 0.17f;
        float ax = cx - s * 0.42f, ay = cy + s * 0.02f, bx = cx - s * 0.12f, by = cy + s * 0.32f, ex = cx + s * 0.44f, ey = cy - s * 0.3f;
        R.poly(color, ax, ay - t, bx, by - t * 1.4f, bx, by + t * 0.6f, ax - t * 0.7f, ay + t * 0.4f);
        R.poly(color, bx, by - t * 1.4f, ex - t * 0.2f, ey - t, ex + t * 0.7f, ey, bx, by + t * 0.6f);
    }

    private static void pill(String text, float x, float y, int color) {
        float w = Fnt.widthB(text, 5) + 8;
        R.roundRect(x, y, w, 9, 4, R.alpha(color, 0.18f));
        Fnt.drawB(text, x + 4, y + mid(9, 5), 5, color);
    }
    private static float pillW(String text) { return Fnt.widthB(text, 5) + 8; }

    private static void price(int n, float x, float y, int size, int color) {
        gem(x, y + size * 0.16f, size * 0.92f, RUBY);
        Fnt.drawB(num(n), x + size * 1.2f, y, size, color);
    }
    private static float priceW(int n, int size) { return size * 1.2f + Fnt.widthB(num(n), size); }

    private static boolean button(String label, float x, float y, float w, float h, int color, int hover, int text, int mx, int my) {
        boolean hov = R.in(mx, my, x, y, w, h);
        R.roundRect(x, y, w, h, 6, hov ? hover : color);
        Fnt.track(label, x + w / 2f, y + mid(h, 7), 7, Fnt.BOLD, 1.2f, text);
        return hov;
    }

    static void draw(ModMenuScreen menu, int mx, int my, int x0, int y0, int w, int h) {
        boolean modal = buying != null || bought != null;
        int hx = modal ? -9999 : mx, hy = modal ? -9999 : my; // nothing under the purchase window reacts
        Fnt.drawB("Store", x0, y0 + 1, 13, TEXT);
        // top right: Noctua+, then the balance, which is also the way to more Rubies
        String bal = Online.status == Online.Status.READY ? num(Online.rubies) : "-";
        float bw = Fnt.widthB(bal, 9) + 38, bx = x0 + w - bw;
        R.roundRect(bx, y0, bw, 18, 6, tab == RUBIES ? R.alpha(RUBY, 0.30f) : R.in(hx, hy, bx, y0, bw, 18) ? R.alpha(RUBY, 0.22f) : R.alpha(RUBY, 0.13f));
        gem(bx + 7, y0 + 4.5f, 9, RUBY);
        Fnt.drawB(bal, bx + 19, y0 + mid(18, 9), 9, TEXT);
        Fnt.drawB("+", bx + bw - 12, y0 + mid(18, 10), 10, MUTED);
        float pw = Fnt.widthB("Noctua+", 8) + 16, pxx = bx - pw - 5;
        R.roundRect(pxx, y0, pw, 18, 6, tab == PLUS ? R.alpha(GOLD, 0.28f) : R.in(hx, hy, pxx, y0, pw, 18) ? R.alpha(GOLD, 0.2f) : R.alpha(GOLD, 0.1f));
        Fnt.drawB("Noctua+", pxx + 8, y0 + mid(18, 8), 8, GOLD);

        float tx = x0, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            float tw = Fnt.width(TABS[i], 8) + 18;
            boolean sel = i == tab, hov = R.in(hx, hy, tx, ty, tw, 16);
            R.roundRect(tx, ty, tw, 16, 6, sel ? R.ACCENT : hov ? Theme.ctlHover : 0);
            Fnt.center(TABS[i], tx + tw / 2f, ty + mid(16, 8), 8, sel || hov ? TEXT : MUTED);
            tx += tw + 3;
        }
        if (tab <= EXTRAS) { // only what you own
            float ow = 74, ox = x0 + w - ow;
            Fnt.draw("Owned only", ox, ty + mid(16, 7), 7, ownedOnly || R.in(hx, hy, ox, ty, ow, 16) ? TEXT : MUTED);
            R.roundRect(ox + ow - 22, ty + 3, 22, 10, 5, ownedOnly ? R.ACCENT : Theme.track);
            R.circle(ox + ow - (ownedOnly ? 5 : 17), ty + 8, 3.6f, 0xFFFFFFFF);
        }
        int by = y0 + 48, bh = h - 48 - 12;
        hovered = null;

        if (Online.status != Online.Status.READY) {
            boolean failed = Online.status == Online.Status.FAILED;
            Fnt.center(failed ? "The store is not available" : "Connecting to the Noctua servers...", x0 + w / 2f, by + bh / 2f - 22, 10, TEXT);
            if (failed) {
                R.mcText(Online.notice, x0 + w / 2f - mc.fontRendererObj.getStringWidth(Online.notice) / 2f, by + bh / 2f - 4, MUTED, false);
                boolean hov = R.in(mx, my, x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18);
                R.roundRect(x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18, 5, hov ? 0xFF6AA0FF : R.ACCENT);
                Fnt.track("TRY AGAIN", x0 + w / 2f, by + bh / 2f + 14 + mid(18, 7), 7, Fnt.BOLD, 1.2f, TEXT);
            }
            return;
        }

        if (tab <= EXTRAS) { drawItems(menu, hx, hy, x0, by, w, bh); if (tab == EXTRAS) drawKill(hx, hy, x0, by, w); }
        else if (tab == PLUS) drawPlus(hx, hy, x0, by, w, bh);
        else drawRubies(x0, by, w, bh);

        if (!modal && !Online.notice.isEmpty() && !Online.notice.equals("Purchased!")) R.mcText(Online.notice, x0 + (tab <= EXTRAS ? PANEL_W + 8 : 0), y0 + h - 9, Online.notice.endsWith("!") ? GREEN : 0xFFFFB86B, false);
        if (modal) drawBuy(menu, mx, my, x0, y0, w, h);
    }

    /** A small picture of the item for its card: the cape's own face, or you wearing it. */
    private static void thumb(ModMenuScreen menu, Online.Item it, float x, float y, float w, float h) {
        if (it.slot.equals("cape")) {
            net.minecraft.util.ResourceLocation tex = dev.lumen.cosmetics.CapeLayer.thumb(it.id);
            if (tex == null) return;
            float th = h, tw = th * 10f / 16f;
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(tex);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + (w - tw) / 2f, y, 0f);
            GlStateManager.scale(tw / 10f, th / 16f, 1f);
            net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect(0, 0, 1f, 1f, 10, 16, 10, 16, 64f, 32f);
            GlStateManager.popMatrix();
            R.reset();
            return;
        }
        if (it.slot.equals("killmsg")) { Fnt.centerB("#", x + w / 2f, y + h / 2f - 12, 18, GOLD); return; }
        if (mc.thePlayer == null) { Fnt.centerB(it.name.substring(0, 1), x + w / 2f, y + h / 2f - 9, 13, R.alpha(Online.RARITY_RGB[it.rarity], 0.8f)); return; }
        java.util.Map<String, String> keep = new java.util.HashMap<String, String>(Online.preview);
        Online.preview.clear();
        Online.preview.put(it.slot, it.id);
        boolean hat = it.slot.equals("hat"), wings = it.slot.equals("wings");
        float sc = hat ? h * 0.8f : wings ? h * 0.3f : h * 0.5f, feet = hat ? y + h / 2f + 1.72f * sc : wings ? y + h / 2f + 1.15f * sc : y + h / 2f + 0.95f * sc;
        menu.clip(x, y, w, h);
        Online.previewing = true;
        try { drawPlayer(x + w / 2f, feet, sc, hat ? 20f : it.slot.equals("shield") ? 115f : 180f); } finally { Online.previewing = false; }
        R.endScissor();
        R.reset();
        Online.preview.clear();
        Online.preview.putAll(keep);
    }

    private static void drawItems(ModMenuScreen menu, int mx, int my, int x0, int by, int w, int bh) {
        List<Online.Item> list = list();
        Online.Item cur = current(list);
        float gx = x0 + PANEL_W + 8, gw = w - PANEL_W - 8, cw = (gw - (COLS - 1) * TGAP) / COLS;
        int rows = (list.size() + COLS - 1) / COLS;
        float gh = tab == EXTRAS ? TH : bh;
        float max = Math.max(0, rows * (TH + TGAP) - TGAP - gh);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Util.lerp(scroll, scrollTo, 0.3f);

        // the grid: a picture, the name and the price; everything else is in the panel
        boolean inView = my >= by && my < by + gh;
        for (int i = 0; i < list.size(); i++) {
            Online.Item it = list.get(i);
            float cx = gx + (i % COLS) * (cw + TGAP), cy = by + (i / COLS) * (TH + TGAP) - scroll;
            if (cy + TH < by || cy > by + gh) continue;
            menu.clip(gx - 1, by, gw + 2, gh);
            boolean hov = inView && R.in(mx, my, cx, cy, cw, TH), sel = it == cur;
            boolean own = Online.owned.contains(it.id), on = it.id.equals(Online.equipped.get(it.slot));
            if (hov) { hovered = it.id; hoveredSlot = it.slot; }
            int rc = Online.RARITY_RGB[it.rarity];
            R.roundRect(cx, cy, cw, TH, 7, sel ? R.alpha(R.ACCENT, 0.16f) : hov ? Theme.cardHover : Theme.card);
            R.roundRect(cx + 7, cy + TH - 2.5f, cw - 14, 2, 1, rc);
            if (sel) R.roundOutline(cx, cy, cw, TH, 7, 1.2f, R.ACCENT); else if (on) R.roundOutline(cx, cy, cw, TH, 7, 1f, R.alpha(GREEN, 0.8f));
            float visTop = Math.max(by, cy + 5), visBot = Math.min(by + gh, cy + 35);
            if (visBot - visTop > 29) thumb(menu, it, cx + 4, cy + 5, cw - 8, 30);
            menu.clip(gx - 1, by, gw + 2, gh);
            if (it.animated()) R.circle(cx + 8, cy + 8, 2.2f, GOLD);
            if (own) { R.circle(cx + cw - 9, cy + 9, 5, on ? GREEN : 0xFF3A4254); tick(cx + cw - 9, cy + 9, 5, 0xFFFFFFFF); }
            Fnt.center(Fnt.fit(it.name, 7, cw - 6), cx + cw / 2f, cy + 37, 7, TEXT);
            if (own) Fnt.center(on ? "Wearing" : "Owned", cx + cw / 2f, cy + 46.5f, 6, on ? GREEN : MUTED);
            else if (it.price == 0) Fnt.center("Free", cx + cw / 2f, cy + 46.5f, 6, GREEN);
            else price(it.price, cx + cw / 2f - priceW(it.price, 6) / 2f, cy + 46.5f, 6, Online.rubies >= it.price ? TEXT : 0xFF7C8496);
            R.endScissor();
        }
        if (list.isEmpty()) Fnt.center(ownedOnly ? "You do not own any of these yet." : "Nothing here yet.", gx + gw / 2f, by + 30, 8, MUTED);
        if (max > 0) {
            float barH = Math.max(16, gh * gh / (gh + max));
            R.roundRect(gx + gw + 2, by + (gh - barH) * (scroll / max), 2.5f, barH, 1.2f, Theme.track);
        }

        // the panel: you, wearing what the mouse is on (or the picked item), and the one button
        float px = x0, pw = PANEL_W;
        R.roundRect(px, by, pw, bh, 8, Theme.card);
        R.gradient(px + 1, by + 1, pw - 2, 70, R.alpha(R.ACCENT, 0.13f), 0x00000000);
        Online.Item show = hovered != null ? item(hovered) : cur;
        float infoY = by + bh - 104;
        if (mc.thePlayer == null) {
            Fnt.center("Join a world to see it on you", px + pw / 2f, by + (infoY - by) / 2f - 4, 7, MUTED);
        } else if (show != null) {
            Online.preview.clear();
            Online.preview.put(show.slot, show.id);
            String slot = show.slot;
            float yaw = (slot.equals("hat") ? 0f : slot.equals("shield") ? 115f : 180f) + (float) Math.sin(System.currentTimeMillis() / 1400.0) * 38f;
            menu.clip(px + 1, by + 2, pw - 2, infoY - by - 4);
            Online.previewing = true;
            float sc = slot.equals("wings") ? 30 : slot.equals("hat") ? 50 : 52;
            try { drawPlayer(px + pw / 2f, infoY - 12, sc, yaw); } finally { Online.previewing = false; }
            R.endScissor();
            R.reset();
        }
        if (show == null) return;
        int rc = Online.RARITY_RGB[show.rarity];
        pill(Online.RARITY[show.rarity].toUpperCase(), px + 10, infoY, rc);
        if (show.animated()) pill("ANIMATED", px + 14 + pillW(Online.RARITY[show.rarity].toUpperCase()), infoY, GOLD);
        Fnt.drawB(Fnt.fit(show.name, 12, pw - 20), px + 10, infoY + 13, 12, TEXT);
        boolean own = Online.owned.contains(show.id), on = show.id.equals(Online.equipped.get(show.slot));
        Fnt.draw(SLOT_NAME[java.util.Arrays.asList(SLOTS).indexOf(show.slot)] + (on ? "  -  wearing" : own ? "  -  owned" : ""), px + 10, infoY + 30, 7, on ? GREEN : MUTED);
        if (!own) {
            if (show.price == 0) Fnt.drawB("Free", px + 10, infoY + 46, 11, GREEN);
            else price(show.price, px + 10, infoY + 46, 11, Online.rubies >= show.price ? TEXT : 0xFFFF7A8A);
        }
        // the button always acts on the picked item, so pointing at another card never changes what a click does
        if (cur == null) return;
        if (show != cur) { Fnt.center("Click the card to select it", px + pw / 2f, by + bh - 24, 7, MUTED); return; }
        boolean cOwn = Online.owned.contains(cur.id), cOn = cur.id.equals(Online.equipped.get(cur.slot));
        float bx = px + 10, bw = pw - 20, byy = by + bh - 30;
        if (cOn) button("TAKE OFF", bx, byy, bw, 20, Theme.bar, Theme.barHover, TEXT, mx, my);
        else if (cOwn) button("EQUIP", bx, byy, bw, 20, GREEN, 0xFF38D283, 0xFFFFFFFF, mx, my);
        else if (Online.rubies < cur.price) button("NEED " + num(cur.price - Online.rubies) + " MORE", bx, byy, bw, 20, Theme.bar, Theme.barHover, MUTED, mx, my);
        else button(cur.price == 0 ? "GET" : "BUY", bx, byy, bw, 20, R.ACCENT, 0xFF6AA0FF, 0xFFFFFFFF, mx, my);
    }

    /** The purchase window's place: x, y, w, h. */
    private static float[] buyBox(int x0, int y0, int w, int h) { return new float[]{x0 + w / 2f - 118, y0 + h / 2f - 78, 236, 156}; }

    private static void drawBuy(ModMenuScreen menu, int mx, int my, int x0, int y0, int w, int h) {
        // the purchase went through, or was refused
        if (buying != null && buySent && !Online.busy) {
            if (Online.owned.contains(buying.id)) { bought = buying; boughtAt = System.currentTimeMillis(); }
            buying = null; buySent = false;
            if (bought == null) return;
        }
        R.roundRect(x0 - 6, y0 - 6, w + 12, h + 12, 8, 0xB0060910);
        float[] b = buyBox(x0, y0, w, h);
        float x = b[0], y = b[1], bw = b[2], bh = b[3];
        Online.Item it = bought != null ? bought : buying;
        int rc = Online.RARITY_RGB[it.rarity];
        float grow = bought != null ? Math.min(1f, (System.currentTimeMillis() - boughtAt) / 260f) : 1f;
        R.glow(x + bw / 2f, y + bh / 2f, 150, R.alpha(bought != null ? GREEN : rc, 0.10f));
        R.roundRect(x, y, bw, bh, 10, 0xFF12172A);
        R.roundOutline(x, y, bw, bh, 10, 1f, R.alpha(bought != null ? GREEN : rc, 0.55f));
        R.roundRect(x + 14, y + 16, 50, 76, 7, 0xFF0B0F1C);
        thumb(menu, it, x + 16, y + 20, 46, 68);
        float tx = x + 76;
        if (bought != null) {
            R.circle(tx + 9, y + 25, 9 * grow, GREEN);
            if (grow >= 1f) tick(tx + 9, y + 25, 9, 0xFFFFFFFF);
            Fnt.drawB("PURCHASED", tx + 23, y + 20, 8, GREEN);
            Fnt.drawB(Fnt.fit(it.name, 13, bw - 90), tx, y + 40, 13, TEXT);
            pill(Online.RARITY[it.rarity].toUpperCase(), tx, y + 60, rc);
            Fnt.draw("It is yours now.", tx, y + 76, 7, MUTED);
            Fnt.draw("Rubies left", tx, y + 90, 7, MUTED);
            price(Online.rubies, x + bw - 14 - priceW(Online.rubies, 8), y + 89, 8, TEXT);
            float half = (bw - 28 - 6) / 2f;
            button("CLOSE", x + 14, y + bh - 34, half, 22, Theme.bar, Theme.barHover, TEXT, mx, my);
            boolean on = it.id.equals(Online.equipped.get(it.slot));
            button(on ? "WEARING" : "EQUIP NOW", x + 14 + half + 6, y + bh - 34, half, 22, GREEN, 0xFF38D283, 0xFFFFFFFF, mx, my);
            return;
        }
        Fnt.draw("CONFIRM PURCHASE", tx, y + 17, 6, MUTED);
        Fnt.drawB(Fnt.fit(it.name, 13, bw - 90), tx, y + 27, 13, TEXT);
        pill(Online.RARITY[it.rarity].toUpperCase(), tx, y + 46, rc);
        if (it.animated()) pill("ANIMATED", tx + 4 + pillW(Online.RARITY[it.rarity].toUpperCase()), y + 46, GOLD);
        String[] rowsL = {"Price", "Your Rubies", "After purchase"};
        int[] rowsV = {it.price, Online.rubies, Online.rubies - it.price};
        for (int i = 0; i < 3; i++) {
            float ry = y + 62 + i * 11;
            Fnt.draw(rowsL[i], tx, ry, 7, MUTED);
            price(rowsV[i], x + bw - 14 - priceW(rowsV[i], 7), ry, 7, i == 2 ? GREEN : TEXT);
        }
        R.rect(tx, y + 96, bw - 90, 1, 0x22FFFFFF);
        float half = (bw - 28 - 6) / 2f;
        boolean wait = buySent;
        button("CANCEL", x + 14, y + bh - 34, half, 22, Theme.bar, Theme.barHover, wait ? MUTED : TEXT, mx, my);
        button(wait ? "BUYING..." : "BUY FOR " + num(it.price), x + 14 + half + 6, y + bh - 34, half, 22, wait ? Theme.bar : R.ACCENT, wait ? Theme.bar : 0xFF6AA0FF, 0xFFFFFFFF, mx, my);
        if (!Online.notice.isEmpty() && !wait) R.mcText(Online.notice, x + 14, y + bh - 9.5f, 0xFFFFB86B, false);
    }

    /** Same as the inventory screen's player figure, but facing where we say. */
    private static void drawPlayer(float x, float y, float scale, float yaw) {
        EntityLivingBase e = mc.thePlayer;
        GlStateManager.enableColorMaterial();
        GlStateManager.enableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 60f);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180f, 0f, 0f, 1f);
        float a = e.renderYawOffset, b = e.rotationYaw, c = e.rotationPitch, d = e.prevRotationYawHead, f = e.rotationYawHead, pa = e.prevRenderYawOffset, pb = e.prevRotationYaw, pc = e.prevRotationPitch;
        GlStateManager.rotate(135f, 0f, 1f, 0f);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135f, 0f, 1f, 0f);
        GlStateManager.rotate(-8f, 1f, 0f, 0f);
        e.renderYawOffset = e.prevRenderYawOffset = yaw; e.rotationYaw = e.prevRotationYaw = yaw; e.rotationPitch = e.prevRotationPitch = 0f;
        e.rotationYawHead = e.prevRotationYawHead = yaw;
        RenderManager rm = mc.getRenderManager();
        rm.setPlayerViewY(180f);
        rm.setRenderShadow(false);
        try { rm.renderEntityWithPosYaw(e, 0.0, 0.0, 0.0, 0f, 1f); } catch (Throwable ignored) {}
        rm.setRenderShadow(true);
        e.renderYawOffset = a; e.rotationYaw = b; e.rotationPitch = c; e.prevRotationYawHead = d; e.rotationYawHead = f;
        e.prevRenderYawOffset = pa; e.prevRotationYaw = pb; e.prevRotationPitch = pc;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableDepth();
    }

    private static void drawPlus(int mx, int my, int x0, int by, int w, int bh) {
        R.roundRect(x0, by, w, 60, Theme.radius, Theme.card);
        Fnt.draw("Noctua+", x0 + 12, by + 9, 14, Fnt.BOLD, 0, GOLD, 0xFFFF8A3D);
        String status = Online.plus ? "Active until " + (Online.plusUntil.length() >= 10 ? Online.plusUntil.substring(0, 10) : Online.plusUntil) : "Not a member";
        Fnt.draw(status, x0 + 12, by + 30, 8, Online.plus ? GREEN : MUTED);
        Fnt.draw("Coloured name for every Noctua user  -  " + num(Online.dailyRubies) + " Rubies every day", x0 + 12, by + 43, 8, MUTED);

        float y = by + 68;
        if (!Online.plus) {
            R.roundRect(x0, y, w, 52, Theme.radius, Theme.card);
            Fnt.draw("Noctua+ is given out by the admin.", x0 + 12, y + 9, 9, TEXT);
            if (!Online.plusPriceText.isEmpty()) R.mcText(Online.plusPriceText, x0 + 12, y + 24, GOLD, false);
            R.mcText(Online.contact.isEmpty() ? "Contact the admin to join." : "Contact: " + Online.contact, x0 + 12, y + 37, MUTED, false);
            return;
        }
        // daily
        R.roundRect(x0, y, w, 30, Theme.radius, Theme.card);
        Fnt.draw("Daily Rubies", x0 + 12, y + mid(30, 9), 9, TEXT);
        boolean can = Online.dailyAvailable && !Online.busy, hov = can && R.in(mx, my, x0 + w - 122, y + 6, 112, 18);
        R.roundRect(x0 + w - 122, y + 6, 112, 18, 5, can ? (hov ? 0xFF38D283 : GREEN) : Theme.ctl);
        Fnt.track(can ? "CLAIM " + num(Online.dailyRubies) : "COME BACK TOMORROW", x0 + w - 66, y + 6 + mid(18, 7), 7, Fnt.BOLD, 1f, can ? TEXT : MUTED);
        // colours
        y += 38;
        R.roundRect(x0, y, w, 62, Theme.radius, Theme.card);
        Fnt.draw("Name colour", x0 + 12, y + 8, 9, TEXT);
        String me = mc.getSession() == null ? "You" : mc.getSession().getUsername();
        R.mcText(me, x0 + w - 12 - mc.fontRendererObj.getStringWidth(me), y + 8, Online.nameColor == 0 ? 0xFFFFFFFF : 0xFF000000 | COLOR_RGB[Math.max(0, COLORS.indexOf(Online.nameColor))], true);
        for (int i = 0; i <= 16; i++) {
            float sx = x0 + 12 + i * 17.3f, sy = y + 26;
            boolean sel = i == 16 ? Online.nameColor == 0 : Online.nameColor == COLORS.charAt(i);
            boolean h = R.in(mx, my, sx, sy, 14, 14);
            if (i == 16) { R.roundRect(sx, sy, 14, 14, 3, Theme.ctl); Fnt.center("x", sx + 7, sy + mid(14, 8) - 0.6f, 8, MUTED); }
            else R.roundRect(sx, sy, 14, 14, 3, 0xFF000000 | COLOR_RGB[i]);
            if (sel || h) R.roundOutline(sx - 1.5f, sy - 1.5f, 17, 17, 4, 1.4f, sel ? 0xFFFFFFFF : MUTED);
        }
        Fnt.draw("Shown on your name tag, in chat and in the tab list for players using Noctua.", x0 + 12, y + 47, 7, MUTED);
    }

    private static void drawRubies(int x0, int by, int w, int bh) {
        R.roundRect(x0, by, w, 34, Theme.radius, Theme.card);
        gem(x0 + 11, by + 10, 14, RUBY);
        Fnt.drawB("To buy Rubies, contact the admin.", x0 + 32, by + 7, 10, TEXT);
        R.mcText(Online.contact.isEmpty() ? "The admin adds the Rubies to your account by hand." : "Contact: " + Online.contact, x0 + 32, by + 21, MUTED, false);
        float y = by + 42;
        List<int[]> pk = Online.packages;
        int best = pk.isEmpty() ? 0 : pk.get(0)[1] * 1000 / Math.max(1, pk.get(0)[0]);
        for (int i = 0; i < pk.size() && y + 22 <= by + bh; i++) {
            int[] p = pk.get(i);
            R.roundRect(x0, y, w, 20, 5, Theme.card);
            gem(x0 + 12, y + 5.5f, 9, RUBY);
            Fnt.drawB(num(p[0]) + " Rubies", x0 + 27, y + mid(20, 9), 9, TEXT);
            String price = num(p[1]) + " KRW";
            Fnt.drawB(price, x0 + w - 12 - Fnt.widthB(price, 9), y + mid(20, 9), 9, GOLD);
            int per = p[1] * 1000 / Math.max(1, p[0]);
            if (i > 0 && best > per) {
                String save = "-" + Math.round((best - per) * 100f / best) + "%";
                Fnt.draw(save, x0 + w - 24 - Fnt.widthB(price, 9) - Fnt.width(save, 8), y + mid(20, 8), 8, GREEN);
            }
            y += 24;
        }
    }

    static void click(int mx, int my, int button, int x0, int y0, int w, int h) {
        if (buying != null || bought != null) { // only the purchase window listens
            float[] b = buyBox(x0, y0, w, h);
            float half = (b[2] - 28 - 6) / 2f, byy = b[1] + b[3] - 34;
            boolean left = R.in(mx, my, b[0] + 14, byy, half, 22), right = R.in(mx, my, b[0] + 14 + half + 6, byy, half, 22);
            if (bought != null) {
                if (right && !bought.id.equals(Online.equipped.get(bought.slot)) && !Online.busy) Online.equip(bought.slot, bought.id);
                if (left || right || !R.in(mx, my, b[0], b[1], b[2], b[3])) bought = null;
            } else if (!buySent) {
                if (right) confirmBuy();
                else if (left || !R.in(mx, my, b[0], b[1], b[2], b[3])) buying = null;
            }
            return;
        }
        // Noctua+ and the balance, top right
        String bal = Online.status == Online.Status.READY ? num(Online.rubies) : "-";
        float balW = Fnt.widthB(bal, 9) + 38, balX = x0 + w - balW, plusW = Fnt.widthB("Noctua+", 8) + 16;
        if (R.in(mx, my, balX, y0, balW, 18)) { tab = tab == RUBIES ? 0 : RUBIES; scroll = scrollTo = 0; return; }
        if (R.in(mx, my, balX - plusW - 5, y0, plusW, 18)) { tab = tab == PLUS ? 0 : PLUS; scroll = scrollTo = 0; return; }
        float tx = x0, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            float tw = Fnt.width(TABS[i], 8) + 18;
            if (R.in(mx, my, tx, ty, tw, 16)) { tab = i; scroll = scrollTo = 0; armed = ""; Online.notice = Online.status == Online.Status.READY ? "" : Online.notice; return; }
            tx += tw + 3;
        }
        if (tab <= EXTRAS && R.in(mx, my, x0 + w - 74, ty, 74, 16)) { ownedOnly = !ownedOnly; scroll = scrollTo = 0; return; }
        int by = y0 + 48, bh = h - 48 - 12;
        if (Online.status != Online.Status.READY) {
            if (Online.status == Online.Status.FAILED && R.in(mx, my, x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18)) Online.signIn();
            return;
        }
        if (Online.busy) return;
        killFocus = -1;
        if (tab == EXTRAS && Online.owned.contains(Online.KILL_ITEM)) {
            float[] b = killBox(x0, by, w);
            for (int i = 0; i < 3; i++) if (R.in(mx, my, b[0] + 30, b[1] + 30 + i * 19, b[2] - 30, 16)) { killFocus = i; return; }
            for (int i = 0; i < PRESETS.length; i++) {
                if (R.in(mx, my, presetX(i, b[0]), b[1] + 13, Fnt.width(PRESETS[i][0], 7) + 9, 12)) { killEdit = new String[]{PRESETS[i][1], PRESETS[i][2], PRESETS[i][3], killEdit[3]}; return; }
            }
            for (int i = 0; i < 16; i++) {
                if (R.in(mx, my, b[0] + 62 + i * 11, b[1] + 90, 10, 11)) { killEdit[3] = String.valueOf(COLORS.charAt(i)); return; }
            }
            if (R.in(mx, my, b[0] + b[2] - 42, b[1] + 89, 42, 13)) { saveKill(); return; }
        }
        if (tab <= EXTRAS) {
            List<Online.Item> list = list();
            Online.Item cur = current(list);
            float gx = x0 + PANEL_W + 8, gw = w - PANEL_W - 8, cw = (gw - (COLS - 1) * TGAP) / COLS, gh = tab == EXTRAS ? TH : bh;
            if (my >= by && my < by + gh) for (int i = 0; i < list.size(); i++) {
                if (R.in(mx, my, gx + (i % COLS) * (cw + TGAP), by + (i / COLS) * (TH + TGAP) - scroll, cw, TH)) { picked[tab] = list.get(i).id; Online.notice = ""; return; }
            }
            if (cur != null && R.in(mx, my, x0 + 10, by + bh - 30, PANEL_W - 20, 20)) {
                boolean own = Online.owned.contains(cur.id), on = cur.id.equals(Online.equipped.get(cur.slot));
                if (on) Online.equip(cur.slot, null);
                else if (own) Online.equip(cur.slot, cur.id);
                else if (Online.rubies < cur.price) { tab = RUBIES; scroll = scrollTo = 0; }
                else { buying = cur; bought = null; buySent = false; Online.notice = ""; }
            }
        } else if (tab == PLUS && Online.plus) {
            float y = by + 68;
            if (Online.dailyAvailable && R.in(mx, my, x0 + w - 122, y + 6, 112, 18)) { Online.claimDaily(); return; }
            y += 38;
            for (int i = 0; i <= 16; i++) {
                if (R.in(mx, my, x0 + 12 + i * 17.3f, y + 26, 14, 14)) { Online.setColor(i == 16 ? 0 : COLORS.charAt(i)); return; }
            }
        }
    }
}
