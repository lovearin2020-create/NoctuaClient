package dev.lumen.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Random;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws the equipped Lumen cape on any player (yours, or another Lumen user's as reported by the backend). */
public class CapeLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final java.util.Map<String, ResourceLocation> textures = new java.util.HashMap<String, ResourceLocation>();

    private final RenderPlayer renderer;
    private final CapeModel model = new CapeModel();

    public CapeLayer(RenderPlayer renderer) { this.renderer = renderer; }

    private static final class CapeModel extends ModelBase {
        final ModelRenderer cape;
        CapeModel() {
            textureWidth = 64; textureHeight = 32;
            cape = new ModelRenderer(this, 0, 0);
            cape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1);
        }
    }

    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt, float age, float yaw, float pitch, float scale) {
        String id = dev.lumen.online.Online.wear(p, "cape");
        if (id == null || p.isInvisible()) return;
        ResourceLocation tex = texture(id);
        if (tex == null) return;
        GlStateManager.color(1, 1, 1, 1);
        renderer.bindTexture(tex);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.125F);
        double d0 = p.prevChasingPosX + (p.chasingPosX - p.prevChasingPosX) * pt - (p.prevPosX + (p.posX - p.prevPosX) * pt);
        double d1 = p.prevChasingPosY + (p.chasingPosY - p.prevChasingPosY) * pt - (p.prevPosY + (p.posY - p.prevPosY) * pt);
        double d2 = p.prevChasingPosZ + (p.chasingPosZ - p.prevChasingPosZ) * pt - (p.prevPosZ + (p.posZ - p.prevPosZ) * pt);
        float f = p.prevRenderYawOffset + (p.renderYawOffset - p.prevRenderYawOffset) * pt;
        double sin = MathHelper.sin(f * (float) Math.PI / 180.0F);
        double cos = -MathHelper.cos(f * (float) Math.PI / 180.0F);
        float lift = MathHelper.clamp_float((float) d1 * 10.0F, -6.0F, 32.0F);
        float back = Math.max(0, (float) (d0 * sin + d2 * cos) * 100.0F);
        float side = (float) (d0 * cos - d2 * sin) * 100.0F;
        float bob = p.prevCameraYaw + (p.cameraYaw - p.prevCameraYaw) * pt;
        lift += MathHelper.sin((p.prevDistanceWalkedModified + (p.distanceWalkedModified - p.prevDistanceWalkedModified) * pt) * 6.0F) * 32.0F * bob;
        if (p.isSneaking()) lift += 25.0F;
        GlStateManager.rotate(6.0F + Math.min(back, 150f) / 2.0F + lift, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(side / 2.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-side / 2.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        model.cape.render(0.0625F);
        GlStateManager.popMatrix();
    }

    public boolean shouldCombineTextures() { return false; }

    /** One texture per design, painted the first time somebody wears it. */
    private static ResourceLocation texture(String id) {
        if (textures.containsKey(id)) return textures.get(id);
        ResourceLocation tex = null;
        try {
            tex = mc.getTextureManager().getDynamicTextureLocation("lumen_cape", new DynamicTexture(generate(id)));
        } catch (Throwable ignored) {}
        textures.put(id, tex);
        return tex;
    }

    /** Paints a 64x32-layout cape texture at 16x resolution: one face design, used for both sides. */
    private static BufferedImage generate(String id) {
        int k = 16, w = 10 * k, h = 16 * k;
        BufferedImage face = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = face.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        // "cape_aurora" -> "Aurora"
        String s = id.startsWith("cape_") ? Character.toUpperCase(id.charAt(5)) + id.substring(6) : id;
        Random rnd = new Random(s.hashCode());
        if (s.equals("Aurora")) aurora(g, w, h, rnd);
        else if (s.equals("Galaxy")) galaxy(g, w, h, rnd);
        else if (s.equals("Sakura")) sakura(g, w, h, rnd);
        else if (s.equals("Ember")) ember(g, w, h, rnd);
        else if (s.equals("Royal")) royal(g, w, h);
        else if (s.equals("Synthwave")) synthwave(g, w, h);
        else if (s.equals("Frost")) frost(g, w, h, rnd);
        else if (s.equals("Ruby")) ruby(g, w, h, rnd);
        else {
            Color a, b;
            if (s.equals("Midnight")) { a = new Color(0x0B0B14); b = new Color(0x5B2EA6); }
            else if (s.equals("Sunset")) { a = new Color(0xFF8A3D); b = new Color(0xE0367A); }
            else if (s.equals("Ocean")) { a = new Color(0x0E7FA8); b = new Color(0x0A2A66); }
            else if (s.equals("Blossom")) { a = new Color(0xFFD1E3); b = new Color(0xFF6BA6); }
            else if (s.equals("Mono")) { a = new Color(0x15171C); b = new Color(0x15171C); }
            else { a = new Color(0x101A33); b = new Color(0x38D9F5); }
            g.setPaint(new GradientPaint(0, 0, a, 0, h, b));
            g.fillRect(0, 0, w, h);
            if (s.equals("Mono")) { g.setColor(Color.WHITE); g.fillRect(0, 11 * k, w, k); }
            {
                int cx = w / 2, cy = 6 * k, r = 3 * k;
                g.setColor(new Color(255, 255, 255, 235));
                g.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
                r = (int) (1.5 * k);
                g.setPaint(new GradientPaint(0, 0, a, 0, h, b));
                g.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
            }
        }
        g.dispose();

        BufferedImage img = new BufferedImage(64 * k, 32 * k, BufferedImage.TYPE_INT_ARGB);
        Graphics2D o = img.createGraphics();
        o.setColor(new Color(face.getRGB(w / 2, 3)));
        o.fillRect(0, 0, 22 * k, 17 * k);
        o.drawImage(face, k, k, null);
        o.drawImage(face, 12 * k, k, null);
        o.dispose();
        return img;
    }

    private static Color rgba(int rgb, int a) { return new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, Math.max(0, Math.min(255, a))); }

    private static void vertical(Graphics2D g, int w, int h, float[] stops, int... colors) {
        Color[] cs = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) cs[i] = new Color(colors[i]);
        g.setPaint(new LinearGradientPaint(0, 0, 0, h, stops, cs));
        g.fillRect(0, 0, w, h);
    }

    private static void glow(Graphics2D g, float x, float y, float r, int rgb, int alpha) {
        g.setPaint(new RadialGradientPaint(x, y, r, new float[]{0f, 1f}, new Color[]{rgba(rgb, alpha), rgba(rgb, 0)}));
        g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
    }

    private static void stars(Graphics2D g, int w, int h, Random rnd, int n, float maxY) {
        for (int i = 0; i < n; i++) {
            float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h * maxY, r = 0.6f + rnd.nextFloat() * 1.6f;
            g.setColor(rgba(0xFFFFFF, 120 + rnd.nextInt(135)));
            g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
            if (r > 1.9f) {
                g.setStroke(new BasicStroke(1f));
                g.setColor(rgba(0xFFFFFF, 150));
                g.draw(new Line2D.Float(x - r * 3, y, x + r * 3, y));
                g.draw(new Line2D.Float(x, y - r * 3, x, y + r * 3));
            }
        }
    }

    private static void aurora(Graphics2D g, int w, int h, Random rnd) {
        vertical(g, w, h, new float[]{0f, 0.6f, 1f}, 0x050B1F, 0x0A2238, 0x0D3B4A);
        stars(g, w, h, rnd, 40, 0.55f);
        int[] cols = {0x3CFFA0, 0x38D9F5, 0xA06BFF};
        for (int b = 0; b < 3; b++) {
            float base = h * (0.22f + b * 0.17f), amp = h * 0.05f, ph = rnd.nextFloat() * 6f, len = h * (0.34f + 0.05f * b);
            Path2D.Float path = new Path2D.Float();
            path.moveTo(0, base + Math.sin(ph) * amp);
            for (int x = 0; x <= w; x += 4) path.lineTo(x, base + Math.sin(ph + x / (float) w * 5.5f) * amp + Math.sin(ph * 2 + x / (float) w * 11f) * amp * 0.35f);
            path.lineTo(w, base + len); path.lineTo(0, base + len); path.closePath();
            g.setPaint(new LinearGradientPaint(0, base - amp, 0, base + len, new float[]{0f, 0.25f, 1f},
                    new Color[]{rgba(cols[b], 210), rgba(cols[b], 110), rgba(cols[b], 0)}));
            g.fill(path);
        }
    }

    private static void galaxy(Graphics2D g, int w, int h, Random rnd) {
        vertical(g, w, h, new float[]{0f, 0.5f, 1f}, 0x070418, 0x1D0B45, 0x081A4A);
        glow(g, w * 0.3f, h * 0.3f, w * 0.75f, 0xC23BFF, 130);
        glow(g, w * 0.75f, h * 0.62f, w * 0.8f, 0x2C7BFF, 140);
        glow(g, w * 0.45f, h * 0.48f, w * 0.4f, 0xFF7AD9, 120);
        glow(g, w * 0.5f, h * 0.47f, w * 0.14f, 0xFFFFFF, 200);
        stars(g, w, h, rnd, 90, 1f);
    }

    private static void sakura(Graphics2D g, int w, int h, Random rnd) {
        vertical(g, w, h, new float[]{0f, 1f}, 0xFFF1F6, 0xFFA9C8);
        g.setStroke(new BasicStroke(w * 0.035f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x6B4036));
        Path2D.Float br = new Path2D.Float();
        br.moveTo(-4, h * 0.10f); br.curveTo(w * 0.35f, h * 0.16f, w * 0.55f, h * 0.30f, w * 0.95f, h * 0.36f);
        br.moveTo(w * 0.42f, h * 0.20f); br.curveTo(w * 0.5f, h * 0.12f, w * 0.7f, h * 0.10f, w * 0.85f, h * 0.05f);
        g.draw(br);
        for (int i = 0; i < 34; i++) {
            boolean onBranch = i < 16;
            float x = onBranch ? rnd.nextFloat() * w : rnd.nextFloat() * w;
            float y = onBranch ? h * (0.05f + 0.33f * rnd.nextFloat()) : h * (0.4f + 0.58f * rnd.nextFloat());
            float r = w * (onBranch ? 0.07f : 0.045f) * (0.7f + rnd.nextFloat() * 0.6f);
            AffineTransform old = g.getTransform();
            g.rotate(rnd.nextFloat() * Math.PI, x, y);
            g.setColor(rgba(rnd.nextBoolean() ? 0xFF7FAE : 0xFFB7D1, onBranch ? 235 : 200));
            g.fill(new Ellipse2D.Float(x - r, y - r * 0.55f, r * 2, r * 1.1f));
            g.setColor(rgba(0xFFFFFF, 150));
            g.fill(new Ellipse2D.Float(x - r * 0.45f, y - r * 0.2f, r * 0.9f, r * 0.4f));
            g.setTransform(old);
        }
    }

    private static void ember(Graphics2D g, int w, int h, Random rnd) {
        vertical(g, w, h, new float[]{0f, 0.55f, 1f}, 0x0D0606, 0x2A0A08, 0x8A1F0C);
        glow(g, w * 0.5f, h * 1.05f, w * 1.1f, 0xFF5A1F, 230);
        glow(g, w * 0.5f, h * 1.02f, w * 0.55f, 0xFFC247, 220);
        for (int i = 0; i < 46; i++) {
            float t = rnd.nextFloat();
            float x = rnd.nextFloat() * w, y = h * (0.15f + 0.85f * t), r = 0.8f + rnd.nextFloat() * 2.2f * t;
            g.setColor(rgba(rnd.nextBoolean() ? 0xFFB347 : 0xFF6A2B, (int) (90 + 165 * t)));
            g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
        }
    }

    private static void royal(Graphics2D g, int w, int h) {
        vertical(g, w, h, new float[]{0f, 1f}, 0x2A0F57, 0x4A1C8C);
        glow(g, w * 0.5f, h * 0.36f, w * 0.7f, 0x9B5CFF, 110);
        Color gold = new Color(0xF5C542), goldDark = new Color(0xC9972B);
        float in = w * 0.08f;
        g.setStroke(new BasicStroke(w * 0.035f));
        g.setColor(gold);
        g.draw(new java.awt.geom.Rectangle2D.Float(in, in, w - in * 2, h - in * 2));
        g.setStroke(new BasicStroke(w * 0.012f));
        g.setColor(goldDark);
        g.draw(new java.awt.geom.Rectangle2D.Float(in * 1.8f, in * 1.8f, w - in * 3.6f, h - in * 3.6f));
        // crown
        float cx = w / 2f, cy = h * 0.36f, cw = w * 0.5f, ch = h * 0.11f;
        Path2D.Float crown = new Path2D.Float();
        crown.moveTo(cx - cw / 2, cy + ch / 2); crown.lineTo(cx - cw / 2, cy - ch / 2); crown.lineTo(cx - cw / 4, cy);
        crown.lineTo(cx, cy - ch * 0.8f); crown.lineTo(cx + cw / 4, cy); crown.lineTo(cx + cw / 2, cy - ch / 2);
        crown.lineTo(cx + cw / 2, cy + ch / 2); crown.closePath();
        g.setPaint(new GradientPaint(0, cy - ch, new Color(0xFFE38A), 0, cy + ch / 2, goldDark));
        g.fill(crown);
        g.setColor(new Color(0xE0367A));
        g.fill(new Ellipse2D.Float(cx - w * 0.03f, cy + ch * 0.05f, w * 0.06f, w * 0.06f));
        // chevrons
        g.setStroke(new BasicStroke(w * 0.03f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
        g.setColor(gold);
        for (int i = 0; i < 3; i++) {
            float y = h * (0.62f + i * 0.09f);
            Path2D.Float v = new Path2D.Float();
            v.moveTo(w * 0.24f, y); v.lineTo(cx, y + h * 0.06f); v.lineTo(w * 0.76f, y);
            g.draw(v);
        }
    }

    private static void synthwave(Graphics2D g, int w, int h) {
        vertical(g, w, h, new float[]{0f, 0.5f, 0.62f, 1f}, 0x130633, 0x5B1E86, 0xFF4FA3, 0x0B0420);
        float horizon = h * 0.62f, r = w * 0.36f, cx = w / 2f, cy = horizon - r * 0.55f;
        java.awt.Shape oldClip = g.getClip();
        g.setClip(0, 0, w, (int) horizon);
        g.setPaint(new GradientPaint(0, cy - r, new Color(0xFFE66B), 0, cy + r, new Color(0xFF3F8E)));
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g.setColor(new Color(0x5B1E86));
        for (int i = 0; i < 5; i++) {
            float y = cy + r * (0.05f + i * 0.2f);
            g.fill(new java.awt.geom.Rectangle2D.Float(cx - r, y, r * 2, 1.5f + i * 1.3f));
        }
        g.setClip(oldClip);
        g.setColor(new Color(0x0B0420));
        g.fillRect(0, (int) horizon, w, h);
        g.setColor(rgba(0x38F5FF, 230));
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < 9; i++) {
            float t = i / 8f, y = horizon + (h - horizon) * t * t;
            g.draw(new Line2D.Float(0, y, w, y));
        }
        for (int i = -6; i <= 6; i++) g.draw(new Line2D.Float(cx + i * w * 0.06f, horizon, cx + i * w * 0.42f, h));
        glow(g, cx, horizon, w * 0.6f, 0xFF4FA3, 90);
    }

    /** Deep red with a cut gem. */
    private static void ruby(Graphics2D g, int w, int h, Random rnd) {
        vertical(g, w, h, new float[]{0f, 0.55f, 1f}, 0x1A0408, 0x5A0A18, 0xB3122E);
        glow(g, w * 0.5f, h * 0.38f, w * 0.75f, 0xFF3355, 120);
        stars(g, w, h, rnd, 26, 1f);
        float cx = w / 2f, cy = h * 0.38f, r = w * 0.30f;
        float[][] o = {{cx - r, cy - r * 0.35f}, {cx - r * 0.55f, cy - r}, {cx + r * 0.55f, cy - r}, {cx + r, cy - r * 0.35f}, {cx, cy + r * 1.25f}};
        Path2D.Float gem = new Path2D.Float();
        gem.moveTo(o[0][0], o[0][1]);
        for (int i = 1; i < o.length; i++) gem.lineTo(o[i][0], o[i][1]);
        gem.closePath();
        g.setPaint(new GradientPaint(0, cy - r, new Color(0xFF8FA3), 0, cy + r * 1.25f, new Color(0xC1121F)));
        g.fill(gem);
        g.setColor(rgba(0xFFFFFF, 190));
        g.setStroke(new BasicStroke(Math.max(1.2f, w * 0.012f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(gem);
        g.draw(new Line2D.Float(o[0][0], o[0][1], o[3][0], o[3][1]));
        g.draw(new Line2D.Float(o[1][0], o[1][1], cx - r * 0.3f, o[0][1])); g.draw(new Line2D.Float(o[2][0], o[2][1], cx + r * 0.3f, o[0][1]));
        g.draw(new Line2D.Float(cx - r * 0.3f, o[0][1], cx, o[4][1])); g.draw(new Line2D.Float(cx + r * 0.3f, o[0][1], cx, o[4][1]));
        g.setStroke(new BasicStroke(w * 0.03f));
        g.setColor(new Color(0xF5C542));
        g.draw(new java.awt.geom.Rectangle2D.Float(w * 0.07f, w * 0.07f, w * 0.86f, h - w * 0.14f));
    }

    private static void frost(Graphics2D g, int w, int h, Random rnd) {
        vertical(g, w, h, new float[]{0f, 0.6f, 1f}, 0xF6FCFF, 0xBFE6FF, 0x6DBBF2);
        // ice facets along the bottom
        for (int i = 0; i < 9; i++) {
            float x = rnd.nextFloat() * w, bw = w * (0.18f + rnd.nextFloat() * 0.25f), bh = h * (0.10f + rnd.nextFloat() * 0.2f);
            Path2D.Float t = new Path2D.Float();
            t.moveTo(x - bw / 2, h); t.lineTo(x, h - bh); t.lineTo(x + bw / 2, h); t.closePath();
            g.setColor(rgba(rnd.nextBoolean() ? 0xFFFFFF : 0x3F96DA, 70 + rnd.nextInt(70)));
            g.fill(t);
        }
        for (int i = 0; i < 9; i++) {
            float x = w * (0.12f + 0.76f * rnd.nextFloat()), y = h * (0.06f + 0.66f * rnd.nextFloat()), r = w * (0.06f + rnd.nextFloat() * 0.11f);
            g.setColor(rgba(i % 3 == 0 ? 0x3F96DA : 0xFFFFFF, 235));
            g.setStroke(new BasicStroke(Math.max(1.2f, r * 0.13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double rot = rnd.nextFloat() * Math.PI;
            for (int a = 0; a < 6; a++) {
                double ang = rot + a * Math.PI / 3;
                float ex = x + (float) Math.cos(ang) * r, ey = y + (float) Math.sin(ang) * r;
                g.draw(new Line2D.Float(x, y, ex, ey));
                float mx = x + (float) Math.cos(ang) * r * 0.6f, my = y + (float) Math.sin(ang) * r * 0.6f;
                for (int sgn = -1; sgn <= 1; sgn += 2) {
                    double ba = ang + sgn * Math.PI / 4;
                    g.draw(new Line2D.Float(mx, my, mx + (float) Math.cos(ba) * r * 0.3f, my + (float) Math.sin(ba) * r * 0.3f));
                }
            }
        }
    }
}
