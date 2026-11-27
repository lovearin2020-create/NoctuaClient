package dev.lumen.cosmetics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Random;

/** The capes that move: each is painted again for the moment it is shown at. Plain Java2D, shared by every game version. */
public final class CapeAnim {
    private CapeAnim() {}

    public static boolean is(String id) { return "cape_eclipse".equals(id) || "cape_starfall".equals(id) || "cape_nebula".equals(id); }

    private static Color rgba(int rgb, double a) { return new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (int) Math.max(0, Math.min(255, a))); }

    private static void glow(Graphics2D g, double x, double y, double r, int rgb, double alpha) {
        g.setPaint(new RadialGradientPaint((float) x, (float) y, (float) r, new float[]{0f, 1f}, new Color[]{rgba(rgb, alpha), rgba(rgb, 0)}));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }

    /** Stars that keep their places and fade in and out, each at its own pace. */
    private static void twinkle(Graphics2D g, int w, int h, double t, int seed, int n) {
        Random rnd = new Random(seed);
        for (int i = 0; i < n; i++) {
            double x = rnd.nextDouble() * w, y = rnd.nextDouble() * h, s = w * (0.006 + rnd.nextDouble() * 0.012);
            double a = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * (0.8 + rnd.nextDouble() * 2.2) + rnd.nextDouble() * 6.28));
            g.setColor(rgba(0xFFFFFF, 235 * a));
            g.fill(new Ellipse2D.Double(x - s, y - s, s * 2, s * 2));
        }
    }

    /** Paints one face (w by h) of the cape as it looks t seconds in. */
    public static void paint(Graphics2D g, String id, int w, int h, double t) {
        if ("cape_nebula".equals(id)) nebula(g, w, h, t);
        else if ("cape_starfall".equals(id)) starfall(g, w, h, t);
        else eclipse(g, w, h, t);
    }

    private static void nebula(Graphics2D g, int w, int h, double t) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x0A0620), 0, h, new Color(0x160B3A)));
        g.fillRect(0, 0, w, h);
        int[] cols = {0xFF4FA3, 0x7A5BFF, 0x2FD0E0, 0xFF8A3D};
        for (int i = 0; i < 4; i++) {
            double a = t * (0.35 + i * 0.09) + i * 1.7;
            double x = w * (0.5 + 0.38 * Math.sin(a)), y = h * (0.5 + 0.36 * Math.cos(a * 0.8 + i));
            glow(g, x, y, w * (0.75 + 0.12 * Math.sin(t * 0.6 + i)), cols[i], i == 3 ? 95 : 150);
        }
        twinkle(g, w, h, t, 71, 26);
    }

    private static void starfall(Graphics2D g, int w, int h, double t) {
        g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 0.65f, 1f}, new Color[]{new Color(0x04071A), new Color(0x0D1B4D), new Color(0x1E3A7A)}));
        g.fillRect(0, 0, w, h);
        twinkle(g, w, h, t, 19, 34);
        Random rnd = new Random(5);
        for (int i = 0; i < 4; i++) {
            double period = 1.9 + rnd.nextDouble() * 1.6, p = (t / period + rnd.nextDouble()) % 1.0;
            double sx = w * (0.15 + rnd.nextDouble() * 0.9), sy = -h * 0.1 + rnd.nextDouble() * h * 0.35;
            if (p > 0.55) continue; // a pause between falls
            double k = p / 0.55, x = sx - w * 0.9 * k, y = sy + h * 0.75 * k, len = w * 0.42;
            double fade = Math.sin(k * Math.PI);
            g.setStroke(new BasicStroke(Math.max(1f, w * 0.02f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setPaint(new GradientPaint((float) x, (float) y, rgba(0xFFFFFF, 240 * fade), (float) (x + len * 0.77), (float) (y - len * 0.64), rgba(0x9FC4FF, 0)));
            g.draw(new Line2D.Double(x, y, x + len * 0.77, y - len * 0.64));
            glow(g, x, y, w * 0.07, 0xFFFFFF, 220 * fade);
        }
    }

    private static void eclipse(Graphics2D g, int w, int h, double t) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x05060F), 0, h, new Color(0x1A1238)));
        g.fillRect(0, 0, w, h);
        twinkle(g, w, h, t, 43, 28);
        double cx = w * 0.5, cy = h * 0.34, r = w * 0.27;
        double phase = (t / 9.0) % 1.0;                 // the shadow crosses once every nine seconds
        double off = (phase * 2 - 1) * r * 2.3;         // from one side to the other
        double total = Math.max(0, 1 - Math.abs(off) / (r * 0.6)); // 1 when the shadow sits right on the moon
        glow(g, cx, cy, r * (2.1 + 0.7 * total), 0xF5C542, 70 + 150 * total);
        g.setColor(new Color(0xF4EFD8));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        java.awt.Shape old = g.getClip();
        g.setClip(new Ellipse2D.Double(cx - r - 1, cy - r - 1, r * 2 + 2, r * 2 + 2));
        g.setColor(new Color(0x07060F));
        g.fill(new Ellipse2D.Double(cx + off - r * 1.02, cy - r * 1.02, r * 2.04, r * 2.04));
        g.setClip(old);
        if (total > 0) {
            g.setStroke(new BasicStroke((float) (w * 0.018)));
            g.setColor(rgba(0xFFD86B, 255 * total));
            g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
        // a thin band of light across the lower half that swells at totality
        g.setPaint(new LinearGradientPaint(0, (float) (h * 0.62), 0, h, new float[]{0f, 1f}, new Color[]{rgba(0xF5C542, 0), rgba(0xF5C542, 40 + 90 * total)}));
        g.fillRect(0, (int) (h * 0.62), w, h);
    }
}
