package dev.lumen.fabric.gui;

import dev.lumen.fabric.core.Client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumen.fabric.Lumen;
import dev.lumen.fabric.core.Category;
import dev.lumen.fabric.core.Config;
import dev.lumen.fabric.core.HudModule;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.core.Setting;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The RSHIFT mod menu: category sidebar, searchable module grid and per-module settings. */
public class ModMenuScreen extends Screen {
    private static final int W = 600, H = 330, SIDE = 98, SETW = 190, CARD_H = 44, GAP = 11, ROW = 26, LINE = 20;
    private static final net.minecraft.resources.Identifier MOON = net.minecraft.resources.Identifier.fromNamespaceAndPath("lumen", "textures/title/moon.png");
    /** Where a line of letters goes so that it sits where the 1.8.9 menu puts it (there y is the top of the line box). */
    private static float u(float y, float size) { return y + 0.3f * size; }
    private static final int TEXT = 0xFFF2F4F8, MUTED = 0xFF8891A5, GREEN = 0xFF2FBF71, OFF = 0xFF2B303B, RED = 0xFFB0344E;
    private static final int ESC = InputConstants.KEY_ESCAPE, ENTER = InputConstants.KEY_RETURN, BACKSPACE = InputConstants.KEY_BACKSPACE, KEY_V = InputConstants.KEY_V;
    private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT, RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;

    private static Category category; // null = all mods
    private Module open;
    /** For screenshots and tests: opens with this mod's settings showing. */
    public ModMenuScreen select(Module m) { open = m; return this; }
    /** Opens straight on the Store. */
    public ModMenuScreen openStore() { store = true; StorePage.opened(); return this; }
    private boolean rowHit;
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
    private final Map<Object, Float> anim = new HashMap<>();

    private int px, py;

    public ModMenuScreen() { super(Component.literal("Noctua Client")); }

    @Override protected void init() { px = (width - W) / 2; py = (height - H) / 2; dev.lumen.fabric.compat.Compat.textInput(this, true); Client.sync(); Theme.apply(); }
    @Override public void removed() { dev.lumen.fabric.compat.Compat.textInput(this, false); StorePage.closed(); Config.save(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    private void close() { minecraft.gui.setScreen(null); }

    /** Menu Blur off: skip the blurred backdrop and only dim the game. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
        if (minecraft.level == null) super.extractBackground(g, mx, my, partial);
        else if (dev.lumen.fabric.Hooks.menuBlur()) g.blurBeforeThisStratum(); // strength comes from Hooks.blurRadius, not the game option
    }

    /** 0..1..0 over time for sliding long names: pauses at both ends, speed independent of how far it has to travel. */
    private static float slide(float distance) {
        float travel = Math.max(0.6f, distance / 14f), pause = 1.2f, cycle = 2 * (travel + pause);
        float t = (System.currentTimeMillis() % (long) (cycle * 1000)) / 1000f;
        float p = t < pause ? 0 : t < pause + travel ? (t - pause) / travel : t < 2 * pause + travel ? 1 : 1 - (t - 2 * pause - travel) / travel;
        return p * p * (3 - 2 * p);
    }

    private float anim(Object key, float target) {
        Float cur = anim.get(key);
        float v = Draw.lerp(cur == null ? target : cur, target, 0.25f);
        anim.put(key, v);
        return v;
    }

    private List<Module> visible() {
        List<Module> out = new ArrayList<>();
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
    private double tx(double mx) { return (mx - width / 2.0) / fit() + width / 2.0; }
    private double ty(double my) { return (my - height / 2.0) / fit() + height / 2.0; }
    /** Panel coordinates back to real GUI coordinates, for things the game draws outside our scaled panel. */
    float sx(float x) { return (x - width / 2f) * fit() + width / 2f; }
    float sy(float y) { return (y - height / 2f) * fit() + height / 2f; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int rawX, int rawY, float partial) {
        Draw.begin(g);
        Draw.uiScale = fit();
        Draw.square = Client.corners.is("Square");
        double mx = tx(rawX), my = ty(rawY);
        Draw.rect(0, 0, width, height, minecraft.level == null ? 0x55000000 : 0x60060910);
        float f = fit();
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, height / 2f);
        g.pose().scale(f, f);
        g.pose().translate(-width / 2f, -height / 2f);

        float pr = 9;
        Draw.roundRect(px, py, W, H, pr, 0x78000000);
        Draw.scissor(px, py, SIDE, H); // one extra layer per pane, so no seam where the pieces would overlap
        Draw.roundRect(px, py, SIDE + 30, H, pr, 0x30000000);
        Draw.endScissor();
        if (!store && !profiles) {
            Draw.scissor(px + W - SETW, py, SETW, H);
            Draw.roundRect(px + W - SETW - 30, py, SETW + 30, H, pr, 0x40000000);
            Draw.endScissor();
            Draw.rect(px + W - SETW, py, 1, H, 0x16FFFFFF);
        }
        Draw.rect(px + SIDE, py, 1, H, 0x16FFFFFF);
        Draw.roundOutline(px, py, W, H, pr, 0x1EFFFFFF);

        drawSidebar(mx, my);
        if (store) StorePage.draw(this, mx, my, px + SIDE + 14, py + 14, W - SIDE - 28, H - 28);
        else if (profiles) drawProfiles(mx, my); else { drawGrid(mx, my); drawSettings(mx, my); }
        g.pose().popMatrix();
    }

    private static final int CAT_Y = 44, CAT_H = 20;

    private void spaced(String t, float x, float y, float size, float gap, int color, boolean bold) {
        for (int i = 0; i < t.length(); i++) { String c = t.substring(i, i + 1); Draw.ui(c, x, y, size, color, bold); x += Draw.uiWidth(c, size, bold) + gap; }
    }
    private float spacedW(String t, float size, float gap, boolean bold) { float w = 0; for (int i = 0; i < t.length(); i++) w += Draw.uiWidth(t.substring(i, i + 1), size, bold) + gap; return w - gap; }

    private void drawSidebar(double mx, double my) {
        try {
            if (minecraft.getResourceManager().getResource(MOON).isPresent()) {
                var g = Draw.g();
                g.pose().pushMatrix();
                g.pose().translate(px + 12, py + 12);
                g.pose().scale(16 / 128f, 16 / 128f);
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MOON, 0, 0, 0f, 0f, 128, 128, 128, 128);
                g.pose().popMatrix();
            }
        } catch (Throwable ignored) {}
        spaced("NOCTUA", px + 31, u(py + 15f, 10), 10, 1.2f, 0xFFFFFFFF, true);

        int y = py + CAT_Y;
        for (int i = -1; i < Category.values().length; i++) {
            Category c = i < 0 ? null : Category.values()[i];
            if (c != null && Lumen.modules.of(c).isEmpty()) continue; // e.g. Cosmetics, which moved to the Store
            boolean sel = !profiles && !store && search.isEmpty() && category == c;
            boolean hov = Draw.in(mx, my, px, y, SIDE, CAT_H);
            float a = anim("cat" + i, sel ? 1f : hov ? 0.35f : 0f);
            if (a > 0.01f) Draw.rect(px, y, SIDE, CAT_H, Draw.alpha(Draw.ACCENT, a * 0.18f));
            if (sel) Draw.roundRect(px, y + 4, 2, CAT_H - 8, 1, Draw.ACCENT);
            spaced((c == null ? "All" : c.label).toUpperCase(), px + 13, u(y + 6.5f, 7), 7, 0.6f, sel ? 0xFFFFFFFF : Draw.mix(MUTED, TEXT, a * 2), false);
            String n = String.valueOf(Lumen.modules.of(c).size());
            Draw.ui(n, px + SIDE - 12 - Draw.uiWidth(n, 6), u(y + 7, 6), 6, 0xFF6F778A);
            y += CAT_H;
        }

        boolean sh = Draw.in(mx, my, px + 9, py + H - 64, SIDE - 18, 19);
        float sta = anim("storeTab", store ? 1f : sh ? 0.5f : 0f);
        Draw.roundRect(px + 9, py + H - 64, SIDE - 18, 19, 5, Draw.mix(0x29E5484D, 0x66E5484D, sta));
        StorePage.gem(px + 24, py + H - 59, 9, 0xFFFF3B55);
        Draw.track("STORE", px + SIDE / 2f + 6, u(py + H - 58f, 7), 7, 1.0f, 0xFFFF8F93);
        boolean ph = Draw.in(mx, my, px + 9, py + H - 41, SIDE - 18, 19);
        float sa = anim("settingsTab", profiles && !store ? 1f : ph ? 0.5f : 0f);
        Draw.roundRect(px + 9, py + H - 41, SIDE - 18, 19, 5, Draw.mix(0x40000000, Draw.alpha(Draw.ACCENT, 0.9f), sa));
        Draw.track("OPTIONS", px + SIDE / 2f, u(py + H - 35f, 7), 7, 1.0f, profiles && !store || ph ? TEXT : MUTED);
        String ver = "Noctua Client " + Lumen.VERSION;
        Draw.uiCenter(ver, px + SIDE / 2f, u(py + H - 16, 6), 6, 0xFF6F778A);
    }

    /** Breaks a description into at most {@code lines} lines that fit the width. */
    private static List<String> wrap(String text, float size, float w, int lines) {
        List<String> out = new ArrayList<>();
        String cur = "";
        for (String word : text.split(" ")) {
            String next = cur.isEmpty() ? word : cur + " " + word;
            if (Draw.uiWidth(next, size) <= w || cur.isEmpty()) cur = next;
            else { out.add(cur); cur = word; if (out.size() == lines) break; }
        }
        if (out.size() < lines && !cur.isEmpty()) out.add(cur);
        else if (out.size() == lines && !cur.isEmpty()) out.set(lines - 1, Draw.fit(out.get(lines - 1) + " " + cur, size, w));
        return out;
    }

    private void toggle(float x, float y, float a) {
        Draw.roundRect(x, y, 18, 10, 5, Draw.mix(0x55000000, Draw.ACCENT, a));
        if (a < 0.5f) Draw.roundOutline(x, y, 18, 10, 5, 0x24FFFFFF);
        Draw.roundRect(x + 1.6f + a * 8, y + 1.6f, 6.8f, 6.8f, 3.4f, Draw.mix(0xFFAAB2C5, 0xFFFFFFFF, a));
    }

    private int gridX() { return px + SIDE + 12; }
    private int gridW() { return W - SIDE - SETW - 24; }
    private int gridTop() { return py + 12 + 44; }
    private int gridH() { return H - 12 - 44 - 10; }

    private void drawGrid(double mx, double my) {
        int x0 = gridX(), y0 = py + 12, w = gridW();
        int bw = 62, sw = w - bw - 6;
        Draw.roundRect(x0, y0, sw, 18, 5, 0x40000000);
        Draw.roundOutline(x0, y0, sw, 18, 5, searching ? Draw.ACCENT : 0x1CFFFFFF);
        String shown = search.isEmpty() && !searching ? "Search mods..." : Draw.fit(search, 8, sw - 18) + (searching && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        Draw.ui(shown, x0 + 8, u(y0 + 5f, 8), 8, search.isEmpty() && !searching ? 0xFF6F778A : TEXT);
        boolean eh = Draw.in(mx, my, x0 + w - bw, y0, bw, 18);
        Draw.roundRect(x0 + w - bw, y0, bw, 18, 5, eh ? 0xFF6AA0FF : Draw.alpha(Draw.ACCENT, 0.9f));
        Draw.track("EDIT HUD", x0 + w - bw / 2f, u(y0 + 5.5f, 7), 7, 1.0f, 0xFFFFFFFF);

        List<Module> list = visible();
        int on = 0;
        for (Module m : list) if (m.isEnabled()) on++;
        String title = (search.isEmpty() ? (category == null ? "All mods" : category.label) : "Search").toUpperCase();
        spaced(title, x0 + 1, u(y0 + 27, 6), 6, 0.6f, MUTED, false);
        String count = (on + " of " + list.size() + " enabled").toUpperCase();
        spaced(count, x0 + w - 1 - spacedW(count, 6, 0.6f, false), u(y0 + 27, 6), 6, 0.6f, MUTED, false);
        Draw.rect(x0, y0 + 37, w, 1, Draw.alpha(Draw.ACCENT, 0.75f));

        int top = gridTop(), viewH = gridH();
        float cw = (w - GAP * 2) / 3f;
        int rows = (list.size() + 2) / 3;
        float max = Math.max(0, rows * (CARD_H + GAP) - GAP - viewH);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Draw.lerp(scroll, scrollTo, 0.3f);
        if (list.isEmpty()) Draw.uiCenter("No mods found", x0 + w / 2f, top + 40, 9, MUTED);

        Draw.scissor(x0 - 2, top, w + 4, viewH);
        boolean inView = my >= top && my < top + viewH;
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < top || cy > top + viewH) continue;
            boolean hov = inView && Draw.in(mx, my, cx, cy, cw, CARD_H), sel = open == m;
            float h = anim(m, hov ? 1 : 0);
            Draw.roundRect(cx, cy, cw, CARD_H, 6, sel ? Draw.mix(0x48000000, Draw.alpha(Draw.ACCENT, 0.5f), 0.45f) : Draw.mix(0x40000000, 0x66000000, h));
            Draw.roundOutline(cx, cy, cw, CARD_H, 6, sel ? Draw.alpha(Draw.ACCENT, 0.9f) : Draw.mix(0x1CFFFFFF, 0x38FFFFFF, h));
            float room = cw - 34, titleW = Draw.uiWidth(m.name, 8, true);
            if (titleW <= room) Draw.uiB(m.name, cx + 7, u(cy + 6, 8), 8, TEXT);
            else {
                // Too long for the card: slide it left and right inside the card instead of cutting it off.
                float clipTop = Math.max(cy, top), clipBottom = Math.min(cy + 17, top + viewH);
                if (clipBottom > clipTop) {
                    Draw.endScissor();
                    Draw.scissor(cx + 7, clipTop, room, clipBottom - clipTop);
                    Draw.uiB(m.name, cx + 7 - (titleW - room) * slide(titleW - room), u(cy + 6, 8), 8, TEXT);
                    Draw.endScissor();
                    Draw.scissor(x0 - 2, top, w + 4, viewH);
                }
            }
            List<String> desc = wrap(m.description, 6, cw - 14, 2);
            for (int k = 0; k < desc.size(); k++) Draw.ui(desc.get(k), cx + 7, u(cy + 18.5f + k * 8, 6), 6, MUTED);
            toggle(cx + cw - 25, cy + 6, anim(m.name + "#on", m.isEnabled() ? 1 : 0));
        }
        Draw.endScissor();
        if (max > 0) {
            float barH = Math.max(18, viewH * (float) viewH / (viewH + max));
            Draw.roundRect(x0 + w + 4, top + (viewH - barH) * (scroll / max), 2, barH, 1f, 0x40FFFFFF);
        }
    }

    /** Height of the client settings block at the top of the Settings tab. */
    private int clientH() { return 26 + 4 + Client.shown.size() * LINE + 10; }

    private void drawProfiles(double mx, double my) {
        int x0 = px + SIDE + 14, w = W - SIDE - 28, top = py + 14 + 26, viewH = H - 14 - 26 - 8;
        Draw.uiB("Settings", x0, py + 15, 13, TEXT);
        java.util.List<String> list = Config.profiles();
        float content = clientH() - 26 + 62 + Math.max(1, list.size()) * ROW + 30;
        float max = Math.max(0, content - viewH);
        settingsScrollTo = Math.max(0, Math.min(max, settingsScrollTo));
        settingsScroll = Draw.lerp(settingsScroll, settingsScrollTo, 0.3f);
        Draw.scissor(x0 - 2, top, w + 4, viewH);
        if (my < top || my >= top + viewH) mx = -9999;
        drawRows(Client.shown, x0, top + 4 - settingsScroll, w, mx, my);

        int y0 = py + 14 + clientH() - Math.round(settingsScroll);
        Draw.uiB("Profiles", x0, y0 + 2, 11, TEXT);
        Draw.ui("Each profile stores every mod's on/off state, settings and HUD layout.", x0, y0 + 24, 8, MUTED);

        // new profile row
        float ry = y0 + 40;
        Draw.roundRect(x0, ry, w, ROW - 4, 5, Theme.card);
        boolean focus = editing == profileName;
        Draw.roundBorder(x0 + 8, ry + 3, 170, 16, 4, Theme.ctl, focus ? Draw.ACCENT : Theme.outline);
        String shown = profileName.value + (focus && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        if (shown.isEmpty()) Draw.ui("New profile name...", x0 + 14, ry + 7, 8, 0xFF8089A0); else Draw.ui(shown, x0 + 14, ry + 7, 8, TEXT);
        boolean sh = Draw.in(mx, my, x0 + w - 118, ry + 3, 110, 16);
        Draw.roundRect(x0 + w - 118, ry + 3, 110, 16, 4, sh ? 0xFF6AA0FF : Draw.ACCENT);
        Draw.uiCenter("Save Current Settings", x0 + w - 63, ry + 7, 8, 0xFFFFFFFF);
        if (!profileNote.isEmpty()) Draw.ui(profileNote, x0 + 186, ry + 7, 8, MUTED);

        float y = ry + ROW + 4;
        if (list.isEmpty()) Draw.uiCenter("No profiles yet. Type a name above and press Save.", x0 + w / 2f, y + 8, 9, MUTED);
        for (String name : list) {
            boolean active = name.equals(Config.activeProfile);
            if (active) Draw.roundBorder(x0, y, w, ROW - 4, 5, Theme.card, 0xFF24764D); else Draw.roundRect(x0, y, w, ROW - 4, 5, Theme.card);
            Draw.ui(name, x0 + 10, y + 6.5f, 9, TEXT);
            if (active) Draw.ui("ACTIVE", x0 + 16 + Draw.uiWidth(name, 9), y + 7.5f, 7, GREEN);
            String[] labels = {"Load", "Overwrite", "Delete"};
            float bx = x0 + w - 8;
            for (int i = labels.length - 1; i >= 0; i--) {
                float bw = i == 1 ? 58 : 44;
                bx -= bw;
                boolean hov = Draw.in(mx, my, bx, y + 3, bw, 16);
                Draw.roundRect(bx, y + 3, bw, 16, 4, hov ? (i == 2 ? 0xFFE0526A : i == 0 ? Draw.ACCENT : Theme.ctlHover) : Theme.ctl);
                Draw.uiCenter(labels[i], bx + bw / 2f, y + 7, 8, TEXT);
                bx -= 4;
            }
            y += ROW;
        }
        Draw.endScissor();
        if (max > 0) {
            float barH = Math.max(18, viewH * viewH / (viewH + max));
            Draw.roundRect(px + W - 8, top + (viewH - barH) * (settingsScroll / max), 3, barH, 1.5f, Theme.track);
        }
    }

    private boolean clickProfiles(double mx, double my) {
        int x0 = px + SIDE + 14, y0 = py + 14 + clientH() - Math.round(settingsScroll), w = W - SIDE - 28;
        float ry = y0 + 40;
        if (Draw.in(mx, my, x0 + 8, ry + 3, 170, 16)) { editing = profileName; return true; }
        if (Draw.in(mx, my, x0 + w - 118, ry + 3, 110, 16)) {
            if (Config.saveProfile(profileName.value)) { profileNote = "Saved."; profileName.value = ""; } else profileNote = "Enter a name first.";
            return true;
        }
        float y = ry + ROW + 4;
        for (String name : Config.profiles()) {
            float bx = x0 + w - 8;
            for (int i = 2; i >= 0; i--) {
                float bw = i == 1 ? 58 : 44;
                bx -= bw;
                if (Draw.in(mx, my, bx, y + 3, bw, 16)) {
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

    private void drawSettings(double mx, double my) {
        int x0 = setX(), y0 = py + 12, w = setW();
        if (open == null) {
            Draw.uiCenter("Select a mod", x0 + w / 2f, u(py + H / 2f - 12, 9), 9, TEXT);
            Draw.uiCenter("to see its settings here.", x0 + w / 2f, u(py + H / 2f + 1, 7), 7, MUTED);
            return;
        }
        Draw.uiB(Draw.fit(open.name, 11, w - 30), x0, u(y0 + 1, 11), 11, TEXT);
        toggle(x0 + w - 18, y0 + 3, anim(open.name + "#on", open.isEnabled() ? 1 : 0));
        List<String> desc = wrap(open.description, 6, w, 2);
        for (int k = 0; k < desc.size(); k++) Draw.ui(desc.get(k), x0, u(y0 + 18 + k * 8, 6), 6, MUTED);
        Draw.rect(x0, y0 + 39, w, 1, Draw.alpha(Draw.ACCENT, 0.75f));

        int top = setTop(), viewH = setH();
        if (dragging != null) dragging.set(dragging.min + (dragging.max - dragging.min) * Math.max(0, Math.min(1, (mx - dragX) / dragW)));
        Draw.scissor(x0 - 2, top, w + 4, viewH);
        boolean inView = my >= top && my < top + viewH;
        float y = rows(open.settings, x0, top - settingsScroll, w, inView ? mx : -9999, my, -1);
        if (open instanceof HudModule) {
            Draw.ui("Position & size", x0, u(y + 6, 8), 8, TEXT);
            boolean hov = inView && Draw.in(mx, my, x0 + w - 66, y + 3, 66, 14);
            Draw.roundRect(x0 + w - 66, y + 3, 66, 14, 4, hov ? Draw.ACCENT : 0x40000000);
            Draw.uiCenter("HUD Editor", x0 + w - 33, u(y + 6.5f, 7), 7, TEXT);
            y += LINE;
        }
        Draw.endScissor();
        float max = Math.max(0, y + settingsScroll - top - viewH + 4);
        settingsScrollTo = Math.max(0, Math.min(max, settingsScrollTo));
        settingsScroll = Draw.lerp(settingsScroll, settingsScrollTo, 0.3f);
        if (max > 0) {
            float barH = Math.max(18, viewH * (float) viewH / (viewH + max));
            Draw.roundRect(px + W - 6, top + (viewH - barH) * (settingsScroll / max), 2, barH, 1f, 0x40FFFFFF);
        }
    }

    private static float segments(Setting.Mode m) { float t = 4; for (String o : m.options) t += Draw.uiWidth(o, 6) + 9; return t; }

    /** The settings whose names share a "Group: " prefix with this one, as one block of chips. */
    private static String group(Setting st) {
        int i = st.name.indexOf(": ");
        return st instanceof Setting.Bool && i > 0 ? st.name.substring(0, i) : null;
    }

    private float drawRows(List<? extends Setting> list, int x0, float y, int w, double mx, double my) { return rows(list, x0, y, w, mx, my, -1); }
    private boolean clickRows(List<? extends Setting> list, int x0, float sy, int w, double mx, double my, int button) { rowHit = false; rows(list, x0, sy, w, mx, my, button == RIGHT ? 1 : 0); return rowHit; }

    /**
     * One pass over the settings that either draws them (button -1), measures them (-2) or applies a click, so all
     * three always agree on where things are. @return the y below the last row.
     */
    private float rows(List<? extends Setting> list, float x0, float y, float w, double mx, double my, int button) {
        boolean click = button >= 0, paint = button == -1;
        float rx = x0 + w;
        for (int idx = 0; idx < list.size(); idx++) {
            Setting st = list.get(idx);
            String g = group(st);
            if (g != null) {
                if (paint) spaced(g.toUpperCase() + "S", x0, u(y + 6, 6), 6, 0.6f, MUTED, false);
                float cx = x0, cy = y + 17;
                for (; idx < list.size() && g.equals(group(list.get(idx))); idx++) {
                    Setting.Bool b = (Setting.Bool) list.get(idx);
                    String label = b.name.substring(g.length() + 2);
                    float cw = Draw.uiWidth(label, 6) + 10;
                    if (cx + cw > rx && cx > x0) { cx = x0; cy += 15; }
                    if (click) { if (Draw.in(mx, my, cx, cy, cw, 12)) { b.value = !b.value; rowHit = true; } }
                    else if (paint) {
                        float a = anim(b, b.value ? 1 : 0);
                        Draw.roundRect(cx, cy, cw, 12, 4, Draw.mix(0x40000000, Draw.alpha(Draw.ACCENT, 0.3f), a));
                        Draw.roundOutline(cx, cy, cw, 12, 4, Draw.mix(0x1CFFFFFF, Draw.alpha(Draw.ACCENT, 0.8f), a));
                        Draw.ui(label, cx + 5, u(cy + 3.2f, 6), 6, Draw.mix(MUTED, 0xFFCFE0FF, a));
                    }
                    cx += cw + 4;
                }
                idx--;
                y = cy + 12 + 6;
                continue;
            }
            boolean hit = click && my >= y && my < y + (st instanceof Setting.Col ? 32 : LINE);
            if (paint) Draw.ui(st.name, x0, u(y + 6, 8), 8, TEXT);
            if (st instanceof Setting.Bool b) {
                if (hit) { if (mx >= rx - 26) b.value = !b.value; rowHit = true; }
                else if (paint) toggle(rx - 18, y + 5, anim(st, b.value ? 1 : 0));
            } else if (st instanceof Setting.Num n) {
                float sw = Math.min(110, w * 0.36f), sx = rx - 24 - sw;
                if (hit) { if (mx >= sx - 6 && mx <= sx + sw + 6) { dragging = n; dragX = sx; dragW = sw; slide(mx); } rowHit = true; }
                else if (paint) {
                    float f = (float) ((n.value - n.min) / (n.max - n.min));
                    Draw.roundRect(sx, y + 9f, sw, 2, 1f, 0x66000000);
                    Draw.roundRect(sx, y + 9f, sw * f, 2, 1f, Draw.ACCENT);
                    Draw.roundRect(sx + sw * f - 3.6f, y + 6.4f, 7.2f, 7.2f, 3.6f, 0xFFFFFFFF);
                    String v = n.display();
                    Draw.ui(v, rx - Draw.uiWidth(v, 7), u(y + 6.5f, 7), 7, MUTED);
                }
            } else if (st instanceof Setting.Mode m && segments(m) <= w * 0.66f) {
                float total = segments(m), bx = rx - total;
                if (paint) Draw.roundRect(bx, y + 3, total, 14, 4, 0x40000000);
                float ox = bx + 2;
                for (int i = 0; i < m.options.length; i++) {
                    float sw = Draw.uiWidth(m.options[i], 6) + 9;
                    if (hit && mx >= ox && mx < ox + sw) m.index = i;
                    if (paint) {
                        if (i == m.index) Draw.roundRect(ox, y + 5, sw, 10, 3, Draw.ACCENT);
                        Draw.ui(m.options[i], ox + 4.5f, u(y + 7.2f, 6), 6, i == m.index ? 0xFFFFFFFF : MUTED);
                    }
                    ox += sw;
                }
                if (hit) rowHit = true;
            } else if (st instanceof Setting.Mode || st instanceof Setting.Key) {
                float bw = Math.min(110, w * 0.5f), bx = rx - bw;
                if (hit) {
                    if (mx >= bx) { if (st instanceof Setting.Mode m) m.cycle(button == 1 || mx < bx + bw / 2 ? -1 : 1); else binding = (Setting.Key) st; }
                    rowHit = true;
                } else if (paint) {
                    boolean hov = Draw.in(mx, my, bx, y + 3, bw, 14), wait = binding == st;
                    Draw.roundRect(bx, y + 3, bw, 14, 4, wait ? Draw.ACCENT : hov ? 0x66000000 : 0x40000000);
                    if (st instanceof Setting.Mode m) {
                        Draw.ui("<", bx + 5, u(y + 6f, 8), 8, MUTED);
                        Draw.ui(">", bx + bw - 9, u(y + 6f, 8), 8, MUTED);
                        Draw.uiCenter(Draw.fit(m.get(), 7, bw - 24), bx + bw / 2f, u(y + 6.5f, 7), 7, TEXT);
                    } else {
                        String k = ((Setting.Key) st).key;
                        Draw.uiCenter(wait ? "Press a key..." : k.equals("NONE") ? "Not set" : k, bx + bw / 2f, u(y + 6.5f, 7), 7, TEXT);
                    }
                }
            } else if (st instanceof Setting.Text ts) {
                float bw = Math.min(150, w * 0.5f), bx = rx - bw;
                if (hit) { if (mx >= bx) editing = ts; rowHit = true; }
                else if (paint) {
                    boolean focus = editing == ts;
                    Draw.roundRect(bx, y + 3, bw, 14, 4, 0x40000000);
                    Draw.roundOutline(bx, y + 3, bw, 14, 4, focus ? Draw.ACCENT : 0x1CFFFFFF);
                    String shown = ts.value;
                    if (!focus && ts.name.endsWith("Key") && shown.length() > 4) shown = shown.substring(0, 4) + " ********";
                    while (shown.length() > 0 && Draw.uiWidth(shown, 7) > bw - 12) shown = shown.substring(1);
                    if (focus && System.currentTimeMillis() / 500 % 2 == 0) shown += "|";
                    if (shown.isEmpty() && !focus) Draw.ui("Click to type...", bx + 6, u(y + 6.5f, 7), 7, 0xFF6F778A);
                    else Draw.ui(shown, bx + 6, u(y + 6.5f, 7), 7, TEXT);
                }
            } else if (st instanceof Setting.Col c) {
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
                        if (i == n) Draw.rainbow(cx + 1, cy + 1, 8, 8); else Draw.roundRect(cx, cy, 10, 10, 3, Setting.Col.PALETTE[i]);
                        if (sel) Draw.roundOutline(cx - 1, cy - 1, 12, 12, 4, 0xFFFFFFFF);
                    }
                }
                y += 12;
            }
            y += LINE;
        }
        return y;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        int button = e.button();
        if (button != LEFT && button != RIGHT) return false;
        double mx = tx(e.x()), my = ty(e.y());
        searching = false;
        editing = null;
        binding = null;
        // sidebar
        int y = py + CAT_Y;
        for (int i = -1; i < Category.values().length; i++) {
            if (i >= 0 && Lumen.modules.of(Category.values()[i]).isEmpty()) continue;
            if (Draw.in(mx, my, px, y, SIDE, CAT_H)) {
                category = i < 0 ? null : Category.values()[i];
                search = ""; scrollTo = 0; profiles = false; if (store) { store = false; StorePage.closed(); }
                return true;
            }
            y += CAT_H;
        }
        if (Draw.in(mx, my, px + 9, py + H - 64, SIDE - 18, 19)) {
            store = !store; profiles = false;
            if (store) StorePage.opened(); else StorePage.closed();
            return true;
        }
        if (Draw.in(mx, my, px + 9, py + H - 41, SIDE - 18, 19)) { if (store) { store = false; StorePage.closed(); } profiles = !profiles; profileNote = ""; settingsScrollTo = settingsScroll = 0; Client.sync(); return true; }
        if (store) { StorePage.click(mx, my, button, px + SIDE + 14, py + 14, W - SIDE - 28, H - 28); return true; }
        if (profiles) {
            int sx0 = px + SIDE + 14, sw = W - SIDE - 28, stop = py + 14 + 26;
            if (my >= stop && my < py + H - 8) {
                if (clickRows(Client.shown, sx0, stop + 4 - settingsScroll, sw, mx, my, button)) Client.apply(); else clickProfiles(mx, my);
            }
            return true;
        }
        int x0 = gridX(), y0 = py + 12, w = gridW();
        if (Draw.in(mx, my, x0 + w - 62, y0, 62, 18)) { minecraft.gui.setScreen(new HudEditorScreen()); return true; }
        if (Draw.in(mx, my, x0, y0, w - 68, 18)) { searching = true; return true; }
        int top = gridTop(), viewH = gridH();
        if (mx < px + W - SETW) {
            if (my < top || my >= top + viewH) return true;
            float cw = (w - GAP * 2) / 3f;
            List<Module> list = visible();
            for (int i = 0; i < list.size(); i++) {
                Module m = list.get(i);
                float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
                if (!Draw.in(mx, my, cx, cy, cw, CARD_H)) continue;
                if (button == LEFT && Draw.in(mx, my, cx + cw - 30, cy, 30, 20)) m.toggle();
                else { if (open != m) settingsScrollTo = settingsScroll = 0; open = m; }
                return true;
            }
            return true;
        }
        if (open == null) return true;
        int sx = setX(), sw = setW();
        if (Draw.in(mx, my, sx + sw - 24, py + 12, 26, 16)) { open.toggle(); return true; }
        int stop = setTop();
        if (my < stop || my >= stop + setH()) return true;
        float sy = stop - settingsScroll;
        if (clickRows(open.settings, sx, sy, sw, mx, my, button)) return true;
        float end = rows(open.settings, sx, sy, sw, -9999, -9999, -2);
        if (open instanceof HudModule && my >= end && my < end + LINE && mx >= sx + sw - 66) minecraft.gui.setScreen(new HudEditorScreen());
        return true;
    }

    private void slide(double mx) {
        if (dragging != null) dragging.set(dragging.min + (dragging.max - dragging.min) * Math.max(0, Math.min(1, (mx - dragX) / dragW)));
        if (dragging != null && profiles) Client.apply();
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { slide(tx(e.x())); return true; }
    @Override public boolean mouseReleased(MouseButtonEvent e) { dragging = null; return true; }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy == 0) return false;
        float step = sy > 0 ? -36 : 36;
        if (store) StorePage.wheel(step); else if (profiles) settingsScrollTo += step; else if (tx(mx) < px + W - SETW) scrollTo += step; else settingsScrollTo += step;
        return true;
    }

    private void saveNewProfile() {
        if (Config.saveProfile(profileName.value)) { profileNote = "Saved."; profileName.value = ""; } else profileNote = "Enter a name first.";
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = e.key();
        if (binding != null) {
            // Escape cancels, Backspace / Delete clears, anything else becomes the key.
            if (key == BACKSPACE || key == InputConstants.KEY_DELETE) binding.set("NONE");
            else if (key != ESC) { String n = dev.lumen.fabric.util.Keys.name(key); if (!n.equals("NONE")) binding.set(n); }
            binding = null;
            return true;
        }
        if (editing != null) {
            if (key == ENTER && editing == profileName) { saveNewProfile(); editing = null; }
            else if (key == ESC || key == ENTER) editing = null;
            else if (key == BACKSPACE) { if (!editing.value.isEmpty()) editing.value = editing.value.substring(0, editing.value.length() - 1); }
            else if (key == KEY_V && (e.modifiers() & InputConstants.MOD_CONTROL) != 0) {
                String clip = minecraft.keyboardHandler.getClipboard().replaceAll("[\\p{Cntrl}\u00A7]", "");
                String v = editing.value + clip;
                editing.value = v.length() > editing.max ? v.substring(0, editing.max) : v;
            }
            return true;
        }
        if (key == ESC) {
            if (searching) searching = false;
            else if (store) { store = false; StorePage.closed(); }
            else if (profiles) profiles = false;
            else if (open != null) open = null;
            else close();
            return true;
        }
        if (Client.keyMenu.is(key) && !searching) { close(); return true; }
        if (profiles || store) return true;
        if (key == BACKSPACE) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            searching = true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!e.isAllowedChatCharacter()) return false;
        String c = e.codepointAsString();
        if (editing != null) {
            if (editing.value.length() + c.length() <= editing.max) editing.value += c;
            return true;
        }
        if (profiles || store) return false;
        if (search.length() < 24) { search += c; searching = true; scrollTo = 0; }
        return true;
    }
}
