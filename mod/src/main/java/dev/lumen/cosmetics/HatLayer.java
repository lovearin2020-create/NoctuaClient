package dev.lumen.cosmetics;

import dev.lumen.online.Online;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;

/** Hats: small voxel models that follow the head. Head top is y = -8, the face looks towards -z. */
public class HatLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final int GOLD = 0xF5C542, GOLD_DARK = 0xC9972B, RUBY = 0xE0244A;
    private final RenderPlayer renderer;

    public HatLayer(RenderPlayer renderer) { this.renderer = renderer; }

    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt, float age, float yaw, float pitch, float scale) {
        String id = Online.wear(p, "hat");
        if (id == null || p.isInvisible()) return;
        GlStateManager.pushMatrix();
        if (p.isSneaking()) GlStateManager.translate(0f, 0.2f, 0f);
        renderer.getMainModel().bipedHead.postRender(0.0625f);
        GlStateManager.scale(0.0625f, 0.0625f, 0.0625f);
        boolean glow = id.equals("hat_halo");
        float lx = OpenGlHelper.lastBrightnessX, ly = OpenGlHelper.lastBrightnessY;
        if (glow) { GlStateManager.disableLighting(); OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f); }
        Shapes.begin();
        if (id.equals("hat_tophat")) topHat();
        else if (id.equals("hat_crown")) crown();
        else if (id.equals("hat_halo")) halo(age);
        else if (id.equals("hat_horns")) horns();
        else if (id.equals("hat_catears")) catEars();
        else if (id.equals("hat_wizard")) wizard();
        Shapes.end();
        if (glow) { OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lx, ly); GlStateManager.enableLighting(); }
        GlStateManager.popMatrix();
    }

    private static void topHat() {
        Shapes.box(-5.5f, -8.8f, -5.5f, 5.5f, -8f, 5.5f, 0x17181C);   // brim
        Shapes.box(-3.6f, -15f, -3.6f, 3.6f, -8.8f, 3.6f, 0x1D1E24);   // crown
        Shapes.box(-3.75f, -10.6f, -3.75f, 3.75f, -8.8f, 3.75f, RUBY); // band
        Shapes.box(-0.9f, -10.4f, -3.95f, 0.9f, -9f, -3.75f, GOLD);    // buckle
    }

    private static void crown() {
        float r = 4.4f, t = 0.8f;
        Shapes.box(-r, -10.2f, -r, r, -8f, -r + t, GOLD); Shapes.box(-r, -10.2f, r - t, r, -8f, r, GOLD);
        Shapes.box(-r, -10.2f, -r + t, -r + t, -8f, r - t, GOLD); Shapes.box(r - t, -10.2f, -r + t, r, -8f, r - t, GOLD);
        // points: corners and the middle of each side
        float[][] pts = {{-r, -r}, {r - 1.2f, -r}, {-r, r - 1.2f}, {r - 1.2f, r - 1.2f}, {-0.6f, -r}, {-0.6f, r - 1.2f}, {-r, -0.6f}, {r - 1.2f, -0.6f}};
        for (int i = 0; i < pts.length; i++) {
            boolean mid = i >= 4;
            Shapes.box(pts[i][0], mid ? -13f : -12f, pts[i][1], pts[i][0] + 1.2f, -10.2f, pts[i][1] + 1.2f, mid ? GOLD : GOLD_DARK);
        }
        Shapes.box(-0.7f, -9.7f, -r - 0.2f, 0.7f, -8.5f, -r, RUBY);
        Shapes.box(-r - 0.2f, -9.7f, -0.7f, -r, -8.5f, 0.7f, 0x3B82F6); Shapes.box(r, -9.7f, -0.7f, r + 0.2f, -8.5f, 0.7f, 0x3B82F6);
        Shapes.box(-0.7f, -9.7f, r, 0.7f, -8.5f, r + 0.2f, 0x34D399);
    }

    private static void halo(float age) {
        float y = -12.2f + (float) Math.sin(age * 0.08f) * 0.35f, r = 4.6f, t = 1.1f;
        Shapes.box(-r + 1.5f, y, -r, r - 1.5f, y + 0.9f, -r + t, 0xFFE98A); Shapes.box(-r + 1.5f, y, r - t, r - 1.5f, y + 0.9f, r, 0xFFE98A);
        Shapes.box(-r, y, -r + 1.5f, -r + t, y + 0.9f, r - 1.5f, 0xFFE98A); Shapes.box(r - t, y, -r + 1.5f, r, y + 0.9f, r - 1.5f, 0xFFE98A);
        float c = r - 1.5f;
        Shapes.box(-c - 0.6f, y, -c - 0.6f, -c + 0.6f, y + 0.9f, -c + 0.6f, 0xFFF4B8); Shapes.box(c - 0.6f, y, -c - 0.6f, c + 0.6f, y + 0.9f, -c + 0.6f, 0xFFF4B8);
        Shapes.box(-c - 0.6f, y, c - 0.6f, -c + 0.6f, y + 0.9f, c + 0.6f, 0xFFF4B8); Shapes.box(c - 0.6f, y, c - 0.6f, c + 0.6f, y + 0.9f, c + 0.6f, 0xFFF4B8);
    }

    private static void horns() {
        for (int s = -1; s <= 1; s += 2) {
            float x = s * 3.2f;
            Shapes.box(Math.min(x, x + s * 1.8f), -9.4f, -1f, Math.max(x, x + s * 1.8f), -7.6f, 1f, 0x7A1420);
            Shapes.box(Math.min(x + s * 1.2f, x + s * 2.8f), -11.2f, -0.8f, Math.max(x + s * 1.2f, x + s * 2.8f), -9.4f, 0.8f, 0xA51C2C);
            Shapes.box(Math.min(x + s * 2.2f, x + s * 3.4f), -13f, -0.6f, Math.max(x + s * 2.2f, x + s * 3.4f), -11.2f, 0.6f, 0xC9303F);
            Shapes.box(Math.min(x + s * 2.0f, x + s * 2.9f), -14.2f, -0.45f, Math.max(x + s * 2.0f, x + s * 2.9f), -13f, 0.45f, 0xEFE2C4);
        }
    }

    private static void catEars() {
        for (int s = -1; s <= 1; s += 2) {
            float in = s * 1.2f, out = s * 4.4f;
            Shapes.box(Math.min(in, out), -9.2f, -1.2f, Math.max(in, out), -8f, 0.2f, 0x2B2D36);
            Shapes.box(Math.min(s * 1.8f, s * 4.0f), -10.4f, -1.1f, Math.max(s * 1.8f, s * 4.0f), -9.2f, 0.1f, 0x2B2D36);
            Shapes.box(Math.min(s * 2.5f, s * 3.6f), -11.5f, -1.0f, Math.max(s * 2.5f, s * 3.6f), -10.4f, 0f, 0x2B2D36);
            Shapes.box(Math.min(s * 2.0f, s * 3.7f), -10.2f, -1.3f, Math.max(s * 2.0f, s * 3.7f), -8.3f, -1.1f, 0xFF8FB8); // inner ear
        }
    }

    private static void wizard() {
        int cloth = 0x4B2A9E, dark = 0x3A1F80;
        Shapes.box(-6.5f, -8.7f, -6.5f, 6.5f, -8f, 6.5f, dark);
        Shapes.box(-4.2f, -11f, -4.2f, 4.2f, -8.7f, 4.2f, cloth);
        Shapes.box(-4.35f, -10f, -4.35f, 4.35f, -8.7f, 4.35f, GOLD);
        Shapes.box(-3.2f, -13.2f, -3.0f, 3.2f, -11f, 3.4f, cloth);
        Shapes.box(-2.2f, -15.2f, -1.6f, 2.2f, -13.2f, 2.9f, dark);
        Shapes.box(-1.3f, -16.8f, -0.2f, 1.3f, -15.2f, 2.6f, cloth);
        Shapes.box(-0.7f, -17.8f, 1.0f, 0.7f, -16.8f, 2.6f, dark);
        Shapes.box(-0.8f, -12.6f, -3.2f, 0.8f, -11.4f, -3.0f, 0xFFE98A); // star
    }

    public boolean shouldCombineTextures() { return false; }
}
