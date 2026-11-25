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
    private static final String[] TABS = {"Capes", "Wings", "Hats", "Shields", "Owned", "Lumen+", "Rubies"};
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

    static void draw(ModMenuScreen menu, int mx, int my, int x0, int y0, int w, int h) {
        Fnt.drawB("Store", x0, y0 + 1, 13, TEXT);
        // balance
        String bal = Online.status == Online.Status.READY ? num(Online.rubies) : "-";
        float bw = Fnt.widthB(bal, 9) + 26;
        R.roundRect(x0 + w - bw, y0, bw, 18, 6, Theme.card);
        gem(x0 + w - bw + 7, y0 + 4.5f, 9, RUBY);
        Fnt.drawB(bal, x0 + w - bw + 19, y0 + mid(18, 9), 9, TEXT);

        float tw = (w - 6 * 3) / 7f, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            float tx = x0 + i * (tw + 3);
            boolean sel = i == tab, hov = R.in(mx, my, tx, ty, tw, 16);
            R.roundRect(tx, ty, tw, 16, 5, sel ? R.ACCENT : hov ? Theme.ctlHover : Theme.card);
            Fnt.center(TABS[i], tx + tw / 2f, ty + mid(16, 8), 8, sel || hov ? TEXT : i == PLUS ? GOLD : MUTED);
        }
        int by = y0 + 48, bh = h - 48 - 12;
        hovered = null;

        if (Online.status != Online.Status.READY) {
            boolean failed = Online.status == Online.Status.FAILED;
            Fnt.center(failed ? "The store is not available" : "Connecting to the Lumen servers...", x0 + w / 2f, by + bh / 2f - 22, 10, TEXT);
            if (failed) {
                R.mcText(Online.notice, x0 + w / 2f - mc.fontRendererObj.getStringWidth(Online.notice) / 2f, by + bh / 2f - 4, MUTED, false);
                boolean hov = R.in(mx, my, x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18);
                R.roundRect(x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18, 5, hov ? 0xFF6AA0FF : R.ACCENT);
                Fnt.track("TRY AGAIN", x0 + w / 2f, by + bh / 2f + 14 + mid(18, 7), 7, Fnt.BOLD, 1.2f, TEXT);
            }
            return;
        }

        if (tab <= OWNED) drawItems(menu, mx, my, x0, by, w, bh);
        else if (tab == PLUS) drawPlus(mx, my, x0, by, w, bh);
        else drawRubies(x0, by, w, bh);

        if (!Online.notice.isEmpty()) R.mcText(Online.notice, x0, y0 + h - 9, Online.notice.endsWith("!") ? GREEN : 0xFFFFB86B, false);
    }

    private static void drawItems(ModMenuScreen menu, int mx, int my, int x0, int by, int w, int bh) {
        List<Online.Item> list = list();
        int lw = w - PREVIEW_W - 8;
        float cw = (lw - GAP) / 2f;
        int rows = (list.size() + 1) / 2;
        float max = Math.max(0, rows * (CARD_H + GAP) - GAP - bh);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Util.lerp(scroll, scrollTo, 0.3f);
        if (System.currentTimeMillis() - armedAt > 3000) armed = "";

        menu.clip(x0 - 1, by, lw + 2, bh);
        boolean inView = my >= by && my < by + bh;
        for (int i = 0; i < list.size(); i++) {
            Online.Item it = list.get(i);
            float cx = x0 + (i % 2) * (cw + GAP), cy = by + (i / 2) * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < by || cy > by + bh) continue;
            boolean hov = inView && R.in(mx, my, cx, cy, cw, CARD_H);
            boolean own = Online.owned.contains(it.id), on = it.id.equals(Online.equipped.get(it.slot));
            if (hov) { hovered = it.id; hoveredSlot = it.slot; }
            R.roundRect(cx, cy, cw, CARD_H, Theme.radius, hov ? Theme.cardHover : Theme.card);
            if (on) R.roundOutline(cx, cy, cw, CARD_H, Theme.radius, 1f, R.alpha(GREEN, 0.7f));
            Fnt.center(Fnt.fit(it.name, 9, cw - 8), cx + cw / 2f, cy + 6, 9, TEXT);
            if (own) Fnt.center(tab == OWNED ? Character.toUpperCase(it.slot.charAt(0)) + it.slot.substring(1) : "Owned", cx + cw / 2f, cy + 19, 7, MUTED);
            else if (it.price == 0) Fnt.center("Free", cx + cw / 2f, cy + 19, 7, GREEN);
            else {
                String p = num(it.price);
                float pw = Fnt.width(p, 8) + 10;
                gem(cx + cw / 2f - pw / 2f, cy + 20.5f, 7.5f, RUBY);
                Fnt.draw(p, cx + cw / 2f - pw / 2f + 10, cy + 19, 8, Online.rubies >= it.price ? TEXT : 0xFFFF7A8A);
            }
            float byy = cy + CARD_H - 15;
            boolean bhov = inView && R.in(mx, my, cx, byy, cw, 15);
            String label; int color;
            if (on) { label = "EQUIPPED"; color = bhov ? 0xFF38D283 : GREEN; }
            else if (own) { label = "EQUIP"; color = bhov ? Theme.barHover : Theme.bar; }
            else if (Online.rubies < it.price) { label = "NOT ENOUGH"; color = Theme.bar; }
            else if (armed.equals(it.id)) { label = "CONFIRM?"; color = 0xFFE0A030; }
            else { label = "BUY"; color = bhov ? 0xFF6AA0FF : R.ACCENT; }
            R.roundRect(cx, byy, cw, 15, Theme.radius, color);
            R.rect(cx, byy, cw, Math.min(7, Theme.radius + 1), color);
            Fnt.track(label, cx + cw / 2f, byy + mid(15, 7), 7, Fnt.BOLD, 1.2f, own && !on || label.equals("NOT ENOUGH") ? MUTED : TEXT);
        }
        R.endScissor();
        if (max > 0) {
            float barH = Math.max(16, bh * bh / (bh + max));
            R.roundRect(x0 + lw + 2, by + (bh - barH) * (scroll / max), 2.5f, barH, 1.2f, Theme.track);
        }

        // preview: your own player, wearing what you point at
        float px = x0 + w - PREVIEW_W, pw = PREVIEW_W;
        R.roundRect(px, by, pw, bh, Theme.radius, Theme.card);
        Fnt.center("PREVIEW", px + pw / 2f, by + 6, 7, MUTED);
        if (mc.thePlayer == null) {
            Fnt.center("Join a world", px + pw / 2f, by + bh / 2f - 8, 8, MUTED);
            Fnt.center("to see it on you", px + pw / 2f, by + bh / 2f + 2, 8, MUTED);
        } else {
            Online.preview.clear();
            String slot = hovered != null ? hoveredSlot : tab < OWNED ? SLOTS[tab] : "cape";
            if (hovered != null) Online.preview.put(slot, hovered);
            // turn the side that carries the item towards you: hats from the front, shields from the left, the rest from behind
            float yaw = (slot.equals("hat") ? 0f : slot.equals("shield") ? 115f : 180f) + (float) Math.sin(System.currentTimeMillis() / 1400.0) * 38f;
            menu.clip(px + 1, by + 14, pw - 2, bh - 16);
            Online.previewing = true;
            try { drawPlayer(px + pw / 2f, by + bh - 34, slot.equals("wings") ? 30 : slot.equals("hat") ? 58 : 62, yaw); } finally { Online.previewing = false; }
            R.endScissor();
            R.reset();
        }
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
        Fnt.draw("Lumen+", x0 + 12, by + 9, 14, Fnt.BOLD, 0, GOLD, 0xFFFF8A3D);
        String status = Online.plus ? "Active until " + (Online.plusUntil.length() >= 10 ? Online.plusUntil.substring(0, 10) : Online.plusUntil) : "Not a member";
        Fnt.draw(status, x0 + 12, by + 30, 8, Online.plus ? GREEN : MUTED);
        Fnt.draw("Coloured name for every Lumen user  -  " + num(Online.dailyRubies) + " Rubies every day", x0 + 12, by + 43, 8, MUTED);

        float y = by + 68;
        if (!Online.plus) {
            R.roundRect(x0, y, w, 52, Theme.radius, Theme.card);
            Fnt.draw("Lumen+ is given out by the admin.", x0 + 12, y + 9, 9, TEXT);
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
        Fnt.draw("Shown on your name tag, in chat and in the tab list for players using Lumen.", x0 + 12, y + 47, 7, MUTED);
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
        float tw = (w - 6 * 3) / 7f, ty = y0 + 26;
        for (int i = 0; i < TABS.length; i++) {
            if (R.in(mx, my, x0 + i * (tw + 3), ty, tw, 16)) { tab = i; scroll = scrollTo = 0; armed = ""; Online.notice = Online.status == Online.Status.READY ? "" : Online.notice; return; }
        }
        int by = y0 + 48, bh = h - 48 - 12;
        if (Online.status != Online.Status.READY) {
            if (Online.status == Online.Status.FAILED && R.in(mx, my, x0 + w / 2f - 40, by + bh / 2f + 14, 80, 18)) Online.signIn();
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
                if (!R.in(mx, my, cx, cy + CARD_H - 15, cw, 15)) continue;
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
            if (Online.dailyAvailable && R.in(mx, my, x0 + w - 122, y + 6, 112, 18)) { Online.claimDaily(); return; }
            y += 38;
            for (int i = 0; i <= 16; i++) {
                if (R.in(mx, my, x0 + 12 + i * 17.3f, y + 26, 14, 14)) { Online.setColor(i == 16 ? 0 : COLORS.charAt(i)); return; }
            }
        }
    }
}
