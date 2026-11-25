package dev.lumen.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Menu font (Poppins, bundled under the SIL Open Font License). Glyphs are rasterised at the exact
 * size they occupy on the monitor and drawn snapped to whole screen pixels, so text is never
 * resampled. Can be switched to the Minecraft font from the menu; text containing characters the
 * bundled font does not have (e.g. Korean) is always drawn with the Minecraft font.
 */
public final class Fnt {
    /** Client setting: draw the menus with the Minecraft font instead of the smooth one. */
    public static boolean minecraft;

    public static final int REGULAR = 0, BOLD = 1, LOGO = 2, LIGHT = 3;
    private static final String[] FILES = {"Poppins-Medium.ttf", "Poppins-SemiBold.ttf", "Poppins-Bold.ttf", "Poppins-Medium.ttf"};
    private static final Font[] BASE = new Font[4];
    private static final boolean[] TRIED = new boolean[4];

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<Integer, Fnt> CACHE = new HashMap<Integer, Fnt>();
    private static final int FIRST = 32, LAST = 126, PAD = 3;
    /** Screen pixels per GUI unit, and the extra scale/centre the current screen draws with. */
    private static float dev = 2f, fit = 1f, cx, cy;

    private final int atlas;
    private DynamicTexture tex;
    private boolean failed;
    private final int[] gx = new int[LAST + 1], gy = new int[LAST + 1], gw = new int[LAST + 1];
    private int cellH;

    /** Call at the start of a screen's draw: {@code scale} is the extra scale applied around (cx, cy). */
    public static void transform(float scale, float centerX, float centerY) {
        dev = new ScaledResolution(mc).getScaleFactor();
        fit = scale; cx = centerX; cy = centerY;
    }

    private static Font base(int weight) {
        if (!TRIED[weight]) {
            TRIED[weight] = true;
            try {
                InputStream in = Fnt.class.getResourceAsStream("/assets/lumen/fonts/" + FILES[weight]);
                try { BASE[weight] = Font.createFont(Font.TRUETYPE_FONT, in); } finally { in.close(); }
            } catch (Throwable t) {
                BASE[weight] = new Font("SansSerif", weight == REGULAR || weight == LIGHT ? Font.PLAIN : Font.BOLD, 1);
            }
        }
        return BASE[weight];
    }

    private Fnt(int px, int weight) {
        atlas = px <= 20 ? 512 : px <= 46 ? 1024 : px <= 100 ? 2048 : 4096;
        try {
            Font font = base(weight).deriveFont((float) px);
            BufferedImage img = new BufferedImage(atlas, atlas, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setFont(font);
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            cellH = fm.getAscent() + fm.getDescent() + PAD * 2;
            int x = 0, y = 0;
            for (int c = FIRST; c <= LAST; c++) {
                int w = fm.charWidth((char) c);
                if (x + w + PAD * 2 > atlas) { x = 0; y += cellH; }
                g.drawString(String.valueOf((char) c), x + PAD, y + PAD + fm.getAscent());
                gx[c] = x; gy[c] = y; gw[c] = w;
                x += w + PAD * 2;
            }
            g.dispose();
            // Keep white under fully transparent texels so filtering can never darken glyph edges.
            int[] px2 = img.getRGB(0, 0, atlas, atlas, null, 0, atlas);
            for (int i = 0; i < px2.length; i++) px2[i] = (px2[i] & 0xFF000000) | 0xFFFFFF;
            img.setRGB(0, 0, atlas, atlas, px2, 0, atlas);
            tex = new DynamicTexture(img);
            GlStateManager.bindTexture(tex.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } catch (Throwable t) {
            failed = true;
        }
    }

    private static float k() { return Math.max(0.5f, dev * fit); }

    private static Fnt of(int size, int weight) {
        int px = Math.max(6, Math.round(size * k()));
        int key = px * 4 + weight;
        Fnt f = CACHE.get(key);
        if (f == null) {
            if (CACHE.size() > 48) {
                for (Fnt old : CACHE.values()) if (old.tex != null) old.tex.deleteGlTexture();
                CACHE.clear();
            }
            f = new Fnt(px, weight);
            CACHE.put(key, f);
        }
        return f;
    }

    private static boolean plain(String s) {
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c < FIRST || c > LAST) return false; }
        return true;
    }

    public static float width(String s, int size) { return width(s, size, REGULAR, 0); }
    public static float widthB(String s, int size) { return width(s, size, BOLD, 0); }
    public static float width(String s, int size, int weight, float spacing) {
        if (minecraft || !plain(s)) return mcWidth(s, size, weight == BOLD || weight == LOGO) + spacing * Math.max(0, s.length() - 1);
        return of(size, weight).w(s) + spacing * Math.max(0, s.length() - 1);
    }

    public static void draw(String s, float x, float y, int size, int color) { draw(s, x, y, size, REGULAR, 0, color, color); }
    public static void drawB(String s, float x, float y, int size, int color) { draw(s, x, y, size, BOLD, 0, color, color); }
    public static void center(String s, float cx, float y, int size, int color) { draw(s, cx - width(s, size) / 2f, y, size, color); }
    public static void centerB(String s, float cx, float y, int size, int color) { drawB(s, cx - widthB(s, size) / 2f, y, size, color); }

    /** Centred text with letter spacing (button labels). */
    public static void track(String s, float cx, float y, int size, int weight, float spacing, int color) {
        draw(s, cx - width(s, size, weight, spacing) / 2f, y, size, weight, spacing, color, color);
    }

    /** Full control: weight, extra letter spacing and a left-to-right colour gradient. */
    public static void draw(String s, float x, float y, int size, int weight, float spacing, int left, int right) {
        if (minecraft || !plain(s)) mcDraw(s, x, y, size, left, weight == BOLD || weight == LOGO, spacing);
        else of(size, weight).d(s, x, y, spacing, left, right);
    }

    /** Cuts the string with "..." so it fits into maxW. */
    public static String fit(String s, int size, float maxW) {
        if (width(s, size) <= maxW) return s;
        while (s.length() > 1 && width(s + "...", size) > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    // ---- Minecraft font mode (whole-number scales only, so the pixel font stays crisp)

    private static float mcScale(int size) { return size >= 36 ? 4f : size >= 22 ? 3f : size >= 15 ? 2f : 1f; }

    private static float mcWidth(String s, int size, boolean bold) {
        return mc.fontRendererObj.getStringWidth(bold ? EnumChatFormatting.BOLD + s : s) * mcScale(size);
    }

    private static void mcDraw(String s, float x, float y, int size, int color, boolean bold, float spacing) {
        float k = mcScale(size);
        if (spacing != 0) {
            float pen = x;
            for (int i = 0; i < s.length(); i++) {
                String ch = String.valueOf(s.charAt(i));
                mcDraw(ch, pen, y, size, color, bold, 0);
                pen += mcWidth(ch, size, bold) + spacing;
            }
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(snapX(x), snapY(y + (size - 8 * k) / 2f), 0);
        GlStateManager.scale(k, k, 1);
        R.mcText(bold ? EnumChatFormatting.BOLD + s : s, 0, 0, color, false);
        GlStateManager.popMatrix();
    }

    // ---- smooth font

    private static float snapX(float x) { return (Math.round(((x - cx) * fit + cx) * dev) / dev - cx) / fit + cx; }
    private static float snapY(float y) { return (Math.round(((y - cy) * fit + cy) * dev) / dev - cy) / fit + cy; }

    private float w(String s) {
        if (failed) return mcWidth(s, 9, false);
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += gw[s.charAt(i)];
        return w / k();
    }

    private static void vcolor(int c) { GL11.glColor4f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, (c >>> 24) / 255f); }

    private void d(String s, float x, float y, float spacing, int left, int right) {
        if (failed) { mcDraw(s, x, y, 9, left, false, spacing); return; }
        float inv = 1f / k();
        float total = Math.max(1f, w(s) + spacing * Math.max(0, s.length() - 1));
        boolean grad = left != right;
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.bindTexture(tex.getGlTextureId());
        if (grad) GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.resetColor();
        GL11.glBegin(GL11.GL_QUADS);
        vcolor(left);
        float start = snapX(x), penX = start - PAD * inv, top = snapY(y) - PAD * inv, h = cellH * inv;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int cw = gw[c] + PAD * 2;
            float w = cw * inv;
            float u0 = gx[c] / (float) atlas, v0 = gy[c] / (float) atlas;
            float u1 = (gx[c] + cw) / (float) atlas, v1 = (gy[c] + cellH) / (float) atlas;
            int cl = left, cr = left;
            if (grad) {
                cl = R.mix(left, right, (penX - start) / total);
                cr = R.mix(left, right, (penX + w - start) / total);
            }
            if (grad) vcolor(cl);
            GL11.glTexCoord2f(u0, v0); GL11.glVertex2f(penX, top);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex2f(penX, top + h);
            if (grad) vcolor(cr);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(penX + w, top + h);
            GL11.glTexCoord2f(u1, v0); GL11.glVertex2f(penX + w, top);
            penX += gw[c] * inv + spacing;
        }
        GL11.glEnd();
        if (grad) GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.resetColor();
        GlStateManager.color(1, 1, 1, 1);
    }
}
