package dev.lumen.fabric.core;

import dev.lumen.fabric.util.Draw;

/** A module that draws a movable, scalable element on the in-game HUD. */
public abstract class HudModule extends Module {
    public static final int START = 0, CENTER = 1, END = 2;

    /** Which screen edge (or the centre) the element is attached to, and its pixel offset from it. */
    public int ax, ay;
    public float ox, oy;
    public float scale = 1f;

    protected HudModule(String name, String description, int ax, float ox, int ay, float oy) { this(name, description, Category.HUD, ax, ox, ay, oy); }

    /** For HUD elements that belong in another tab of the menu (e.g. Hypixel). */
    protected HudModule(String name, String description, Category category, int ax, float ox, int ay, float oy) {
        super(name, description, category);
        this.ax = ax; this.ox = ox; this.ay = ay; this.oy = oy;
        if (hasLumenStyle()) { lumenHud = new Setting.Bool("Noctua HUD", true); settings.add(lumenHud); }
    }

    /** The switch between the Noctua look (soft dark panel, Noctua's own letters) and the classic one. */
    private Setting.Bool lumenHud;
    /** True while the element paints the Noctua panel itself, so the optional one behind it is left out. */
    protected boolean ownPanel() { return false; }
    protected boolean panelOn() { return panel == null || panel.on(); }
    /** How far below the given y the capitals of Noctua's letters start and end, as a share of the size. */
    protected static final float CAP_TOP = 0f, CAP_MID = 0.38f; // measured from the drawn letters
    /** The y that puts a line of letters in the middle of a box. */
    protected static float mid(float top, float h, float size) { return top + h / 2f - CAP_MID * size; }
    /** The y that puts the top of the capitals at this height. */
    protected static float cap(float inkTop, float size) { return inkTop - CAP_TOP * size; }
    /** Elements that only have one look say no. */
    protected boolean hasLumenStyle() { return true; }
    protected boolean lumen() { return lumenHud != null && lumenHud.on(); }
    /** The Noctua panel: the one Direction HUD sits on. */
    protected static void panel(float x, float y, float w, float h) {
        Draw.roundRect(x, y, w, h, 6, 0x78000000);
    }
    /** The box behind an element, in whichever look is chosen. */
    protected void box(float x, float y, float w, float h) { if (lumen()) panel(x, y, w, h); else Draw.roundRect(x, y, w, h, 3, 0x70000000); }

    /** Set by elements that had no box of their own: an optional panel behind them. */
    private Setting.Bool panel;
    protected void optionalBackground() { panel = bool("Background", false); }
    /** Whether the element is drawing anything right now (the panel is hidden with it). */
    protected boolean showing(boolean editing) { return true; }

    public abstract float width();
    public abstract float height();
    /** Draw at the origin. {@code editing} is true inside the HUD editor (show placeholder data). */
    public abstract void draw(boolean editing);

    private static float resolve(int anchor, float off, float screen, float size) {
        float v = anchor == START ? off : anchor == CENTER ? screen / 2f + off : screen - off - size;
        return Math.max(0, Math.min(Math.max(0, screen - size), v));
    }

    public float px(int sw) { return resolve(ax, ox, sw, width() * scale); }
    public float py(int sh) { return resolve(ay, oy, sh, height() * scale); }

    /** Moves the element so its top-left corner is at (x, y), re-attaching it to the nearest edge. */
    public void moveTo(int sw, int sh, float x, float y) {
        float w = width() * scale, h = height() * scale, cx = x + w / 2f, cy = y + h / 2f;
        ax = cx < sw / 3f ? START : cx > sw * 2f / 3f ? END : CENTER;
        ay = cy < sh / 3f ? START : cy > sh * 2f / 3f ? END : CENTER;
        ox = ax == START ? x : ax == CENTER ? x - sw / 2f : sw - x - w;
        oy = ay == START ? y : ay == CENTER ? y - sh / 2f : sh - y - h;
    }

    public void render(int sw, int sh, boolean editing) {
        Draw.push(Math.round(px(sw)), Math.round(py(sh)), scale);
        if (panel != null && panel.on() && showing(editing) && !ownPanel()) box(-3, -3, width() + 6, height() + 6);
        try { draw(editing); } catch (Throwable ignored) {}
        Draw.pop();
    }
}
