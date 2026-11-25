package dev.lumen.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Small immediate-mode drawing helpers shared by the HUD and the menus. */
public final class R {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final int ACCENT = 0xFF4C8DFF;

    private static void begin(int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f);
    }

    private static void end() {
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void reset() {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void rect(float x, float y, float w, float h, int color) {
        begin(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y); GL11.glVertex2f(x, y + h); GL11.glVertex2f(x + w, y + h); GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        end();
    }

    private static void arc(float cx, float cy, float r, int from) {
        for (int i = 0; i <= 6; i++) {
            double a = Math.toRadians(from - i * 15);
            GL11.glVertex2d(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
    }

    private static void roundPath(float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w, h) / 2f);
        arc(x + r, y + r, r, 270);
        arc(x + r, y + h - r, r, 180);
        arc(x + w - r, y + h - r, r, 90);
        arc(x + w - r, y + r, r, 360);
    }

    /** While true, everything "rounded" is drawn with square corners (the menu's Corners setting). Only the menu sets it, and clears it when it is done. */
    public static boolean square;

    public static void roundRect(float x, float y, float w, float h, float r, int color) {
        if (w <= 0 || h <= 0) return;
        if (square) { rect(x, y, w, h, color); return; }
        begin(color);
        GL11.glBegin(GL11.GL_POLYGON);
        roundPath(x, y, w, h, r);
        GL11.glEnd();
        end();
    }

    public static void roundOutline(float x, float y, float w, float h, float r, float lineWidth, int color) {
        if (square) r = 0.01f;
        begin(color);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        roundPath(x, y, w, h, r);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1f);
        end();
    }

    /** A filled convex polygon; points as x, y pairs, clockwise on screen. */
    public static void poly(int color, float... xy) {
        begin(color);
        GlStateManager.disableCull(); // callers need not care which way round their points go
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < xy.length; i += 2) GL11.glVertex2f(xy[i], xy[i + 1]);
        GL11.glEnd();
        GlStateManager.enableCull();
        end();
    }

    public static void circle(float cx, float cy, float r, int color) {
        begin(color);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < 24; i++) {
            double a = -Math.PI * 2 * i / 24;
            GL11.glVertex2d(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        GL11.glEnd();
        end();
    }

    private static void vcolor(int c) { GL11.glColor4f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, (c >>> 24) / 255f); }

    /** Rectangle with a vertical gradient. */
    public static void gradient(float x, float y, float w, float h, int top, int bottom) {
        begin(top);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.resetColor();
        GL11.glBegin(GL11.GL_QUADS);
        vcolor(top); GL11.glVertex2f(x, y);
        vcolor(bottom); GL11.glVertex2f(x, y + h); GL11.glVertex2f(x + w, y + h);
        vcolor(top); GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.resetColor();
        end();
    }

    /** Soft additive glow: opaque-ish in the centre, fading to nothing at the radius. */
    public static void glow(float cx, float cy, float r, int color) {
        begin(color);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.resetColor();
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        vcolor(color); GL11.glVertex2f(cx, cy);
        vcolor(color & 0xFFFFFF);
        for (int i = 0; i <= 40; i++) {
            double a = -Math.PI * 2 * i / 40;
            GL11.glVertex2d(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.resetColor();
        end();
    }

    /** Text floating in the world at (x, y, z), facing the camera. Depth-tested, so walls hide it. */
    public static void worldLabel(String s, double x, double y, double z, int color) {
        net.minecraft.client.renderer.entity.RenderManager rm = mc.getRenderManager();
        net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) (x - rm.viewerPosX), (float) (y - rm.viewerPosY), (float) (z - rm.viewerPosZ));
        GL11.glNormal3f(0, 1, 0);
        GlStateManager.rotate(-rm.playerViewY, 0, 1, 0);
        GlStateManager.rotate(rm.playerViewX, 1, 0, 0);
        GlStateManager.scale(-0.03f, -0.03f, 0.03f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        int w = fr.getStringWidth(s) / 2;
        rect(-w - 2, -1, w * 2 + 4, 10, 0x60000000);
        GlStateManager.enableBlend();
        fr.drawString(s, -w, 0, color);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    /** Horizontal rainbow strip, used for the chroma swatch. */
    public static void rainbow(float x, float y, float w, float h) {
        int n = 8;
        for (int i = 0; i < n; i++) {
            int c = 0xFF000000 | java.awt.Color.HSBtoRGB(i / (float) n, 0.8f, 1f);
            rect(x + w * i / n, y, w / n + 0.5f, h, c);
        }
    }

    public static void mcText(String s, float x, float y, int color, boolean shadow) {
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        mc.fontRendererObj.drawString(s, x, y, color, shadow);
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void scissor(float x, float y, float w, float h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * f), (int) ((sr.getScaledHeight() - (y + h)) * f), (int) (w * f), (int) (h * f));
    }

    public static void endScissor() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }

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

    public static boolean in(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
