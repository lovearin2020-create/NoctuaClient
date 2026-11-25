package dev.lumen.gui;

import dev.lumen.core.Client;

import dev.lumen.Lumen;
import dev.lumen.core.Category;
import dev.lumen.core.Config;
import dev.lumen.core.HudModule;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The RSHIFT mod menu: category sidebar, searchable module grid and per-module settings. */
public class ModMenuScreen extends GuiScreen {
    private static final int W = 600, H = 330, SIDE = 98, SETW = 190, CARD_H = 44, GAP = 11, ROW = 26, LINE = 20;
    private static final net.minecraft.util.ResourceLocation MOON = new net.minecraft.util.ResourceLocation("lumen", "icon/moon.png");
    private static final int TEXT = 0xFFF2F4F8, MUTED = 0xFF8891A5, GREEN = 0xFF2FBF71, OFF = 0xFF2B303B, RED = 0xFFB0344E;

    private static Category category; // null = all mods
    private Module open;
    /** For screenshots and tests: opens with this mod's settings showing. */
    public ModMenuScreen select(Module m) { open = m; return this; }
    private boolean rowHit;
    /** Opens straight on the Store. */
    public ModMenuScreen openStore() { store = true; StorePage.opened(); return this; }
    private String search = "";
    private boolean searching;
    private float scroll, scrollTo, settingsScroll, settingsScrollTo;
    private Setting.Num dragging;
    private Setting.Text editing;
    /** The key setting that is waiting for a key press. */
    private Setting.Key binding;
    private boolean profiles;
    /** The Store tab is showing. */
    private boolean store;
    private final Setting.Text profileName = new Setting.Text("Profile name", "", 24);
    private String profileNote = "";
    private float dragX, dragW;
    private final long opened = System.currentTimeMillis();
    private final Map<Object, Float> anim = new HashMap<Object, Float>();

    private int px, py;

    public void initGui() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        Keyboard.enableRepeatEvents(true);
        Client.sync();
        Theme.apply();
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        StorePage.closed();
        Config.save();
    }

    public boolean doesGuiPauseGame() { return false; }

    private float anim(Object key, float target) {
        Float cur = anim.get(key);
        float v = Util.lerp(cur == null ? target : cur, target, 0.25f);
        anim.put(key, v);
        return v;
    }

    private List<Module> visible() {
        List<Module> out = new ArrayList<Module>();
        String q = search.toLowerCase();
        for (Module m : Lumen.modules.all) {
            if (m.hidden) continue;
            if (q.isEmpty() ? (category == null || m.category == category) : m.name.toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    // ---------------------------------------------------------------- drawing

    /** Shrinks the panel on small windows / large GUI scales so it always fits on screen. */
    private float fit() { return Math.min(MAX_FIT * Client.menuSize.f() / 100f, Math.min((width - 16) / (float) W, (height - 16) / (float) H)); }
    /** The panel grows to fill most of the window, up to this factor (GUI scale 2 at 1080p reaches it). */
    private static final float MAX_FIT = 1.6f;
    private int tx(int mx) { return Math.round((mx - width / 2f) / fit() + width / 2f); }
    private int ty(int my) { return Math.round((my - height / 2f) / fit() + height / 2f); }

    /** 0..1..0 over time for sliding long names: pauses at both ends, speed independent of how far it has to travel. */
    private static float slide(float distance) {
        float travel = Math.max(0.6f, distance / 14f), pause = 1.2f, cycle = 2 * (travel + pause);
        float t = (System.currentTimeMillis() % (long) (cycle * 1000)) / 1000f;
        float p = t < pause ? 0 : t < pause + travel ? (t - pause) / travel : t < 2 * pause + travel ? 1 : 1 - (t - 2 * pause - travel) / travel;
        return p * p * (3 - 2 * p);
    }

    void clip(float x, float y, float w, float h) {
        float f = fit(), cx = width / 2f, cy = height / 2f;
        R.scissor((x - cx) * f + cx, (y - cy) * f + cy, w * f, h * f);
    }

    public void drawScreen(int mx, int my, float pt) {
        R.square = Client.corners.is("Square");
        try { drawMenu(mx, my, pt); } finally { R.square = false; }
    }

    private void drawMenu(int mx, int my, float pt) {
        mx = tx(mx); my = ty(my);
        Fnt.transform(fit(), width / 2f, height / 2f);
        float t = Math.min(1f, (System.currentTimeMillis() - opened) / 160f);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t);
        if (mc.theWorld == null) { Fnt.transform(1f, 0, 0); Backdrop.draw(width, height, mx, my); Fnt.transform(fit(), width / 2f, height / 2f); }
        R.rect(0, 0, width, height, R.alpha(mc.theWorld == null ? 0x55000000 : 0x60060910, ease));

        GlStateManager.pushMatrix();
        float s = (0.94f + 0.06f * ease) * fit();
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(s, s, 1);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);

        float pr = 9;
        R.roundRect(px, py, W, H, pr, 0x78000000);
        R.gradient(px + 6, py + 1, W - 12, 12, 0x12FFFFFF, 0x00FFFFFF);
        clip(px, py, SIDE, H); // one extra layer per pane, so no seam where the pieces would overlap
        R.roundRect(px, py, SIDE + 30, H, pr, 0x30000000);
        R.endScissor();
        if (!store && !profiles) {
            clip(px + W - SETW, py, SETW, H);
            R.roundRect(px + W - SETW - 30, py, SETW + 30, H, pr, 0x40000000);
            R.endScissor();
            R.rect(px + W - SETW, py, 1, H, 0x16FFFFFF);
        }
        R.rect(px + SIDE, py, 1, H, 0x16FFFFFF);
        R.roundOutline(px, py, W, H, pr, 1f, 0x1EFFFFFF);

        drawSidebar(mx, my);
        if (store) StorePage.draw(this, mx, my, px + SIDE + 14, py + 14, W - SIDE - 28, H - 28);
        else if (profiles) drawProfiles(mx, my); else { drawGrid(mx, my); drawSettings(mx, my); }
        GlStateManager.popMatrix();
        R.reset();
    }

    private void moon(float x, float y, float size) {
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(MOON);
        org.lwjgl.opengl.GL11.glTexParameteri(3553, 10241, 9729);
        org.lwjgl.opengl.GL11.glTexParameteri(3553, 10240, 9729);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, (int) size, (int) size, size, size);
        GlStateManager.popMatrix();
    }

    private static final int CAT_Y = 44, CAT_H = 20;

    private void drawSidebar(int mx, int my) {
        moon(px + 12, py + 12, 16);
        Fnt.draw("NOCTUA", px + 31, py + 15f, 10, Fnt.LOGO, 1.2f, 0xFFFFFFFF, 0xFFFFFFFF);

        int y = py + CAT_Y;
        for (int i = -1; i < Category.values().length; i++) {
            Category c = i < 0 ? null : Category.values()[i];
            if (c != null && Lumen.modules.of(c).isEmpty()) continue; // e.g. Cosmetics, which moved to the Store
            boolean sel = !profiles && !store && search.isEmpty() && category == c;
            boolean hov = R.in(mx, my, px, y, SIDE, CAT_H);
            float a = anim("cat" + i, sel ? 1f : hov ? 0.35f : 0f);
            if (a > 0.01f) R.rect(px, y, SIDE, CAT_H, R.alpha(R.ACCENT, a * 0.18f));
            if (sel) R.roundRect(px, y + 4, 2, CAT_H - 8, 1, R.ACCENT);
            Fnt.draw((c == null ? "All" : c.label).toUpperCase(), px + 13, y + 6.5f, 7, Fnt.REGULAR, 0.6f, sel ? 0xFFFFFFFF : R.mix(MUTED, TEXT, a * 2), sel ? 0xFFFFFFFF : R.mix(MUTED, TEXT, a * 2));
            String n = String.valueOf(Lumen.modules.of(c).size());
            Fnt.draw(n, px + SIDE - 12 - Fnt.width(n, 6), y + 7, 6, 0xFF6F778A);
            y += CAT_H;
        }

        boolean sh = R.in(mx, my, px + 9, py + H - 64, SIDE - 18, 19);
        float sta = anim("storeTab", store ? 1f : sh ? 0.5f : 0f);
        R.roundRect(px + 9, py + H - 64, SIDE - 18, 19, 5, R.mix(0x29E5484D, 0x66E5484D, sta));
        StorePage.gem(px + 24, py + H - 59, 9, 0xFFFF3B55);
        Fnt.track("STORE", px + SIDE / 2f + 6, py + H - 58f, 7, Fnt.BOLD, 1.0f, 0xFFFF8F93);
        boolean ph = R.in(mx, my, px + 9, py + H - 41, SIDE - 18, 19);
        float sa = anim("settingsTab", profiles && !store ? 1f : ph ? 0.5f : 0f);
        R.roundRect(px + 9, py + H - 41, SIDE - 18, 19, 5, R.mix(0x40000000, R.alpha(R.ACCENT, 0.9f), sa));
        Fnt.track("OPTIONS", px + SIDE / 2f, py + H - 35f, 7, Fnt.BOLD, 1.0f, profiles && !store || ph ? TEXT : MUTED);
        String ver = "Noctua Client " + Lumen.VERSION;
        Fnt.center(ver, px + SIDE / 2f, py + H - 16, 6, 0xFF6F778A);
    }

    /** Breaks a description into at most {@code lines} lines that fit the width. */
    private static List<String> wrap(String text, int size, float w, int lines) {
        List<String> out = new ArrayList<String>();
        String cur = "";
        for (String word : text.split(" ")) {
            String next = cur.isEmpty() ? word : cur + " " + word;
            if (Fnt.width(next, size) <= w || cur.isEmpty()) cur = next;
            else { out.add(cur); cur = word; if (out.size() == lines) break; }
        }
        if (out.size() < lines && !cur.isEmpty()) out.add(cur);
        else if (out.size() == lines && !cur.isEmpty()) out.set(lines - 1, Fnt.fit(out.get(lines - 1) + " " + cur, size, w));
        return out;
    }

    private void toggle(float x, float y, float a) {
        R.roundRect(x, y, 18, 10, 5, R.mix(0x55000000, R.ACCENT, a));
        if (a < 0.5f) R.roundOutline(x, y, 18, 10, 5, 1f, 0x24FFFFFF);
        R.circle(x + 5 + a * 8, y + 5, 3.4f, R.mix(0xFFAAB2C5, 0xFFFFFFFF, a));
    }

    private int gridX() { return px + SIDE + 12; }
    private int gridW() { return W - SIDE - SETW - 24; }
    private int gridTop() { return py + 12 + 44; }
    private int gridH() { return H - 12 - 44 - 10; }

    private void drawGrid(int mx, int my) {
        int x0 = gridX(), y0 = py + 12, w = gridW();
        // search box and the HUD editor button
        int bw = 62, sw = w - bw - 6;
        R.roundRect(x0, y0, sw, 18, 5, 0x40000000);
        R.roundOutline(x0, y0, sw, 18, 5, 1f, searching ? R.ACCENT : 0x1CFFFFFF);
        String shown = search.isEmpty() && !searching ? "Search mods..." : Fnt.fit(search, 8, sw - 18) + (searching && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        Fnt.draw(shown, x0 + 8, y0 + 5f, 8, search.isEmpty() && !searching ? 0xFF6F778A : TEXT);
        boolean eh = R.in(mx, my, x0 + w - bw, y0, bw, 18);
        R.roundRect(x0 + w - bw, y0, bw, 18, 5, eh ? 0xFF6AA0FF : R.alpha(R.ACCENT, 0.9f));
        Fnt.track("EDIT HUD", x0 + w - bw / 2f, y0 + 5.5f, 7, Fnt.BOLD, 1.0f, 0xFFFFFFFF);

        List<Module> list = visible();
        int on = 0;
        for (Module m : list) if (m.isEnabled()) on++;
        String title = (search.isEmpty() ? (category == null ? "All mods" : category.label) : "Search").toUpperCase();
        Fnt.draw(title, x0 + 1, y0 + 27, 6, Fnt.REGULAR, 0.6f, MUTED, MUTED);
        String count = (on + " of " + list.size() + " enabled").toUpperCase();
        Fnt.draw(count, x0 + w - 1 - Fnt.width(count, 6, Fnt.REGULAR, 0.6f), y0 + 27, 6, Fnt.REGULAR, 0.6f, MUTED, MUTED);
        R.rect(x0, y0 + 37, w, 1, R.alpha(R.ACCENT, 0.75f));

        int top = gridTop(), viewH = gridH();
        float cw = (w - GAP * 2) / 3f;
        int rows = (list.size() + 2) / 3;
        float max = Math.max(0, rows * (CARD_H + GAP) - GAP - viewH);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Util.lerp(scroll, scrollTo, 0.3f);
        if (list.isEmpty()) Fnt.center("No mods found", x0 + w / 2f, top + 40, 9, MUTED);

        clip(x0 - 2, top, w + 4, viewH);
        boolean inView = my >= top && my < top + viewH;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < top || cy > top + viewH) continue;
            boolean hov = inView && R.in(mx, my, cx, cy, cw, CARD_H), sel = open == m;
            float h = anim(m, hov ? 1 : 0);
            R.roundRect(cx, cy, cw, CARD_H, 6, sel ? R.mix(0x48000000, R.alpha(R.ACCENT, 0.5f), 0.45f) : R.mix(0x40000000, 0x66000000, h));
            R.roundOutline(cx, cy, cw, CARD_H, 6, 1f, sel ? R.alpha(R.ACCENT, 0.9f) : R.mix(0x1CFFFFFF, 0x38FFFFFF, h));
            float room = cw - 34, titleW = Fnt.widthB(m.name, 8);
            if (titleW <= room) Fnt.drawB(m.name, cx + 7, cy + 6, 8, TEXT);
            else {
                // Too long for the card: slide it left and right inside the card instead of cutting it off.
                float clipTop = Math.max(cy, top), clipBottom = Math.min(cy + 17, top + viewH);
                if (clipBottom > clipTop) {
                    clip(cx + 7, clipTop, room, clipBottom - clipTop);
                    Fnt.drawB(m.name, cx + 7 - (titleW - room) * slide(titleW - room), cy + 6, 8, TEXT);
                    clip(x0 - 2, top, w + 4, viewH);
                }
            }
            List<String> desc = wrap(m.description, 6, cw - 14, 2);
            for (int k = 0; k < desc.size(); k++) Fnt.draw(desc.get(k), cx + 7, cy + 18.5f + k * 8, 6, MUTED);
            toggle(cx + cw - 25, cy + 6, anim(m.name + "#on", m.isEnabled() ? 1 : 0));
        }
        R.endScissor();
        if (max > 0) {
            float barH = Math.max(18, viewH * (float) viewH / (viewH + max));
            R.roundRect(x0 + w + 4, top + (viewH - barH) * (scroll / max), 2, barH, 1f, 0x40FFFFFF);
        }
    }

    /** Height of the client settings block at the top of the Settings tab. */
    private int clientH() { return 26 + 4 + Client.shown.size() * LINE + 10; }

    private void drawProfiles(int mx, int my) {
        int x0 = px + SIDE + 14, w = W - SIDE - 28, top = py + 14 + 26, viewH = H - 14 - 26 - 8;
        Fnt.drawB("Settings", x0, py + 15, 13, TEXT);
        java.util.List<String> list = Config.profiles();
        float content = clientH() - 26 + 62 + Math.max(1, list.size()) * ROW + 30;
        float max = Math.max(0, content - viewH);
        settingsScrollTo = Math.max(0, Math.min(max, settingsScrollTo));
        settingsScroll = Util.lerp(settingsScroll, settingsScrollTo, 0.3f);
        if (dragging != null) {
            if (!Mouse.isButtonDown(0)) dragging = null;
            else { dragging.set(dragging.min + (dragging.max - dragging.min) * Math.max(0, Math.min(1, (mx - dragX) / dragW))); Client.apply(); }
        }
        clip(x0 - 2, top, w + 4, viewH);
        boolean inView = my >= top && my < top + viewH;
        if (!inView) { mx = -9999; }
        drawRows(Client.shown, x0, top + 4 - settingsScroll, w, mx, my);

        int y0 = py + 14 + clientH() - Math.round(settingsScroll);
        Fnt.drawB("Profiles", x0, y0 + 2, 11, TEXT);
        Fnt.draw("Each profile stores every mod's on/off state, settings and HUD layout.", x0, y0 + 24, 8, MUTED);

        // new profile row
        float ry = y0 + 40;
        R.roundRect(x0, ry, w, ROW - 4, 5, Theme.card);
        boolean focus = editing == profileName;
        R.roundRect(x0 + 8, ry + 3, 170, 16, 4, Theme.ctl);
        R.roundOutline(x0 + 8, ry + 3, 170, 16, 4, 1f, focus ? R.ACCENT : Theme.outline);
        String shown = profileName.value + (focus && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        if (shown.isEmpty()) Fnt.draw("New profile name...", x0 + 14, ry + 7, 8, 0xFF8089A0); else Fnt.draw(shown, x0 + 14, ry + 7, 8, TEXT);
        boolean sh = R.in(mx, my, x0 + w - 118, ry + 3, 110, 16);
        R.roundRect(x0 + w - 118, ry + 3, 110, 16, 4, sh ? 0xFF6AA0FF : R.ACCENT);
        Fnt.center("Save Current Settings", x0 + w - 63, ry + 7, 8, 0xFFFFFFFF);
        if (!profileNote.isEmpty()) Fnt.draw(profileNote, x0 + 186, ry + 7, 8, MUTED);

        float y = ry + ROW + 4;
        if (list.isEmpty()) Fnt.center("No profiles yet. Type a name above and press Save.", x0 + w / 2f, y + 8, 9, MUTED);
        for (String name : list) {
            boolean active = name.equals(Config.activeProfile);
            R.roundRect(x0, y, w, ROW - 4, 5, Theme.card);
            if (active) R.roundOutline(x0, y, w, ROW - 4, 5, 1f, R.alpha(GREEN, 0.6f));
            Fnt.draw(name, x0 + 10, y + 6.5f, 9, TEXT);
            if (active) Fnt.draw("ACTIVE", x0 + 16 + Fnt.width(name, 9), y + 7.5f, 7, GREEN);
            String[] labels = {"Load", "Overwrite", "Delete"};
            float bx = x0 + w - 8;
            for (int i = labels.length - 1; i >= 0; i--) {
                float bw = i == 1 ? 58 : 44;
                bx -= bw;
                boolean hov = R.in(mx, my, bx, y + 3, bw, 16);
                R.roundRect(bx, y + 3, bw, 16, 4, hov ? (i == 2 ? 0xFFE0526A : i == 0 ? R.ACCENT : Theme.ctlHover) : Theme.ctl);
                Fnt.center(labels[i], bx + bw / 2f, y + 7, 8, TEXT);
                bx -= 4;
            }
            y += ROW;
        }
        R.endScissor();
        if (max > 0) {
            float barH = Math.max(18, viewH * viewH / (viewH + max));
            R.roundRect(px + W - 8, top + (viewH - barH) * (settingsScroll / max), 3, barH, 1.5f, Theme.track);
        }
    }

    private boolean clickProfiles(int mx, int my) {
        int x0 = px + SIDE + 14, y0 = py + 14 + clientH() - Math.round(settingsScroll), w = W - SIDE - 28;
        float ry = y0 + 40;
        if (R.in(mx, my, x0 + 8, ry + 3, 170, 16)) { editing = profileName; return true; }
        if (R.in(mx, my, x0 + w - 118, ry + 3, 110, 16)) {
            if (Config.saveProfile(profileName.value)) { profileNote = "Saved."; profileName.value = ""; } else profileNote = "Enter a name first.";
            return true;
        }
        float y = ry + ROW + 4;
        for (String name : Config.profiles()) {
            float bx = x0 + w - 8;
            for (int i = 2; i >= 0; i--) {
                float bw = i == 1 ? 58 : 44;
                bx -= bw;
                if (R.in(mx, my, bx, y + 3, bw, 16)) {
                    if (i == 0) { Config.loadProfile(name); profileNote = "Loaded " + name + "."; }
                    else if (i == 1) { Config.saveProfile(name); profileNote = "Updated " + name + "."; }
                    else { Config.deleteProfile(name); profileNote = "Deleted " + name + "."; }
                    return true;
                }
                bx -= 4;
            }
            y += ROW;
        }
        return false;
    }

    private int setX() { return px + W - SETW + 12; }
    private int setW() { return SETW - 24; }
    private int setTop() { return py + 12 + 46; }
    private int setH() { return H - 12 - 46 - 10; }

    private void drawSettings(int mx, int my) {
        int x0 = setX(), y0 = py + 12, w = setW();
        if (open == null) {
            Fnt.center("Select a mod", x0 + w / 2f, py + H / 2f - 12, 9, TEXT);
            Fnt.center("to see its settings here.", x0 + w / 2f, py + H / 2f + 1, 7, MUTED);
            return;
        }
        Fnt.drawB(Fnt.fit(open.name, 11, w - 26), x0, y0 + 1, 11, TEXT);
        toggle(x0 + w - 18, y0 + 3, anim(open.name + "#on", open.isEnabled() ? 1 : 0));
        List<String> desc = wrap(open.description, 6, w, 2);
        for (int k = 0; k < desc.size(); k++) Fnt.draw(desc.get(k), x0, y0 + 18 + k * 8, 6, MUTED);
        R.rect(x0, y0 + 39, w, 1, R.alpha(R.ACCENT, 0.75f));

        int top = setTop(), viewH = setH();
        if (dragging != null) {
            if (!Mouse.isButtonDown(0)) dragging = null;
            else dragging.set(dragging.min + (dragging.max - dragging.min) * Math.max(0, Math.min(1, (mx - dragX) / dragW)));
        }
        clip(x0 - 2, top, w + 4, viewH);
        boolean inView = my >= top && my < top + viewH;
        float y = rows(open.settings, x0, top - settingsScroll, w, inView ? mx : -9999, my, -1);
        if (open instanceof HudModule) {
            Fnt.draw("Position & size", x0, y + 6, 8, TEXT);
            boolean hov = inView && R.in(mx, my, x0 + w - 66, y + 3, 66, 14);
            R.roundRect(x0 + w - 66, y + 3, 66, 14, 4, hov ? R.ACCENT : 0x40000000);
            Fnt.center("HUD Editor", x0 + w - 33, y + 6.5f, 7, TEXT);
            y += LINE;
        }
        R.endScissor();
        float max = Math.max(0, y + settingsScroll - top - viewH + 4);
        settingsScrollTo = Math.max(0, Math.min(max, settingsScrollTo));
        settingsScroll = Util.lerp(settingsScroll, settingsScrollTo, 0.3f);
        if (max > 0) {
            float barH = Math.max(18, viewH * (float) viewH / (viewH + max));
            R.roundRect(px + W - 6, top + (viewH - barH) * (settingsScroll / max), 2, barH, 1f, 0x40FFFFFF);
        }
    }

    private static float segments(Setting.Mode m) { float t = 4; for (String o : m.options) t += Fnt.width(o, 6) + 9; return t; }

    /** The settings whose names share a "Group: " prefix with this one, as one block of chips. */
    private static String group(Setting st) {
        int i = st.name.indexOf(": ");
        return st instanceof Setting.Bool && i > 0 ? st.name.substring(0, i) : null;
    }

    private float drawRows(List<? extends Setting> list, int x0, float y, int w, int mx, int my) { return rows(list, x0, y, w, mx, my, -1); }
    private boolean clickRows(List<? extends Setting> list, int x0, float sy, int w, int mx, int my, int button) { rowHit = false; rows(list, x0, sy, w, mx, my, button); return rowHit; }

    /**
     * One pass over the settings that either draws them (button -1) or applies a click, so both always agree on where
     * things are. @return the y below the last row.
     */
    private float rows(List<? extends Setting> list, float x0, float y, float w, int mx, int my, int button) {
        boolean click = button >= 0, paint = button == -1;
        float rx = x0 + w;
        for (int idx = 0; idx < list.size(); idx++) {
            Setting st = list.get(idx);
            String g = group(st);
            if (g != null) {
                // a run of on/off switches of one group: a heading and chips
                if (paint) Fnt.draw(g.toUpperCase() + "S", x0, y + 6, 6, Fnt.REGULAR, 0.6f, MUTED, MUTED);
                float cx = x0, cy = y + 17;
                for (; idx < list.size() && g.equals(group(list.get(idx))); idx++) {
                    Setting.Bool b = (Setting.Bool) list.get(idx);
                    String label = b.name.substring(g.length() + 2);
                    float cw = Fnt.width(label, 6) + 10;
                    if (cx + cw > rx && cx > x0) { cx = x0; cy += 15; }
                    if (click) { if (R.in(mx, my, cx, cy, cw, 12)) { b.value = !b.value; rowHit = true; } }
                    else {
                        float a = anim(b, b.value ? 1 : 0);
                        R.roundRect(cx, cy, cw, 12, 4, R.mix(0x40000000, R.alpha(R.ACCENT, 0.3f), a));
                        R.roundOutline(cx, cy, cw, 12, 4, 1f, R.mix(0x1CFFFFFF, R.alpha(R.ACCENT, 0.8f), a));
                        Fnt.draw(label, cx + 5, cy + 3.2f, 6, R.mix(MUTED, 0xFFCFE0FF, a));
                    }
                    cx += cw + 4;
                }
                idx--;
                y = cy + 12 + 6;
                continue;
            }
            boolean hit = click && my >= y && my < y + (st instanceof Setting.Col ? 32 : LINE);
            if (paint) Fnt.draw(st.name, x0, y + 6, 8, TEXT);
            if (st instanceof Setting.Bool) {
                Setting.Bool b = (Setting.Bool) st;
                if (hit) { if (mx >= rx - 26) b.value = !b.value; rowHit = true; }
                else if (paint) toggle(rx - 18, y + 5, anim(st, b.value ? 1 : 0));
            } else if (st instanceof Setting.Num) {
                Setting.Num n = (Setting.Num) st;
                float sw = Math.min(110, w * 0.36f), sx = rx - 24 - sw;
                if (hit) { if (mx >= sx - 6 && mx <= sx + sw + 6) { dragging = n; dragX = sx; dragW = sw; } rowHit = true; }
                else if (paint) {
                    float f = (float) ((n.value - n.min) / (n.max - n.min));
                    R.roundRect(sx, y + 9f, sw, 2, 1f, 0x66000000);
                    R.roundRect(sx, y + 9f, sw * f, 2, 1f, R.ACCENT);
                    R.circle(sx + sw * f, y + 10, 3.6f, 0xFFFFFFFF);
                    String v = n.display();
                    Fnt.draw(v, rx - Fnt.width(v, 7), y + 6.5f, 7, MUTED);
                }
            } else if (st instanceof Setting.Mode && segments((Setting.Mode) st) <= w * 0.66f) {
                // few short choices: all of them side by side, the chosen one filled
                Setting.Mode m = (Setting.Mode) st;
                float total = segments(m), bx = rx - total;
                if (paint) R.roundRect(bx, y + 3, total, 14, 4, 0x40000000);
                float ox = bx + 2;
                for (int i = 0; i < m.options.length; i++) {
                    float sw = Fnt.width(m.options[i], 6) + 9;
                    if (hit && mx >= ox && mx < ox + sw) m.index = i;
                    if (paint) {
                        if (i == m.index) R.roundRect(ox, y + 5, sw, 10, 3, R.ACCENT);
                        Fnt.draw(m.options[i], ox + 4.5f, y + 7.2f, 6, i == m.index ? 0xFFFFFFFF : MUTED);
                    }
                    ox += sw;
                }
                if (hit) rowHit = true;
            } else if (st instanceof Setting.Mode || st instanceof Setting.Key) {
                float bw = Math.min(110, w * 0.5f), bx = rx - bw;
                if (hit) {
                    if (mx >= bx) { if (st instanceof Setting.Mode) ((Setting.Mode) st).cycle(button == 1 || mx < bx + bw / 2 ? -1 : 1); else binding = (Setting.Key) st; }
                    rowHit = true;
                } else if (paint) {
                    boolean hov = R.in(mx, my, bx, y + 3, bw, 14), wait = binding == st;
                    R.roundRect(bx, y + 3, bw, 14, 4, wait ? R.ACCENT : hov ? 0x66000000 : 0x40000000);
                    if (st instanceof Setting.Mode) {
                        Fnt.draw("<", bx + 5, y + 6f, 8, MUTED);
                        Fnt.draw(">", bx + bw - 9, y + 6f, 8, MUTED);
                        Fnt.center(Fnt.fit(((Setting.Mode) st).get(), 7, bw - 24), bx + bw / 2f, y + 6.5f, 7, TEXT);
                    } else {
                        String k = ((Setting.Key) st).key;
                        Fnt.center(wait ? "Press a key..." : k.equals("NONE") ? "Not set" : k, bx + bw / 2f, y + 6.5f, 7, TEXT);
                    }
                }
            } else if (st instanceof Setting.Text) {
                Setting.Text ts = (Setting.Text) st;
                float bw = Math.min(150, w * 0.5f), bx = rx - bw;
                if (hit) { if (mx >= bx) editing = ts; rowHit = true; }
                else if (paint) {
                    boolean focus = editing == ts;
                    R.roundRect(bx, y + 3, bw, 14, 4, 0x40000000);
                    R.roundOutline(bx, y + 3, bw, 14, 4, 1f, focus ? R.ACCENT : 0x1CFFFFFF);
                    String shown = ts.value;
                    if (!focus && ts.name.endsWith("Key") && shown.length() > 4) shown = shown.substring(0, 4) + " ********"; // keys are not left readable on screen
                    while (shown.length() > 0 && Fnt.width(shown, 7) > bw - 12) shown = shown.substring(1);
                    if (focus && System.currentTimeMillis() / 500 % 2 == 0) shown += "|";
                    if (shown.isEmpty() && !focus) Fnt.draw("Click to type...", bx + 6, y + 6.5f, 7, 0xFF6F778A);
                    else Fnt.draw(shown, bx + 6, y + 6.5f, 7, TEXT);
                }
            } else if (st instanceof Setting.Col) {
                // a line of its own: the swatches do not fit beside the name
                Setting.Col c = (Setting.Col) st;
                int n = Setting.Col.PALETTE.length;
                float step = Math.min(13, w / (n + 1)), sx = x0, cy = y + 18;
                if (hit) {
                    int i = (int) Math.floor((mx - sx) / step);
                    if (my >= cy - 2 && mx >= sx && i >= 0 && i <= n) { if (i == n) c.chroma = true; else { c.chroma = false; c.color = Setting.Col.PALETTE[i]; } }
                    rowHit = true;
                } else if (paint) {
                    for (int i = 0; i <= n; i++) {
                        float cx = sx + i * step;
                        boolean sel = i == n ? c.chroma : (!c.chroma && c.color == Setting.Col.PALETTE[i]);
                        if (i == n) R.rainbow(cx + 1, cy + 1, 8, 8); else R.roundRect(cx, cy, 10, 10, 3, Setting.Col.PALETTE[i]);
                        if (sel) R.roundOutline(cx - 1, cy - 1, 12, 12, 4, 1.5f, 0xFFFFFFFF);
                    }
                }
                y += 12;
            }
            y += LINE;
        }
        return y;
    }

    // ---------------------------------------------------------------- input

    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0 && button != 1) return;
        mx = tx(mx); my = ty(my);
        searching = false;
        editing = null;
        binding = null;
        // sidebar
        int y = py + CAT_Y;
        for (int i = -1; i < Category.values().length; i++) {
            if (i >= 0 && Lumen.modules.of(Category.values()[i]).isEmpty()) continue;
            if (R.in(mx, my, px, y, SIDE, CAT_H)) {
                category = i < 0 ? null : Category.values()[i];
                search = ""; scrollTo = 0; profiles = false; if (store) { store = false; StorePage.closed(); }
                return;
            }
            y += CAT_H;
        }
        if (R.in(mx, my, px + 9, py + H - 64, SIDE - 18, 19)) {
            store = !store; profiles = false;
            if (store) StorePage.opened(); else StorePage.closed();
            return;
        }
        if (R.in(mx, my, px + 9, py + H - 41, SIDE - 18, 19)) { if (store) { store = false; StorePage.closed(); } profiles = !profiles; profileNote = ""; settingsScrollTo = settingsScroll = 0; Client.sync(); return; }
        if (store) { StorePage.click(mx, my, button, px + SIDE + 14, py + 14, W - SIDE - 28, H - 28); return; }
        if (profiles) {
            int sx0 = px + SIDE + 14, sw = W - SIDE - 28, stop = py + 14 + 26;
            if (my >= stop && my < py + H - 8) {
                if (clickRows(Client.shown, sx0, stop + 4 - settingsScroll, sw, mx, my, button)) Client.apply(); else clickProfiles(mx, my);
            }
            return;
        }
        int x0 = gridX(), y0 = py + 12, w = gridW();
        if (R.in(mx, my, x0 + w - 62, y0, 62, 18)) { mc.displayGuiScreen(new HudEditorScreen()); return; }
        if (R.in(mx, my, x0, y0, w - 68, 18)) { searching = true; return; }
        int top = gridTop(), viewH = gridH();
        if (mx < px + W - SETW) {
            if (my < top || my >= top + viewH) return;
            float cw = (w - GAP * 2) / 3f;
            List<Module> list = visible();
            for (int i = 0; i < list.size(); i++) {
                Module m = list.get(i);
                float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
                if (!R.in(mx, my, cx, cy, cw, CARD_H)) continue;
                if (button == 0 && R.in(mx, my, cx + cw - 30, cy, 30, 20)) m.toggle();
                else { if (open != m) settingsScrollTo = settingsScroll = 0; open = m; }
                return;
            }
            return;
        }
        if (open == null) return;
        int sx = setX(), sw = setW();
        if (R.in(mx, my, sx + sw - 24, py + 12, 26, 16)) { open.toggle(); return; }
        int stop = setTop();
        if (my < stop || my >= stop + setH()) return;
        float sy = stop - settingsScroll;
        if (clickRows(open.settings, sx, sy, sw, mx, my, button)) return;
        float end = rows(open.settings, sx, sy, sw, -9999, -9999, -2);
        if (open instanceof HudModule && my >= end && my < end + LINE && mx >= sx + sw - 66) mc.displayGuiScreen(new HudEditorScreen());
    }

    protected void mouseReleased(int mx, int my, int state) { dragging = null; }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        float step = d > 0 ? -36 : 36;
        if (store) StorePage.wheel(step); else if (profiles) settingsScrollTo += step; else if (tx(Mouse.getEventX() * width / mc.displayWidth) < px + W - SETW) scrollTo += step; else settingsScrollTo += step;
    }

    protected void keyTyped(char c, int key) throws IOException {
        if (binding != null) {
            // Escape cancels, Backspace / Delete clears, anything else becomes the key.
            if (key == Keyboard.KEY_BACK || key == Keyboard.KEY_DELETE) binding.set("NONE");
            else if (key != Keyboard.KEY_ESCAPE && key > 0) binding.set(dev.lumen.util.Keys.name(key));
            binding = null;
            return;
        }
        if (editing != null) {
            if (key == Keyboard.KEY_RETURN && editing == profileName) { if (Config.saveProfile(profileName.value)) { profileNote = "Saved."; profileName.value = ""; } editing = null; }
            else if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN) editing = null;
            else if (key == Keyboard.KEY_BACK) { if (!editing.value.isEmpty()) editing.value = editing.value.substring(0, editing.value.length() - 1); }
            else if (key == Keyboard.KEY_V && isCtrlKeyDown()) {
                String clip = ChatAllowedCharacters.filterAllowedCharacters(getClipboardString());
                editing.value = (editing.value + clip).length() > editing.max ? (editing.value + clip).substring(0, editing.max) : editing.value + clip;
            } else if (ChatAllowedCharacters.isAllowedCharacter(c) && editing.value.length() < editing.max) editing.value += c;
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            if (searching) { searching = false; return; }
            if (store) { store = false; StorePage.closed(); return; }
            if (profiles) { profiles = false; return; }
            mc.displayGuiScreen(null);
            return;
        }
        if (Client.keyMenu.is(key) && !searching) { mc.displayGuiScreen(null); return; }
        if (profiles || store) return;
        if (key == Keyboard.KEY_BACK) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            searching = true;
        } else if (ChatAllowedCharacters.isAllowedCharacter(c) && search.length() < 24) {
            search += c;
            searching = true;
            scrollTo = 0;
        }
    }
}
