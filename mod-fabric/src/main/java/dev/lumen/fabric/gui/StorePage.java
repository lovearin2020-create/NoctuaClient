package dev.lumen.fabric.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lumen.fabric.online.Online;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The Store tab of the menu: cosmetics by slot, Lumen+ and how to get Rubies. Drawn inside the menu's content area. */
final class StorePage {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final String[] TABS = {"Capes", "Wings", "Hats", "Shields"};
    private static final String[] SLOTS = {"cape", "wings", "hat", "shield"};
    private static final String[] SLOT_NAME = {"Cape", "Wings", "Hat", "Shield"};
    /** Tabs 0-3 are item types; Noctua+ and Rubies are pages opened from the buttons beside the balance. */
    private static final int LAST = 3, PLUS = 6, RUBIES = 7;
    /** Width of the detail panel on the left, and the thumbnail grid beside it. */
    private static final int PANEL_W = 150, COLS = 4, TH = 60, TGAP = 5;
    private static boolean ownedOnly;
    /** The item picked in each tab (null: what you wear there, else the first). */
    private static final String[] picked = new String[4];
    /** The purchase window: the item being bought, and once it went through, the same item for the "purchased" view. */
    private static Online.Item buying, bought;
    private static long boughtAt;
    private static boolean buySent;
    private static final int TEXT = 0xFFF2F4F8, MUTED = 0xFF8891A5, GREEN = 0xFF2FBF71, RUBY = 0xFFFF3B55, GOLD = 0xFFF5C542;
    private static final int CARD_H = 46, GAP = 6, PREVIEW_W = 98;
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
    private static float mid(float h, int size) { return (h - size) / 2f; }
    /** The same for the spaced-out capital labels, which sit lower in their line. */
    private static float midT(float h, int size) { return h / 2f - size * 0.69f; }

    private static final Identifier GEM = Identifier.fromNamespaceAndPath("lumen", "gem");
    private static boolean gemReady;

    /** A brilliant-cut gem seen from the side: the Ruby symbol. Painted once as a white-to-dark texture and tinted when drawn. */
    static void gem(float x, float y, float s, int color) {
        if (!gemReady) {
            gemReady = true;
            int n = 64;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(n, n, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            float l = 0, r = n, cx = n / 2f, top = n * 0.06f, gird = n * 0.36f, tip = n * 0.98f, tl = n * 0.22f, tr = n * 0.78f, il = n * 0.36f, ir = n * 0.64f;
            int base = 0xFF3B55;
            float[][] facets = {{tl, top, il, gird, l, gird}, {tl, top, tr, top, ir, gird, il, gird}, {tr, top, r, gird, ir, gird}, {l, gird, il, gird, cx, tip}, {il, gird, ir, gird, cx, tip}, {ir, gird, r, gird, cx, tip},
                    {tl + n * 0.08f, top + n * 0.07f, tl + n * 0.26f, top + n * 0.07f, tl + n * 0.2f, top + n * 0.17f, tl + n * 0.05f, top + n * 0.17f}};
            int[] colors = {Draw.mix(0xFF000000 | base, 0xFFFFFFFF, 0.22f), Draw.mix(0xFF000000 | base, 0xFFFFFFFF, 0.45f), 0xFF000000 | base, 0xFF000000 | base,
                    Draw.mix(0xFF000000 | base, 0xFF000000, 0.22f), Draw.mix(0xFF000000 | base, 0xFF000000, 0.42f), 0xCCFFFFFF};
            for (int i = 0; i < facets.length; i++) {
                java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
                p.moveTo(facets[i][0], facets[i][1]);
                for (int k = 2; k < facets[i].length; k += 2) p.lineTo(facets[i][k], facets[i][k + 1]);
                p.closePath();
                g.setColor(new java.awt.Color(colors[i], true));
                g.fill(p);
                if (i < 6) { g.setStroke(new java.awt.BasicStroke(0.8f)); g.draw(p); } // closes the hairline gaps between facets
            }
            g.dispose();
            NativeImage out = new NativeImage(n, n, false);
            for (int py = 0; py < n; py++) for (int px = 0; px < n; px++) out.setPixel(px, py, img.getRGB(px, py));
            mc.getTextureManager().register(GEM, new DynamicTexture(() -> "Noctua gem", out));
        }
        Draw.g().pose().pushMatrix();
        Draw.g().pose().translate(x, y);
        Draw.g().pose().scale(s / 64f, s / 64f);
        Draw.g().blit(RenderPipelines.GUI_TEXTURED, GEM, 0, 0, 0f, 0f, 64, 64, 64, 64);
        Draw.g().pose().popMatrix();
    }

    static void opened() { scroll = scrollTo = 0; armed = ""; buying = bought = null; if (Online.status == Online.Status.READY) Online.refresh(); else if (Online.status != Online.Status.SIGNING_IN) Online.signIn(); }
    static void closed() { Online.previewing = false; Online.preview.clear(); buying = bought = null; }

    /** Keys for the purchase window: Esc backs out, Enter confirms. True when the key was used. */
    static boolean key(boolean esc, boolean enter) {
        if (buying == null && bought == null) return false;
        if (esc) { if (!Online.busy) buying = bought = null; }
        else if (enter) { if (bought != null) bought = null; else confirmBuy(); }
        return true;
    }
    static void wheel(float step) { scrollTo += step; }

    private static List<Online.Item> list() {
        List<Online.Item> out = new ArrayList<Online.Item>();
        if (tab > LAST) return out;
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

    private static void dot(float cx, float cy, float r, int color) { Draw.roundRect(cx - r, cy - r, r * 2, r * 2, r, color); }

    /** A check mark about s wide, centred on (cx, cy): an L turned on its corner. */
    private static void tick(float cx, float cy, float s, int color) {
        float t = s * 0.2f;
        Draw.g().pose().pushMatrix();
        Draw.g().pose().translate(cx - s * 0.08f, cy + s * 0.12f);
        Draw.g().pose().rotate((float) Math.toRadians(45));
        Draw.rect(0, -s * 0.62f, t, s * 0.62f + t, color);
        Draw.rect(-s * 0.3f, 0, s * 0.3f + t, t, color);
        Draw.g().pose().popMatrix();
    }

    private static void pill(String text, float x, float y, int color) {
        float w = Draw.uiWidth(text, 5, true) + 8;
        Draw.roundRect(x, y, w, 9, 4, Draw.alpha(color, 0.18f));
        Draw.uiB(text, x + 4, y + mid(9, 5) + 0.6f, 5, color);
    }
    private static float pillW(String text) { return Draw.uiWidth(text, 5, true) + 8; }

    private static void price(int n, float x, float y, int size, int color) {
        gem(x, y - size * 0.06f, size * 0.92f, RUBY);
        Draw.uiB(num(n), x + size * 1.2f, y, size, color);
    }
    private static float priceW(int n, int size) { return size * 1.2f + Draw.uiWidth(num(n), size, true); }

    private static void button(String label, float x, float y, float w, float h, int color, int hover, int text, double mx, double my) {
        Draw.roundRect(x, y, w, h, 6, Draw.in(mx, my, x, y, w, h) ? hover : color);
        Draw.track(label, x + w / 2f, y + midT(h, 7), 7, 1.2f, text);
    }

    static void draw(ModMenuScreen menu, double mx, double my, int x0, int y0, int w, int h) {
        boolean modal = buying != null || bought != null;
        double hx = modal ? -9999 : mx, hy = modal ? -9999 : my; // nothing under the purchase window reacts
        Draw.uiB("Store", x0, y0 + 1, 13, TEXT);
        // top right: Noctua+, then the balance, which is also the way to more Rubies
        String bal = Online.status == Online.Status.READY ? num(Online.rubies) : "-";
        float bw = Draw.uiWidth(bal, 9, true) + 38, bx = x0 + w - bw;
        Draw.roundRect(bx, y0, bw, 18, 6, tab == RUBIES ? Draw.alpha(RUBY, 0.30f) : Draw.in(hx, hy, bx, y0, bw, 18) ? Draw.alpha(RUBY, 0.22f) : Draw.alpha(RUBY, 0.13f));
        gem(bx + 7, y0 + 4.5f, 9, RUBY);
        Draw.uiB(bal, bx + 19, y0 + mid(18, 9), 9, TEXT);
        Draw.uiB("+", bx + bw - 12, y0 + mid(18, 10), 10, MUTED);
        float pw = Draw.uiWidth("Noctua+", 8, true) + 16, pxx = bx - pw - 5;
        Draw.roundRect(pxx, y0, pw, 18, 6, tab == PLUS ? Draw.alpha(GOLD, 0.28f) : Draw.in(hx, hy, pxx, y0, pw, 18) ? Draw.alpha(GOLD, 0.2f) : Draw.alpha(GOLD, 0.1f));
        Draw.uiB("Noctua+", pxx + 8, y0 + mid(18, 8), 8, GOLD);

        float tx = x0, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            float tw = Draw.uiWidth(TABS[i], 8) + 18;
            boolean sel = i == tab, hov = Draw.in(hx, hy, tx, ty, tw, 16);
            if (sel || hov) Draw.roundRect(tx, ty, tw, 16, 6, sel ? Draw.ACCENT : Theme.ctlHover);
            Draw.uiCenter(TABS[i], tx + tw / 2f, ty + mid(16, 8), 8, sel || hov ? TEXT : MUTED);
            tx += tw + 3;
        }
        if (tab <= LAST) { // only what you own
            float ow = 74, ox = x0 + w - ow;
            Draw.ui("Owned only", ox, ty + mid(16, 7), 7, ownedOnly || Draw.in(hx, hy, ox, ty, ow, 16) ? TEXT : MUTED);
            Draw.roundRect(ox + ow - 22, ty + 3, 22, 10, 5, ownedOnly ? Draw.ACCENT : Theme.track);
            dot(ox + ow - (ownedOnly ? 5 : 17), ty + 8, 3.6f, 0xFFFFFFFF);
        }
        int by = y0 + 48, bh = h - 48 - 12;
        hovered = null;

        if (Online.status != Online.Status.READY) {
            boolean failed = Online.status == Online.Status.FAILED;
            Draw.uiCenter(failed ? "The store is not available" : "Connecting to the Noctua servers...", x0 + w / 2f, by + bh / 2f - 22, 10, TEXT);
            if (failed) {
                Draw.text(Online.notice, x0 + w / 2f - mc.font.width(Online.notice) / 2f, by + bh / 2f - 4, MUTED, false);
                boolean hov = Draw.in(mx, my, x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18);
                Draw.roundRect(x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18, 5, hov ? 0xFF6AA0FF : Draw.ACCENT);
                Draw.track("TRY AGAIN", x0 + w / 2f, by + bh / 2f + 14 + midT(18, 7), 7, 1.2f, TEXT);
            }
            return;
        }

        if (tab <= LAST) drawItems(menu, hx, hy, x0, by, w, bh, modal);
        else if (tab == PLUS) drawPlus(hx, hy, x0, by, w, bh);
        else drawRubies(x0, by, w, bh);

        if (!modal && !Online.notice.isEmpty() && !Online.notice.equals("Purchased!")) Draw.text(Online.notice, x0 + (tab <= LAST ? PANEL_W + 8 : 0), y0 + h - 9, Online.notice.endsWith("!") ? GREEN : 0xFFFFB86B, false);
        if (modal) drawBuy(mx, my, x0, y0, w, h);
    }

    /** A small picture of the item for its card: the cape's own face; other items show their initial (the big preview shows them worn). */
    private static void thumb(Online.Item it, float x, float y, float w, float h) {
        if (it.slot.equals("cape")) {
            Identifier tex = dev.lumen.fabric.cosmetics.CapeTextures.thumb(it.id);
            if (tex == null) return;
            float th = h, tw = th * 10f / 16f;
            Draw.g().pose().pushMatrix();
            Draw.g().pose().translate(x + (w - tw) / 2f, y);
            Draw.g().pose().scale(tw / 10f, th / 16f);
            Draw.g().blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 1f, 1f, 10, 16, 64, 32);
            Draw.g().pose().popMatrix();
            return;
        }
        int rc = Online.RARITY_RGB[it.rarity];
        float s = Math.min(w, h);
        Draw.roundRect(x + (w - s) / 2f, y + (h - s) / 2f, s, s, s * 0.28f, Draw.alpha(rc, 0.16f));
        Draw.uiCenterB(it.name.substring(0, 1), x + w / 2f, y + h / 2f - s * 0.23f, s * 0.6f, Draw.alpha(rc, 0.95f));
    }

    private static void drawItems(ModMenuScreen menu, double mx, double my, int x0, int by, int w, int bh, boolean modal) {
        List<Online.Item> list = list();
        Online.Item cur = current(list);
        float gx = x0 + PANEL_W + 8, gw = w - PANEL_W - 8, cw = (gw - (COLS - 1) * TGAP) / COLS;
        int rows = (list.size() + COLS - 1) / COLS;
        float max = Math.max(0, rows * (TH + TGAP) - TGAP - bh);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Draw.lerp(scroll, scrollTo, 0.3f);

        // the grid: a picture, the name and the price; everything else is in the panel
        Draw.scissor(gx - 1, by, gw + 2, bh);
        boolean inView = my >= by && my < by + bh;
        for (int i = 0; i < list.size(); i++) {
            Online.Item it = list.get(i);
            float cx = gx + (i % COLS) * (cw + TGAP), cy = by + (i / COLS) * (TH + TGAP) - scroll;
            if (cy + TH < by || cy > by + bh) continue;
            boolean hov = inView && Draw.in(mx, my, cx, cy, cw, TH), sel = it == cur;
            boolean own = Online.owned.contains(it.id), on = it.id.equals(Online.equipped.get(it.slot));
            if (hov) { hovered = it.id; hoveredSlot = it.slot; }
            int rc = Online.RARITY_RGB[it.rarity];
            Draw.roundRect(cx, cy, cw, TH, 7, sel ? Draw.alpha(Draw.ACCENT, 0.16f) : hov ? Theme.cardHover : Theme.card);
            Draw.roundRect(cx + 7, cy + TH - 2.5f, cw - 14, 2, 1, rc);
            if (sel) Draw.roundOutline(cx, cy, cw, TH, 7, Draw.ACCENT); else if (on) Draw.roundOutline(cx, cy, cw, TH, 7, Draw.alpha(GREEN, 0.8f));
            thumb(it, cx + 4, cy + 5, cw - 8, 30);
            if (it.animated()) dot(cx + 8, cy + 8, 2.2f, GOLD);
            if (own) { dot(cx + cw - 9, cy + 9, 5, on ? GREEN : 0xFF3A4254); tick(cx + cw - 9, cy + 9, 5, 0xFFFFFFFF); }
            Draw.uiCenter(Draw.fit(it.name, 7, cw - 6), cx + cw / 2f, cy + 38.5f, 7, TEXT);
            if (own) Draw.uiCenter(on ? "Wearing" : "Owned", cx + cw / 2f, cy + 48, 6, on ? GREEN : MUTED);
            else if (it.price == 0) Draw.uiCenter("Free", cx + cw / 2f, cy + 48, 6, GREEN);
            else price(it.price, cx + cw / 2f - priceW(it.price, 6) / 2f, cy + 48, 6, Online.rubies >= it.price ? TEXT : 0xFF7C8496);
        }
        Draw.endScissor();
        if (list.isEmpty()) Draw.uiCenter(ownedOnly ? "You do not own any of these yet." : "Nothing here yet.", gx + gw / 2f, by + 30, 8, MUTED);
        if (max > 0) {
            float barH = Math.max(16, bh * bh / (bh + max));
            Draw.roundRect(gx + gw + 2, by + (bh - barH) * (scroll / max), 2.5f, barH, 1.2f, Theme.track);
        }

        // the panel: you, wearing what the mouse is on (or the picked item), and the one button
        float px = x0, pw = PANEL_W;
        Draw.roundRect(px, by, pw, bh, 8, Theme.card);
        Draw.roundRect(px + 1, by + 1, pw - 2, 40, 8, Draw.alpha(Draw.ACCENT, 0.07f));
        Online.Item show = hovered != null ? item(hovered) : cur;
        float infoY = by + bh - 104;
        if (mc.player == null) {
            Draw.uiCenter("Join a world to see it on you", px + pw / 2f, by + (infoY - by) / 2f - 4, 7, MUTED);
        } else if (show != null && !modal) { // the game draws the figure above everything, so not behind the purchase window
            Online.preview.clear();
            Online.preview.put(show.slot, show.id);
            String slot = show.slot;
            float yaw = (slot.equals("hat") ? 0f : slot.equals("shield") ? 115f : 180f) + (float) Math.sin(System.currentTimeMillis() / 1400.0) * 38f;
            drawPlayer(menu, px + 1, by + 2, pw - 2, infoY - by - 4, slot.equals("wings") ? 0.62f : 1f, yaw);
        }
        if (show == null) return;
        int rc = Online.RARITY_RGB[show.rarity];
        pill(Online.RARITY[show.rarity].toUpperCase(), px + 10, infoY, rc);
        if (show.animated()) pill("ANIMATED", px + 14 + pillW(Online.RARITY[show.rarity].toUpperCase()), infoY, GOLD);
        Draw.uiB(Draw.fit(show.name, 12, pw - 20), px + 10, infoY + 15, 12, TEXT);
        boolean own = Online.owned.contains(show.id), on = show.id.equals(Online.equipped.get(show.slot));
        Draw.ui(SLOT_NAME[java.util.Arrays.asList(SLOTS).indexOf(show.slot)] + (on ? "  -  wearing" : own ? "  -  owned" : ""), px + 10, infoY + 32, 7, on ? GREEN : MUTED);
        if (!own) {
            if (show.price == 0) Draw.uiB("Free", px + 10, infoY + 48, 11, GREEN);
            else price(show.price, px + 10, infoY + 48, 11, Online.rubies >= show.price ? TEXT : 0xFFFF7A8A);
        }
        // the button always acts on the picked item, so it gives way to a hint while the mouse is on another card
        if (cur == null) return;
        if (show != cur) { Draw.uiCenter("Click the card to select it", px + pw / 2f, by + bh - 22, 7, MUTED); return; }
        boolean cOwn = Online.owned.contains(cur.id), cOn = cur.id.equals(Online.equipped.get(cur.slot));
        float bx = px + 10, bw = pw - 20, byy = by + bh - 30;
        if (cOn) button("TAKE OFF", bx, byy, bw, 20, Theme.bar, Theme.barHover, TEXT, mx, my);
        else if (cOwn) button("EQUIP", bx, byy, bw, 20, GREEN, 0xFF38D283, 0xFFFFFFFF, mx, my);
        else if (Online.rubies < cur.price) button("NEED " + num(cur.price - Online.rubies) + " MORE", bx, byy, bw, 20, Theme.bar, Theme.barHover, MUTED, mx, my);
        else button(cur.price == 0 ? "GET" : "BUY", bx, byy, bw, 20, Draw.ACCENT, 0xFF6AA0FF, 0xFFFFFFFF, mx, my);
    }

    /** The purchase window's place: x, y, w, h. */
    private static float[] buyBox(int x0, int y0, int w, int h) { return new float[]{x0 + w / 2f - 118, y0 + h / 2f - 78, 236, 156}; }

    private static void drawBuy(double mx, double my, int x0, int y0, int w, int h) {
        // the purchase went through, or was refused
        if (buying != null && buySent && !Online.busy) {
            if (Online.owned.contains(buying.id)) { bought = buying; boughtAt = System.currentTimeMillis(); }
            buying = null; buySent = false;
            if (bought == null) return;
        }
        Draw.roundRect(x0 - 6, y0 - 6, w + 12, h + 12, 8, 0xB0060910);
        float[] b = buyBox(x0, y0, w, h);
        float x = b[0], y = b[1], bw = b[2], bh = b[3];
        Online.Item it = bought != null ? bought : buying;
        int rc = Online.RARITY_RGB[it.rarity];
        float grow = bought != null ? Math.min(1f, (System.currentTimeMillis() - boughtAt) / 260f) : 1f;
        Draw.roundBorder(x, y, bw, bh, 10, 0xFF12172A, Draw.alpha(bought != null ? GREEN : rc, 0.55f));
        Draw.roundRect(x + 14, y + 16, 50, 76, 7, 0xFF0B0F1C);
        thumb(it, x + 16, y + 20, 46, 68);
        float tx = x + 76;
        if (bought != null) {
            dot(tx + 9, y + 25, 9 * grow, GREEN);
            if (grow >= 1f) tick(tx + 9, y + 25, 9, 0xFFFFFFFF);
            Draw.uiB("PURCHASED", tx + 23, y + 22, 8, GREEN);
            Draw.uiB(Draw.fit(it.name, 13, bw - 90), tx, y + 42, 13, TEXT);
            pill(Online.RARITY[it.rarity].toUpperCase(), tx, y + 60, rc);
            Draw.ui("It is yours now.", tx, y + 77, 7, MUTED);
            Draw.ui("Rubies left", tx, y + 91, 7, MUTED);
            price(Online.rubies, x + bw - 14 - priceW(Online.rubies, 8), y + 90.5f, 8, TEXT);
            float half = (bw - 28 - 6) / 2f;
            button("CLOSE", x + 14, y + bh - 34, half, 22, Theme.bar, Theme.barHover, TEXT, mx, my);
            boolean on = it.id.equals(Online.equipped.get(it.slot));
            button(on ? "WEARING" : "EQUIP NOW", x + 14 + half + 6, y + bh - 34, half, 22, GREEN, 0xFF38D283, 0xFFFFFFFF, mx, my);
            return;
        }
        Draw.ui("CONFIRM PURCHASE", tx, y + 18, 6, MUTED);
        Draw.uiB(Draw.fit(it.name, 13, bw - 90), tx, y + 29, 13, TEXT);
        pill(Online.RARITY[it.rarity].toUpperCase(), tx, y + 46, rc);
        if (it.animated()) pill("ANIMATED", tx + 4 + pillW(Online.RARITY[it.rarity].toUpperCase()), y + 46, GOLD);
        String[] rowsL = {"Price", "Your Rubies", "After purchase"};
        int[] rowsV = {it.price, Online.rubies, Online.rubies - it.price};
        for (int i = 0; i < 3; i++) {
            float ry = y + 63.5f + i * 11;
            Draw.ui(rowsL[i], tx, ry, 7, MUTED);
            price(rowsV[i], x + bw - 14 - priceW(rowsV[i], 7), ry, 7, i == 2 ? GREEN : TEXT);
        }
        Draw.rect(tx, y + 96, bw - 90, 1, 0x22FFFFFF);
        float half = (bw - 28 - 6) / 2f;
        boolean wait = buySent;
        button("CANCEL", x + 14, y + bh - 34, half, 22, Theme.bar, Theme.barHover, wait ? MUTED : TEXT, mx, my);
        button(wait ? "BUYING..." : "BUY FOR " + num(it.price), x + 14 + half + 6, y + bh - 34, half, 22, wait ? Theme.bar : Draw.ACCENT, wait ? Theme.bar : 0xFF6AA0FF, 0xFFFFFFFF, mx, my);
        if (!Online.notice.isEmpty() && !wait) Draw.text(Online.notice, x + 14, y + bh - 9.5f, 0xFFFFB86B, false);
    }

    /**
     * Your own player the way the inventory screen draws it, but facing where we say. The outfit is read while the
     * figure is set up, which is why the preview flag is only on for that moment.
     */
    private static void drawPlayer(ModMenuScreen menu, float x, float y, float w, float h, float zoom, float yaw) {
        EntityRenderState st;
        Online.previewing = true;
        try {
            st = mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 1f);
        } catch (Throwable t) {
            return;
        } finally {
            Online.previewing = false;
        }
        st.lightCoords = 0xF000F0;
        st.shadowPieces.clear();
        st.outlineColor = 0;
        if (st instanceof LivingEntityRenderState l) {
            l.bodyRot = 180f + yaw;
            l.yRot = 0f;
            l.xRot = 0f;
            l.boundingBoxWidth /= l.scale;
            l.boundingBoxHeight /= l.scale;
            l.scale = 1f;
        }
        // The figure is drawn by the game outside our scaled panel, so it gets real screen coordinates.
        int x0 = Math.round(menu.sx(x)), y0 = Math.round(menu.sy(y)), x1 = Math.round(menu.sx(x + w)), y1 = Math.round(menu.sy(y + h));
        float size = (y1 - y0) * 0.36f * zoom;
        Quaternionf flip = new Quaternionf().rotateZ((float) Math.PI), tilt = new Quaternionf().rotateX(-8f * 0.017453292f);
        flip.mul(tilt);
        Draw.g().entity(st, size, new Vector3f(0f, st.boundingBoxHeight / 2f + 0.0625f, 0f), flip, tilt, x0, y0, x1, y1);
    }

    private static void drawPlus(double mx, double my, int x0, int by, int w, int bh) {
        Draw.roundRect(x0, by, w, 60, Theme.radius, Theme.card);
        Draw.uiB("Noctua+", x0 + 12, by + 9, 14, GOLD);
        String status = Online.plus ? "Active until " + (Online.plusUntil.length() >= 10 ? Online.plusUntil.substring(0, 10) : Online.plusUntil) : "Not a member";
        Draw.ui(status, x0 + 12, by + 30, 8, Online.plus ? GREEN : MUTED);
        Draw.ui("Coloured name for every Noctua user  -  " + num(Online.dailyRubies) + " Rubies every day", x0 + 12, by + 43, 8, MUTED);

        float y = by + 68;
        if (!Online.plus) {
            Draw.roundRect(x0, y, w, 52, Theme.radius, Theme.card);
            Draw.ui("Noctua+ is given out by the admin.", x0 + 12, y + 9, 9, TEXT);
            if (!Online.plusPriceText.isEmpty()) Draw.text(Online.plusPriceText, x0 + 12, y + 24, GOLD, false);
            Draw.text(Online.contact.isEmpty() ? "Contact the admin to join." : "Contact: " + Online.contact, x0 + 12, y + 37, MUTED, false);
            return;
        }
        // daily
        Draw.roundRect(x0, y, w, 30, Theme.radius, Theme.card);
        Draw.ui("Daily Rubies", x0 + 12, y + mid(30, 9), 9, TEXT);
        boolean can = Online.dailyAvailable && !Online.busy, hov = can && Draw.in(mx, my, x0 + w - 122, y + 6, 112, 18);
        Draw.roundRect(x0 + w - 122, y + 6, 112, 18, 5, can ? (hov ? 0xFF38D283 : GREEN) : Theme.ctl);
        Draw.track(can ? "CLAIM " + num(Online.dailyRubies) : "COME BACK TOMORROW", x0 + w - 66, y + 6 + midT(18, 7), 7, 1f, can ? TEXT : MUTED);
        // colours
        y += 38;
        Draw.roundRect(x0, y, w, 62, Theme.radius, Theme.card);
        Draw.ui("Name colour", x0 + 12, y + 8, 9, TEXT);
        String me = mc.getUser() == null ? "You" : mc.getUser().getName();
        Draw.text(me, x0 + w - 12 - mc.font.width(me), y + 8, Online.nameColor == 0 ? 0xFFFFFFFF : 0xFF000000 | COLOR_RGB[Math.max(0, COLORS.indexOf(Online.nameColor))], true);
        for (int i = 0; i <= 16; i++) {
            float sx = x0 + 12 + i * 17.3f, sy = y + 26;
            boolean sel = i == 16 ? Online.nameColor == 0 : Online.nameColor == COLORS.charAt(i);
            boolean h = Draw.in(mx, my, sx, sy, 14, 14);
            if (i == 16) { Draw.roundRect(sx, sy, 14, 14, 3, Theme.ctl); Draw.uiCenter("x", sx + 7, sy + mid(14, 8) - 0.5f, 8, MUTED); }
            else Draw.roundRect(sx, sy, 14, 14, 3, 0xFF000000 | COLOR_RGB[i]);
            if (sel || h) Draw.roundOutline(sx - 1.5f, sy - 1.5f, 17, 17, 4, sel ? 0xFFFFFFFF : MUTED);
        }
        Draw.ui("Shown on your name tag, in chat and in the tab list for players using Noctua.", x0 + 12, y + 47, 7, MUTED);
    }

    private static void drawRubies(int x0, int by, int w, int bh) {
        Draw.roundRect(x0, by, w, 34, Theme.radius, Theme.card);
        gem(x0 + 11, by + 10, 14, RUBY);
        Draw.uiB("To buy Rubies, contact the admin.", x0 + 32, by + 7, 10, TEXT);
        Draw.text(Online.contact.isEmpty() ? "The admin adds the Rubies to your account by hand." : "Contact: " + Online.contact, x0 + 32, by + 21, MUTED, false);
        float y = by + 42;
        List<int[]> pk = Online.packages;
        int best = pk.isEmpty() ? 0 : pk.get(0)[1] * 1000 / Math.max(1, pk.get(0)[0]);
        for (int i = 0; i < pk.size() && y + 22 <= by + bh; i++) {
            int[] p = pk.get(i);
            Draw.roundRect(x0, y, w, 20, 5, Theme.card);
            gem(x0 + 12, y + 5.5f, 9, RUBY);
            Draw.uiB(num(p[0]) + " Rubies", x0 + 27, y + mid(20, 9), 9, TEXT);
            String price = num(p[1]) + " KRW";
            Draw.uiB(price, x0 + w - 12 - Draw.uiWidth(price, 9, true), y + mid(20, 9), 9, GOLD);
            int per = p[1] * 1000 / Math.max(1, p[0]);
            if (i > 0 && best > per) {
                String save = "-" + Math.round((best - per) * 100f / best) + "%";
                Draw.ui(save, x0 + w - 24 - Draw.uiWidth(price, 9, true) - Draw.uiWidth(save, 8), y + mid(20, 8), 8, GREEN);
            }
            y += 24;
        }
    }

    static void click(double mx, double my, int button, int x0, int y0, int w, int h) {
        if (buying != null || bought != null) { // only the purchase window listens
            float[] b = buyBox(x0, y0, w, h);
            float half = (b[2] - 28 - 6) / 2f, byy = b[1] + b[3] - 34;
            boolean left = Draw.in(mx, my, b[0] + 14, byy, half, 22), right = Draw.in(mx, my, b[0] + 14 + half + 6, byy, half, 22);
            if (bought != null) {
                if (right && !bought.id.equals(Online.equipped.get(bought.slot)) && !Online.busy) Online.equip(bought.slot, bought.id);
                if (left || right || !Draw.in(mx, my, b[0], b[1], b[2], b[3])) bought = null;
            } else if (!buySent) {
                if (right) confirmBuy();
                else if (left || !Draw.in(mx, my, b[0], b[1], b[2], b[3])) buying = null;
            }
            return;
        }
        // Noctua+ and the balance, top right
        String bal = Online.status == Online.Status.READY ? num(Online.rubies) : "-";
        float balW = Draw.uiWidth(bal, 9, true) + 38, balX = x0 + w - balW, plusW = Draw.uiWidth("Noctua+", 8, true) + 16;
        if (Draw.in(mx, my, balX, y0, balW, 18)) { tab = tab == RUBIES ? 0 : RUBIES; scroll = scrollTo = 0; return; }
        if (Draw.in(mx, my, balX - plusW - 5, y0, plusW, 18)) { tab = tab == PLUS ? 0 : PLUS; scroll = scrollTo = 0; return; }
        float tx = x0, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            float tw = Draw.uiWidth(TABS[i], 8) + 18;
            if (Draw.in(mx, my, tx, ty, tw, 16)) { tab = i; scroll = scrollTo = 0; armed = ""; Online.notice = Online.status == Online.Status.READY ? "" : Online.notice; return; }
            tx += tw + 3;
        }
        if (tab <= LAST && Draw.in(mx, my, x0 + w - 74, ty, 74, 16)) { ownedOnly = !ownedOnly; scroll = scrollTo = 0; return; }
        int by = y0 + 48, bh = h - 48 - 12;
        if (Online.status != Online.Status.READY) {
            if (Online.status == Online.Status.FAILED && Draw.in(mx, my, x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18)) Online.signIn();
            return;
        }
        if (Online.busy) return;
        if (tab <= LAST) {
            List<Online.Item> list = list();
            Online.Item cur = current(list);
            float gx = x0 + PANEL_W + 8, gw = w - PANEL_W - 8, cw = (gw - (COLS - 1) * TGAP) / COLS;
            if (my >= by && my < by + bh) for (int i = 0; i < list.size(); i++) {
                if (Draw.in(mx, my, gx + (i % COLS) * (cw + TGAP), by + (i / COLS) * (TH + TGAP) - scroll, cw, TH)) { picked[tab] = list.get(i).id; Online.notice = ""; return; }
            }
            if (cur != null && Draw.in(mx, my, x0 + 10, by + bh - 30, PANEL_W - 20, 20)) {
                boolean own = Online.owned.contains(cur.id), on = cur.id.equals(Online.equipped.get(cur.slot));
                if (on) Online.equip(cur.slot, null);
                else if (own) Online.equip(cur.slot, cur.id);
                else if (Online.rubies < cur.price) { tab = RUBIES; scroll = scrollTo = 0; }
                else { buying = cur; bought = null; buySent = false; Online.notice = ""; }
            }
        } else if (tab == PLUS && Online.plus) {
            float y = by + 68;
            if (Online.dailyAvailable && Draw.in(mx, my, x0 + w - 122, y + 6, 112, 18)) { Online.claimDaily(); return; }
            y += 38;
            for (int i = 0; i <= 16; i++) {
                if (Draw.in(mx, my, x0 + 12 + i * 17.3f, y + 26, 14, 14)) { Online.setColor(i == 16 ? 0 : COLORS.charAt(i)); return; }
            }
        }
    }
}
