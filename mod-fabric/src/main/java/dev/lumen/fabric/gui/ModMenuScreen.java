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
    private static final int W = 452, H = 308, SIDE = 108, CARD_H = 72, GAP = 8, ROW = 26;
    private static final int TEXT = 0xFFF2F4F8, MUTED = 0xFF8891A5, GREEN = 0xFF2FBF71, OFF = 0xFF2B303B, RED = 0xFFB0344E;
    private static final int ESC = InputConstants.KEY_ESCAPE, ENTER = InputConstants.KEY_RETURN, BACKSPACE = InputConstants.KEY_BACKSPACE, KEY_V = InputConstants.KEY_V;
    private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT, RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;

    private static Category category; // null = all mods
    private Module open;
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

    public ModMenuScreen() { super(Component.literal("Lumen Client")); }

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
        Draw.rect(0, 0, width, height, minecraft.level == null ? 0x55000000 : Theme.dim);
        float f = fit();
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, height / 2f);
        g.pose().scale(f, f);
        g.pose().translate(-width / 2f, -height / 2f);

        float pr = Theme.glass ? 14 : 9;
        Draw.roundBorder(px, py, W, H, pr, Theme.bg, Theme.glass ? Theme.outline : 0xFF262B36);
        Draw.scissor(px + 1, py + 1, SIDE - 1, H - 2); // one translucent layer only, so no seam where the pieces would overlap
        Draw.roundRect(px + 1, py + 1, SIDE + 30, H - 2, pr - 1, Theme.side);
        Draw.endScissor();
        Draw.rect(px + SIDE, py + 1, 1, H - 2, Theme.line);

        drawSidebar(mx, my);
        if (store) StorePage.draw(this, mx, my, px + SIDE + 14, py + 14, W - SIDE - 28, H - 28);
        else if (profiles) drawProfiles(mx, my); else if (open == null) drawGrid(mx, my); else drawSettings(mx, my);
        g.pose().popMatrix();
    }

    private void drawSidebar(double mx, double my) {
                Draw.uiB("LUMEN", px + 15, py + 12, 17, 0xFFFFFFFF);
        Draw.uiB("C L I E N T", px + 16, py + 34, 7, Draw.ACCENT);
        String ver = "v" + Lumen.VERSION;
        Draw.ui(ver, px + SIDE - 12 - Draw.uiWidth(ver, 7), py + 35, 7, (Theme.glass ? 0xFFA9B1C4 : 0xFF5C6475));

        int y = py + 52;
        for (int i = -1; i < Category.values().length; i++) {
            Category c = i < 0 ? null : Category.values()[i];
            if (c != null && Lumen.modules.of(c).isEmpty()) continue; // e.g. Cosmetics, which moved to the Store
            boolean sel = !profiles && !store && open == null && search.isEmpty() && category == c;
            boolean hov = Draw.in(mx, my, px + 8, y, SIDE - 16, 18);
            float a = anim("cat" + i, sel ? 1f : hov ? 0.35f : 0f);
            if (a > 0.01f) Draw.roundRect(px + 8, y, SIDE - 16, 18, 5, Draw.alpha(Draw.ACCENT, a * 0.9f));
            Draw.ui(c == null ? "All Mods" : c.label, px + 15, y + 4.5f, 9, sel ? 0xFFFFFFFF : Draw.mix(MUTED, TEXT, a * 2));
            String n = String.valueOf(Lumen.modules.of(c).size());
            Draw.ui(n, px + SIDE - 14 - Draw.uiWidth(n, 8), y + 5, 8, sel ? 0xDDFFFFFF : (Theme.glass ? 0xFFA9B1C4 : 0xFF5C6475));
            y += 20;
        }

        boolean sh = Draw.in(mx, my, px + 8, py + H - 88, SIDE - 16, 22);
        float sta = anim("storeTab", store ? 1f : sh ? 0.35f : 0f);
        Draw.roundRect(px + 8, py + H - 88, SIDE - 16, 22, 6, sta > 0.01f ? Draw.mix(Theme.card, Draw.ACCENT, sta * 0.9f) : Theme.card);
        StorePage.gem(px + 17, py + H - 82, 10, 0xFFFF3B55);
        Draw.track("STORE", px + SIDE / 2f + 4, py + H - 80.5f, 7, 1.2f, store || sh ? TEXT : MUTED);
        boolean ph = Draw.in(mx, my, px + 8, py + H - 62, SIDE - 16, 22);
        float sa = anim("settingsTab", profiles && !store ? 1f : ph ? 0.35f : 0f);
        Draw.roundRect(px + 8, py + H - 62, SIDE - 16, 22, 6, sa > 0.01f ? Draw.mix(Theme.card, Draw.ACCENT, sa * 0.9f) : Theme.card);
        Draw.track("SETTINGS", px + SIDE / 2f, py + H - 54.5f, 7, 1.2f, profiles && !store || ph ? TEXT : MUTED);

        boolean hov = Draw.in(mx, my, px + 8, py + H - 34, SIDE - 16, 24);
        Draw.roundBorder(px + 8, py + H - 34, SIDE - 16, 24, 6, hov ? Theme.ctlHover : Theme.ctl, hov ? Draw.ACCENT : Theme.outline);
        
        Draw.track("EDIT HUD LAYOUT", px + SIDE / 2f, py + H - 25.5f, 7, 1.2f, TEXT);
    }

    private void drawGrid(double mx, double my) {
        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        Draw.uiB(search.isEmpty() ? (category == null ? "All Mods" : category.label) : "Search", x0, y0 + 1, 13, TEXT);

        // search box
        int sw = 120, sx = x0 + w - sw;
        Draw.roundBorder(sx, y0, sw, 18, 5, Theme.card, searching ? Draw.ACCENT : Theme.outline);
        String shown = search.isEmpty() && !searching ? "Search mods..." : Draw.fit(search, 9, sw - 18) + (searching && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        Draw.ui(shown, sx + 7, y0 + 4.5f, 9, search.isEmpty() && !searching ? (Theme.glass ? 0xFFA9B1C4 : 0xFF5C6475) : TEXT);

        List<Module> list = visible();
        int top = y0 + 30, viewH = H - 44 - 30;
        int cw = (w - GAP * 2) / 3;
        int rows = (list.size() + 2) / 3;
        float max = Math.max(0, rows * (CARD_H + GAP) - GAP - viewH);
        scrollTo = Math.max(0, Math.min(max, scrollTo));
        scroll = Draw.lerp(scroll, scrollTo, 0.3f);

        Module hovered = null;
        if (list.isEmpty()) Draw.uiCenter("No mods found", x0 + w / 2f, top + 40, 10, MUTED);

        Draw.scissor(x0 - 2, top, w + 4, viewH);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < top || cy > top + viewH) continue;
            boolean inView = my >= top && my < top + viewH;
            boolean hov = inView && Draw.in(mx, my, cx, cy, cw, CARD_H);
            float h = anim(m, hov ? 1 : 0);
            if (Theme.glass) Draw.roundBorder(cx, cy, cw, CARD_H, Theme.radius, Draw.mix(Theme.card, Theme.cardHover, h), Theme.edge);
            else Draw.roundRect(cx, cy, cw, CARD_H, Theme.radius, Draw.mix(Theme.card, Theme.cardHover, h));
            if (hov) hovered = m;
            boolean gear = !m.settings.isEmpty();
            float titleY = cy + (CARD_H - (gear ? 34 : 17)) / 2f - 4.5f, titleW = Draw.uiWidth(m.name, 11), room = cw - 12;
            int titleColor = Draw.mix(0xFFC9CFDB, 0xFFFFFFFF, h);
            if (titleW <= room) Draw.uiCenter(m.name, cx + cw / 2f, titleY, 11, titleColor);
            else {
                // Too long for the card: slide it left and right inside the card instead of cutting it off.
                Draw.scissor(cx + 6, cy, room, CARD_H - 17);
                Draw.ui(m.name, cx + 6 - (titleW - room) * slide(titleW - room), titleY, 11, titleColor);
                Draw.endScissor();
            }
            if (gear) {
                float oy = cy + CARD_H - 34;
                boolean oh = inView && Draw.in(mx, my, cx, oy, cw, 16);
                Draw.rect(cx, oy, cw, 16, oh ? Theme.barHover : Theme.bar);
                Draw.track("OPTIONS", cx + cw / 2f, oy + 4.5f, 7, 1.6f, oh ? 0xFFFFFFFF : 0xFFD5DAE4);
            }
            float by = cy + CARD_H - 17;
            float e = anim(m.name + "#on", m.isEnabled() ? 1 : 0);
            boolean bh = inView && Draw.in(mx, my, cx, by, cw, 17);
            int bar = Draw.mix(bh ? 0xFFC8405C : RED, bh ? 0xFF38D283 : GREEN, e);
            Draw.roundRect(cx, by, cw, 17, Theme.radius, bar);
            Draw.rect(cx, by, cw, Theme.radius + 1, bar);
            Draw.track(m.isEnabled() ? "ENABLED" : "DISABLED", cx + cw / 2f, by + 5, 7, 1.6f, 0xFFFFFFFF);
        }
        Draw.endScissor();
        Draw.rect(x0, py + H - 24, w, 1, Theme.line);
        Draw.ui(Draw.fit(hovered != null ? hovered.name + "  -  " + hovered.description : "Right-click a mod to open its settings.", 8, w), x0, py + H - 17, 8, hovered != null ? 0xFFB3BACB : (Theme.glass ? 0xFFA9B1C4 : 0xFF5C6475));

        if (max > 0) {
            float trackH = viewH, barH = Math.max(18, trackH * viewH / (viewH + max));
            Draw.roundRect(px + W - 8, top + (trackH - barH) * (scroll / max), 3, barH, 1.5f, Theme.track);
        }
    }

    /** Height of the client settings block at the top of the Settings tab. */
    private int clientH() { return 26 + 4 + Client.shown.size() * ROW + 10; }

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

    private void drawSettings(double mx, double my) {
        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        boolean bh = Draw.in(mx, my, x0, y0, 44, 18);
        Draw.roundRect(x0, y0, 44, 18, 5, bh ? Theme.ctlHover : Theme.card);
        Draw.uiCenter("< Back", x0 + 22, y0 + 4.5f, 9, TEXT);
        Draw.uiB(open.name, x0 + 54, y0 + 1, 13, TEXT);

        // master toggle
        float e = anim(open.name + "#on", open.isEnabled() ? 1 : 0);
        float tx = x0 + w - 64;
        Draw.roundRect(tx, y0, 64, 18, 5, Draw.mix(RED, GREEN, e));
        Draw.track(open.isEnabled() ? "ENABLED" : "DISABLED", tx + 32, y0 + 5.5f, 7, 1.2f, 0xFFFFFFFF);

        Draw.ui(Draw.fit(open.description, 8, w), x0, y0 + 26, 8, MUTED);

        int top = y0 + 42, viewH = H - 56 - 14;
        float max = Math.max(0, open.settings.size() * ROW + (open instanceof HudModule ? ROW : 0) - viewH);
        settingsScrollTo = Math.max(0, Math.min(max, settingsScrollTo));
        settingsScroll = Draw.lerp(settingsScroll, settingsScrollTo, 0.3f);

        if (dragging != null) {
            dragging.set(dragging.min + (dragging.max - dragging.min) * Math.max(0, Math.min(1, (mx - dragX) / dragW)));
        }

        Draw.scissor(x0 - 2, top, w + 4, viewH);
        float y = top - settingsScroll;
        y = drawRows(open.settings, x0, y, w, mx, my);
        if (open instanceof HudModule) {
            Draw.roundRect(x0, y, w, ROW - 4, 5, Theme.card);
            Draw.ui("Position & size", x0 + 10, y + 6.5f, 9, TEXT);
            boolean hov = Draw.in(mx, my, x0 + w - 120, y + 3, 110, 16);
            Draw.roundRect(x0 + w - 120, y + 3, 110, 16, 4, hov ? Draw.ACCENT : Theme.ctl);
            Draw.uiCenter("Open HUD Editor", x0 + w - 65, y + 7, 8, TEXT);
        }
        Draw.endScissor();
    }

    /** Draws one row per setting. @return the y below the last row. */
    private float drawRows(List<? extends Setting> list, int x0, float y, int w, double mx, double my) {
        for (Setting st : list) {
            Draw.roundRect(x0, y, w, ROW - 4, 5, Theme.card);
            Draw.ui(st.name, x0 + 10, y + 6.5f, 9, TEXT);
            float rx = x0 + w - 10;
            if (st instanceof Setting.Bool) {
                Setting.Bool b = (Setting.Bool) st;
                float a = anim(st, b.value ? 1 : 0);
                Draw.roundRect(rx - 28, y + 5, 28, 12, 6, Draw.mix(Theme.track, Draw.ACCENT, a));
                Draw.roundRect(rx - 26 + a * 16, y + 7, 8, 8, 4, 0xFFFFFFFF);
            } else if (st instanceof Setting.Num) {
                Setting.Num n = (Setting.Num) st;
                float sw = 110, sx = rx - sw;
                float f = (float) ((n.value - n.min) / (n.max - n.min));
                Draw.rect(sx, y + 10, sw, 2, Theme.track);
                Draw.rect(sx, y + 10, sw * f, 2, Draw.ACCENT);
                Draw.roundRect(sx + sw * f - 4, y + 7, 8, 8, 4, 0xFFFFFFFF);
                String v = n.display();
                Draw.ui(v, sx - 8 - Draw.uiWidth(v, 8), y + 7, 8, MUTED);
            } else if (st instanceof Setting.Mode) {
                Setting.Mode m = (Setting.Mode) st;
                float bw = 110, bx = rx - bw;
                boolean hov = Draw.in(mx, my, bx, y + 3, bw, 16);
                Draw.roundRect(bx, y + 3, bw, 16, 4, hov ? Theme.ctlHover : Theme.ctl);
                Draw.ui("<", bx + 6, y + 6.5f, 9, MUTED);
                Draw.ui(">", bx + bw - 11, y + 6.5f, 9, MUTED);
                Draw.uiCenter(Draw.fit(m.get(), 8, bw - 28), bx + bw / 2f, y + 7, 8, TEXT);
            } else if (st instanceof Setting.Key) {
                Setting.Key k = (Setting.Key) st;
                float bw = 110, bx = rx - bw;
                boolean wait = binding == k, hov = Draw.in(mx, my, bx, y + 3, bw, 16);
                Draw.roundRect(bx, y + 3, bw, 16, 4, wait ? Draw.ACCENT : hov ? Theme.ctlHover : Theme.ctl);
                Draw.uiCenter(wait ? "Press a key..." : k.key.equals("NONE") ? "Not set" : k.key, bx + bw / 2f, y + 7, 8, TEXT);
            } else if (st instanceof Setting.Text) {
                Setting.Text ts = (Setting.Text) st;
                float bw = 150, bx = rx - bw;
                boolean focus = editing == ts;
                Draw.roundBorder(bx, y + 3, bw, 16, 4, Theme.ctl, focus ? Draw.ACCENT : Theme.outline);
                String shown = ts.value;
                while (shown.length() > 0 && Draw.uiWidth(shown, 8) > bw - 14) shown = shown.substring(1);
                if (focus && System.currentTimeMillis() / 500 % 2 == 0) shown += "|";
                if (shown.isEmpty() && !focus) Draw.ui("Click to type...", bx + 6, y + 7, 8, (Theme.glass ? 0xFFA9B1C4 : 0xFF5C6475));
                else Draw.ui(shown, bx + 6, y + 7, 8, TEXT);
            } else if (st instanceof Setting.Col) {
                Setting.Col c = (Setting.Col) st;
                int n = Setting.Col.PALETTE.length;
                float sx = rx - (n + 1) * 14 + 2;
                for (int i = 0; i <= n; i++) {
                    float cx = sx + i * 14;
                    boolean sel = i == n ? c.chroma : (!c.chroma && c.color == Setting.Col.PALETTE[i]);
                    Draw.roundRect(cx - 1, y + 4, 14, 14, 4, sel ? 0xFFFFFFFF : Theme.outline);
                    if (i == n) Draw.rainbow(cx + 1, y + 6, 10, 10);
                    else Draw.roundRect(cx + (sel ? 1 : 0), y + 5 + (sel ? 1 : 0), sel ? 10 : 12, sel ? 10 : 12, 3, Setting.Col.PALETTE[i]);
                }
            }
            y += ROW;
        }
        return y;
    }

    /** @return true if the click landed on one of the rows. */
    private boolean clickRows(List<? extends Setting> list, int x0, float sy, int w, double mx, double my, int button) {
        float rx = x0 + w - 10;
        for (Setting st : list) {
            if (my >= sy && my < sy + ROW - 4) {
                if (st instanceof Setting.Bool b) {
                    if (mx >= rx - 34) b.value = !b.value;
                } else if (st instanceof Setting.Num n) {
                    if (mx >= rx - 116 && mx <= rx + 6) { dragging = n; dragX = rx - 110; dragW = 110; slide(mx); }
                } else if (st instanceof Setting.Mode m) {
                    if (mx >= rx - 110 && mx <= rx) m.cycle(button == RIGHT || mx < rx - 55 ? -1 : 1);
                } else if (st instanceof Setting.Key k) {
                    if (mx >= rx - 110) binding = k;
                } else if (st instanceof Setting.Text t) {
                    if (mx >= rx - 150) editing = t;
                } else if (st instanceof Setting.Col c) {
                    int n = Setting.Col.PALETTE.length;
                    float sx = rx - (n + 1) * 14 + 2;
                    int idx = (int) Math.floor((mx - sx) / 14f);
                    if (mx >= sx && idx >= 0 && idx <= n) {
                        if (idx == n) c.chroma = true; else { c.chroma = false; c.color = Setting.Col.PALETTE[idx]; }
                    }
                }
                return true;
            }
            sy += ROW;
        }
        return false;
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
        int y = py + 52;
        for (int i = -1; i < Category.values().length; i++) {
            if (i >= 0 && Lumen.modules.of(Category.values()[i]).isEmpty()) continue;
            if (Draw.in(mx, my, px + 8, y, SIDE - 16, 18)) {
                category = i < 0 ? null : Category.values()[i];
                open = null; search = ""; scrollTo = 0; profiles = false; if (store) { store = false; StorePage.closed(); }
                return true;
            }
            y += 20;
        }
        if (Draw.in(mx, my, px + 8, py + H - 88, SIDE - 16, 22)) {
            store = !store; open = null; profiles = false;
            if (store) StorePage.opened(); else StorePage.closed();
            return true;
        }
        if (Draw.in(mx, my, px + 8, py + H - 62, SIDE - 16, 22)) { if (store) { store = false; StorePage.closed(); } profiles = !profiles; open = null; profileNote = ""; settingsScrollTo = settingsScroll = 0; Client.sync(); return true; }
        if (store) { StorePage.click(mx, my, button, px + SIDE + 14, py + 14, W - SIDE - 28, H - 28); return true; }
        if (profiles) {
            int sx0 = px + SIDE + 14, sw = W - SIDE - 28, stop = py + 14 + 26;
            if (my >= stop && my < py + H - 8) {
                if (clickRows(Client.shown, sx0, stop + 4 - settingsScroll, sw, mx, my, button)) Client.apply(); else clickProfiles(mx, my);
            }
            return true;
        }
        if (Draw.in(mx, my, px + 8, py + H - 34, SIDE - 16, 24)) { minecraft.gui.setScreen(new HudEditorScreen()); return true; }

        int x0 = px + SIDE + 14, y0 = py + 14, w = W - SIDE - 28;
        if (open == null) {
            if (Draw.in(mx, my, x0 + w - 120, y0, 120, 18)) { searching = true; return true; }
            int top = y0 + 30, viewH = H - 44 - 30;
            if (my < top || my >= top + viewH) return true;
            int cw = (w - GAP * 2) / 3;
            List<Module> list = visible();
            for (int i = 0; i < list.size(); i++) {
                Module m = list.get(i);
                float cx = x0 + (i % 3) * (cw + GAP), cy = top + (i / 3) * (CARD_H + GAP) - scroll;
                if (!Draw.in(mx, my, cx, cy, cw, CARD_H)) continue;
                boolean gear = !m.settings.isEmpty();
                if (gear && (button == RIGHT || Draw.in(mx, my, cx, cy + CARD_H - 34, cw, 16))) { open = m; settingsScrollTo = settingsScroll = 0; }
                else if (Draw.in(mx, my, cx, cy + CARD_H - 17, cw, 17)) m.toggle();
                return true;
            }
            return true;
        }

        if (Draw.in(mx, my, x0, y0, 44, 18)) { open = null; return true; }
        if (Draw.in(mx, my, x0 + w - 64, y0, 64, 18)) { open.toggle(); return true; }
        int top = y0 + 42, viewH = H - 56 - 14;
        if (my < top || my >= top + viewH) return true;
        float sy = top - settingsScroll;
        float rx = x0 + w - 10;
        if (clickRows(open.settings, x0, sy, w, mx, my, button)) return true;
        sy += open.settings.size() * ROW;
        if (open instanceof HudModule && my >= sy && my < sy + ROW - 4 && mx >= x0 + w - 120) minecraft.gui.setScreen(new HudEditorScreen());
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
        if (store) StorePage.wheel(step); else if (open == null && !profiles) scrollTo += step; else settingsScrollTo += step;
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
        if (open != null || profiles || store) return true;
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
        if (open != null || profiles || store) return false;
        if (search.length() < 24) { search += c; searching = true; scrollTo = 0; }
        return true;
    }
}
