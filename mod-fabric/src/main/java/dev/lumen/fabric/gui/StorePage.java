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
    private static final String[] TABS = {"Capes", "Wings", "Hats", "Shields", "Owned", "Noctua+", "Rubies"};
    private static final String[] SLOTS = {"cape", "wings", "hat", "shield"};
    private static final int OWNED = 4, PLUS = 5;
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

    static void opened() { scroll = scrollTo = 0; armed = ""; if (Online.status == Online.Status.READY) Online.refresh(); else if (Online.status != Online.Status.SIGNING_IN) Online.signIn(); }
    static void closed() { Online.previewing = false; Online.preview.clear(); }
    static void wheel(float step) { scrollTo += step; }

    private static List<Online.Item> list() {
        List<Online.Item> out = new ArrayList<Online.Item>();
        if (tab < OWNED) { for (Online.Item i : Online.items) if (i.slot.equals(SLOTS[tab])) out.add(i); }
        else if (tab == OWNED) {
            // everything you own, what you are wearing first
            for (int pass = 0; pass < 2; pass++) for (Online.Item i : Online.items) {
                if (Online.owned.contains(i.id) && i.id.equals(Online.equipped.get(i.slot)) == (pass == 0)) out.add(i);
            }
        }
        return out;
    }

    static void draw(ModMenuScreen menu, double mx, double my, int x0, int y0, int w, int h) {
        Draw.uiB("Store", x0, y0 + 1, 13, TEXT);
        // balance
        String bal = Online.status == Online.Status.READY ? num(Online.rubies) : "-";
        float bw = Draw.uiWidth(bal, 9, true) + 26;
        Draw.roundRect(x0 + w - bw, y0, bw, 18, 6, Theme.card);
        gem(x0 + w - bw + 7, y0 + 4.5f, 9, RUBY);
        Draw.uiB(bal, x0 + w - bw + 19, y0 + mid(18, 9), 9, TEXT);

        float tw = (w - 6 * 3) / 7f, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            float tx = x0 + i * (tw + 3);
            boolean sel = i == tab, hov = Draw.in(mx, my, tx, ty, tw, 16);
            Draw.roundRect(tx, ty, tw, 16, 5, sel ? Draw.ACCENT : hov ? Theme.ctlHover : Theme.card);
            Draw.uiCenter(TABS[i], tx + tw / 2f, ty + mid(16, 8), 8, sel || hov ? TEXT : i == PLUS ? GOLD : MUTED);
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

        if (tab <= OWNED) drawItems(menu, mx, my, x0, by, w, bh);
        else if (tab == PLUS) drawPlus(mx, my, x0, by, w, bh);
        else drawRubies(x0, by, w, bh);

        if (!Online.notice.isEmpty()) Draw.text(Online.notice, x0, y0 + h - 9, Online.notice.endsWith("!") ? GREEN : 0xFFFFB86B, false);
    }

    private static void drawItems(ModMenuScreen menu, double mx, double my, int x0, int by, int w, int bh) {
        List<Online.Item> list = list();
        int lw = w - PREVIEW_W - 8;
        float cw = (lw - GAP) / 2f;
        int rows = (list.size() + 1) / 2;
        float max = Math.max(0, rows * (CARD_H + GAP) - GAP - bh);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Draw.lerp(scroll, scrollTo, 0.3f);
        if (System.currentTimeMillis() - armedAt > 3000) armed = "";

        Draw.scissor(x0 - 1, by, lw + 2, bh);
        boolean inView = my >= by && my < by + bh;
        for (int i = 0; i < list.size(); i++) {
            Online.Item it = list.get(i);
            float cx = x0 + (i % 2) * (cw + GAP), cy = by + (i / 2) * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < by || cy > by + bh) continue;
            boolean hov = inView && Draw.in(mx, my, cx, cy, cw, CARD_H);
            boolean own = Online.owned.contains(it.id), on = it.id.equals(Online.equipped.get(it.slot));
            if (hov) { hovered = it.id; hoveredSlot = it.slot; }
            Draw.roundRect(cx, cy, cw, CARD_H, Theme.radius, hov ? Theme.cardHover : Theme.card);
            if (on) Draw.roundOutline(cx, cy, cw, CARD_H, Theme.radius, Draw.alpha(GREEN, 0.7f));
            Draw.uiCenter(Draw.fit(it.name, 9, cw - 8), cx + cw / 2f, cy + 6, 9, TEXT);
            if (own) Draw.uiCenter(tab == OWNED ? Character.toUpperCase(it.slot.charAt(0)) + it.slot.substring(1) : "Owned", cx + cw / 2f, cy + 19, 7, MUTED);
            else if (it.price == 0) Draw.uiCenter("Free", cx + cw / 2f, cy + 19, 7, GREEN);
            else {
                String p = num(it.price);
                float pw = Draw.uiWidth(p, 8) + 10;
                gem(cx + cw / 2f - pw / 2f, cy + 20.5f, 7.5f, RUBY);
                Draw.ui(p, cx + cw / 2f - pw / 2f + 10, cy + 19, 8, Online.rubies >= it.price ? TEXT : 0xFFFF7A8A);
            }
            float byy = cy + CARD_H - 15;
            boolean bhov = inView && Draw.in(mx, my, cx, byy, cw, 15);
            String label; int color;
            if (on) { label = "EQUIPPED"; color = bhov ? 0xFF38D283 : GREEN; }
            else if (own) { label = "EQUIP"; color = bhov ? Theme.barHover : Theme.bar; }
            else if (Online.rubies < it.price) { label = "NOT ENOUGH"; color = Theme.bar; }
            else if (armed.equals(it.id)) { label = "CONFIRM?"; color = 0xFFE0A030; }
            else { label = "BUY"; color = bhov ? 0xFF6AA0FF : Draw.ACCENT; }
            Draw.roundRect(cx, byy, cw, 15, Theme.radius, color);
            Draw.rect(cx, byy, cw, Math.min(7, Theme.radius + 1), color);
            Draw.track(label, cx + cw / 2f, byy + midT(15, 7), 7, 1.2f, own && !on || label.equals("NOT ENOUGH") ? MUTED : TEXT);
        }
        Draw.endScissor();
        if (max > 0) {
            float barH = Math.max(16, bh * bh / (bh + max));
            Draw.roundRect(x0 + lw + 2, by + (bh - barH) * (scroll / max), 2.5f, barH, 1.2f, Theme.track);
        }

        // preview: your own player, wearing what you point at
        float px = x0 + w - PREVIEW_W, pw = PREVIEW_W;
        Draw.roundRect(px, by, pw, bh, Theme.radius, Theme.card);
        Draw.uiCenter("PREVIEW", px + pw / 2f, by + 6, 7, MUTED);
        if (mc.player == null) {
            Draw.uiCenter("Join a world", px + pw / 2f, by + bh / 2f - 8, 8, MUTED);
            Draw.uiCenter("to see it on you", px + pw / 2f, by + bh / 2f + 2, 8, MUTED);
        } else {
            Online.preview.clear();
            String slot = hovered != null ? hoveredSlot : tab < OWNED ? SLOTS[tab] : "cape";
            if (hovered != null) Online.preview.put(slot, hovered);
            // turn the side that carries the item towards you: hats from the front, shields from the left, the rest from behind
            float yaw = (slot.equals("hat") ? 0f : slot.equals("shield") ? 115f : 180f) + (float) Math.sin(System.currentTimeMillis() / 1400.0) * 38f;
            drawPlayer(menu, px + 1, by + 14, pw - 2, bh - 16, slot.equals("wings") ? 0.62f : 1f, yaw);
        }
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
        float tw = (w - 6 * 3) / 7f, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            if (Draw.in(mx, my, x0 + i * (tw + 3), ty, tw, 16)) { tab = i; scroll = scrollTo = 0; armed = ""; Online.notice = Online.status == Online.Status.READY ? "" : Online.notice; return; }
        }
        int by = y0 + 48, bh = h - 48 - 12;
        if (Online.status != Online.Status.READY) {
            if (Online.status == Online.Status.FAILED && Draw.in(mx, my, x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18)) Online.signIn();
            return;
        }
        if (Online.busy) return;
        if (tab <= OWNED) {
            List<Online.Item> list = list();
            int lw = w - PREVIEW_W - 8;
            float cw = (lw - GAP) / 2f;
            if (my < by || my >= by + bh) return;
            for (int i = 0; i < list.size(); i++) {
                Online.Item it = list.get(i);
                float cx = x0 + (i % 2) * (cw + GAP), cy = by + (i / 2) * (CARD_H + GAP) - scroll;
                if (!Draw.in(mx, my, cx, cy + CARD_H - 15, cw, 15)) continue;
                boolean own = Online.owned.contains(it.id), on = it.id.equals(Online.equipped.get(it.slot));
                if (on) Online.equip(it.slot, null);
                else if (own) Online.equip(it.slot, it.id);
                else if (Online.rubies >= it.price) {
                    // two clicks to spend: the first only arms the button for a few seconds
                    if (armed.equals(it.id)) { armed = ""; Online.buy(it.id); }
                    else { armed = it.id; armedAt = System.currentTimeMillis(); }
                }
                return;
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
