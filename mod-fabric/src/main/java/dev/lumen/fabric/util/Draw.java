package dev.lumen.fabric.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Drawing helpers on top of the game's GUI extractor. Everything goes through the same calls
 * vanilla uses for its own HUD, so it works unchanged with Sodium and with 26.2 / 26.3.
 */
public final class Draw {
    public static final int ACCENT = 0xFF4C8DFF;
    private static final Minecraft mc = Minecraft.getInstance();
    private static GuiGraphicsExtractor g;
    private static Style regular, bold;
    /** Menu font choice: false = smooth (Poppins), true = Minecraft font. */
    public static boolean minecraftFont;

    /** Must be called with the current extractor before any other method in a frame. */
    public static void begin(GuiGraphicsExtractor graphics) { g = graphics; uiScale = 1f; square = false; }
    public static GuiGraphicsExtractor g() { return g; }

    public static void push(float x, float y, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
    }
    public static void pop() { g.pose().popMatrix(); }

    public static void rect(float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        float p = pixelsPerUnit();
        int ux = Math.round(x), uy = Math.round(y), ux2 = Math.round(x + w), uy2 = Math.round(y + h);
        if (p == Math.rint(p)) { g.fill(ux, uy, ux2, uy2, color); return; }
        // Same pixel snapping as roundRect, so straight and rounded pieces always line up.
        g.pose().pushMatrix();
        g.pose().scale(1f / p, 1f / p);
        g.fill(Math.round(ux * p), Math.round(uy * p), Math.round(ux2 * p), Math.round(uy2 * p), color);
        g.pose().popMatrix();
    }

    /** Rounded rectangle. The corners are stepped per screen pixel, not per GUI unit, so they stay round when the GUI is scaled up. */
    /** While true, everything "rounded" is drawn with square corners (the menu's Corners setting). Reset by begin(), so only the menu that sets it is affected. */
    public static boolean square;

    public static void roundRect(float fx, float fy, float fw, float fh, float fr, int color) {
        if (square) fr = 0;
        int ux = Math.round(fx), uy = Math.round(fy), uw = Math.round(fw), uh = Math.round(fh);
        if (uw <= 0 || uh <= 0 || (color >>> 24) == 0) return;
        float p = pixelsPerUnit();
        int x = Math.round(ux * p), y = Math.round(uy * p), w = Math.round((ux + uw) * p) - x, h = Math.round((uy + uh) * p) - y;
        int r = Math.max(0, Math.min(Math.round(Math.round(fr) * p), Math.min(w, h) / 2));
        g.pose().pushMatrix();
        g.pose().scale(1f / p, 1f / p);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        g.pose().popMatrix();
    }

    /** One pixel outline with clipped corners. */
    public static void outline(float fx, float fy, float fw, float fh, int color) {
        int x = Math.round(fx), y = Math.round(fy), w = Math.round(fw), h = Math.round(fh);
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + 1, y + h - 2, color);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, color); g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color); g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }

    /** Minecraft-font text (used by the HUD). */
    public static void text(String s, float x, float y, int color, boolean shadow) {
        if ((color >>> 24) < 8) return;
        g.text(mc.font, s, Math.round(x), Math.round(y), color, shadow);
    }

    public static void item(ItemStack stack, int x, int y, boolean decorations) {
        g.item(stack, x, y);
        if (decorations) g.itemDecorations(mc.font, stack, x, y);
    }

    public static void scissor(float x, float y, float w, float h) { g.enableScissor(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h)); }
    public static void endScissor() { g.disableScissor(); }

    // ---- menu font (Poppins when the mod's resources are loaded, otherwise the Minecraft font)

    /**
     * The font is registered once per raster density. Each piece of text is drawn with the density closest
     * to the pixels it will really cover, and its size is nudged (a few percent at most) so one glyph texel
     * lands on exactly one screen pixel. That keeps it sharp at every GUI scale and window size.
     */
    private static final float[] DENSITY = {0.8f, 0.92f, 1.058f, 1.217f, 1.399f, 1.609f, 1.85f, 2.128f, 2.447f, 2.814f, 3.236f, 3.722f, 4.28f, 4.922f, 5.661f, 6.51f, 7.486f, 8.609f, 9.9f, 11.385f, 13.093f};
    private static Style[] regularAt, boldAt;
    /** Extra scale the current screen draws with (set after {@link #begin}); 1 for everything else. */
    public static float uiScale = 1f;

    private static void styles() {
        if (regular != null) return;
        regular = Style.EMPTY; bold = Style.EMPTY.withBold(true);
        try {
            if (mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("lumen", "font/ui_00.json")).isPresent()) {
                regularAt = new Style[DENSITY.length]; boldAt = new Style[DENSITY.length];
                for (int i = 0; i < DENSITY.length; i++) {
                    regularAt[i] = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("lumen", String.format("ui_%02d", i))));
                    boldAt[i] = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("lumen", String.format("uib_%02d", i))));
                }
                regular = regularAt[0];
            }
        } catch (Throwable ignored) { regularAt = null; regular = Style.EMPTY; }
    }

    private static float pixelsPerUnit() { return Math.max(0.25f, (float) mc.getWindow().getGuiScale() * uiScale); }

    private static int level(float size) {
        float want = size / 9f * pixelsPerUnit();
        int best = 0;
        double bestErr = Double.MAX_VALUE;
        for (int i = 0; i < DENSITY.length; i++) {
            double err = Math.abs(Math.log(DENSITY[i] / want));
            if (err < bestErr) { bestErr = err; best = i; }
        }
        return best;
    }

    private static boolean smooth() { styles(); return !minecraftFont && regularAt != null; }
    /** Scale to draw a 9-unit-tall font at so it comes out at {@code size}. */
    private static float k(float size) { return smooth() ? DENSITY[level(size)] / pixelsPerUnit() : size / 9f; }

    private static Component comp(String s, boolean b, float size) {
        if (!smooth()) return minecraftFont || !b ? Component.literal(s) : Component.literal(s).withStyle(bold);
        int l = level(size);
        return Component.literal(s).withStyle(b ? boldAt[l] : regularAt[l]);
    }

    /** True when the smooth font is available (mod resources are loaded by a resource loader, e.g. the one bundled with Sodium). */
    public static boolean smoothAvailable() { styles(); return regularAt != null; }

    public static float uiWidth(String s, float size, boolean b) { return mc.font.width(comp(s, b, size)) * k(size); }
    public static float uiWidth(String s, float size) { return uiWidth(s, size, false); }

    public static void ui(String s, float x, float y, float size, int color, boolean b) {
        if ((color >>> 24) < 8 || s.isEmpty()) return;
        float k = k(size);
        push(x, y + (size - 9f * k) * 0.5f, k);
        g.text(mc.font, comp(s, b, size), 0, 0, color, false);
        pop();
    }
    public static void ui(String s, float x, float y, float size, int color) { ui(s, x, y, size, color, false); }
    public static void uiB(String s, float x, float y, float size, int color) { ui(s, x, y, size, color, true); }
    public static void uiCenter(String s, float cx, float y, float size, int color) { ui(s, cx - uiWidth(s, size) / 2f, y, size, color, false); }
    public static void uiCenterB(String s, float cx, float y, float size, int color) { ui(s, cx - uiWidth(s, size, true) / 2f, y, size, color, true); }

    /** Bold, letter-spaced, centred text (button labels). */
    public static void track(String s, float cx, float y, float size, float spacing, int color) {
        float total = -spacing;
        float[] w = new float[s.length()];
        for (int i = 0; i < s.length(); i++) { w[i] = uiWidth(String.valueOf(s.charAt(i)), size, true); total += w[i] + spacing; }
        float pen = cx - total / 2f;
        for (int i = 0; i < s.length(); i++) { ui(String.valueOf(s.charAt(i)), pen, y, size, color, true); pen += w[i] + spacing; }
    }

    public static String fit(String s, float size, float maxW) {
        if (uiWidth(s, size) <= maxW) return s;
        while (s.length() > 1 && uiWidth(s + "...", size) > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    /** Applies a rotation to a world-space pose stack (same call on every supported game version). */
    public static void rotate(com.mojang.blaze3d.vertex.PoseStack ps, org.joml.Quaternionfc q) { ps.mulPose(new org.joml.Matrix4f().rotation(q)); }

    // ---- colour helpers

    /** Opaque rounded panel with a one pixel border. */
    /** A filled rounded rectangle with a one-unit outline. The two never overlap, so translucent colours stay even. */
    public static void roundBorder(float x, float y, float w, float h, float r, int fill, int border) {
        roundRect(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
        roundOutline(x, y, w, h, r, border);
    }

    /** One-unit rounded outline: four straight strips plus the stepped corners. */
    public static void roundOutline(float fx, float fy, float fw, float fh, float fr, int color) {
        if (square) fr = 0;
        int ux = Math.round(fx), uy = Math.round(fy), uw = Math.round(fw), uh = Math.round(fh);
        if (uw <= 2 || uh <= 2 || (color >>> 24) == 0) return;
        float p = pixelsPerUnit();
        int x = Math.round(ux * p), y = Math.round(uy * p), w = Math.round((ux + uw) * p) - x, h = Math.round((uy + uh) * p) - y;
        int t = Math.max(1, Math.round(p));
        int r = Math.max(t, Math.min(Math.round(Math.round(fr) * p), Math.min(w, h) / 2));
        int ri = r - t;
        g.pose().pushMatrix();
        g.pose().scale(1f / p, 1f / p);
        g.fill(x + r, y, x + w - r, y + t, color);
        g.fill(x + r, y + h - t, x + w - r, y + h, color);
        g.fill(x, y + r, x + t, y + h - r, color);
        g.fill(x + w - t, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int outer = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            int inner = i < t ? r : t + ri - (int) Math.round(Math.sqrt(Math.max(0, ri * ri - dy * dy)));
            inner = Math.max(outer + 1, Math.min(r, inner));
            g.fill(x + outer, y + i, x + inner, y + i + 1, color);
            g.fill(x + w - inner, y + i, x + w - outer, y + i + 1, color);
            g.fill(x + outer, y + h - i - 1, x + inner, y + h - i, color);
            g.fill(x + w - inner, y + h - i - 1, x + w - outer, y + h - i, color);
        }
        g.pose().popMatrix();
    }

    public static void rainbow(float x, float y, float w, float h) {
        for (int i = 0; i < (int) w; i++) rect(x + i, y, 1, h, 0xFF000000 | java.awt.Color.HSBtoRGB(i / w, 0.8f, 1f));
    }

    public static int alpha(int color, float a) {
        int al = (int) ((color >>> 24) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int s = 0; s <= 24; s += 8) {
            int ca = a >>> s & 255, cb = b >>> s & 255;
            r |= ((int) (ca + (cb - ca) * t) & 255) << s;
        }
        return r;
    }

    public static boolean in(double mx, double my, float x, float y, float w, float h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    public static float lerp(float cur, float target, float speed) {
        float d = target - cur;
        return Math.abs(d) < 0.001f ? target : cur + d * Math.min(1f, speed);
    }
}
