package dev.lumen.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Draws the equipped Lumen wings on any player (yours, or another Lumen user's as reported by the backend). */
public class WingsLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /** The arm of the wing, shoulder to wrist, that the feathers hang from. Declared first: the feathers are built from it. */
    private static final float[][] ARM = {{0f, 0f}, {4.5f, -5.5f}, {10f, -8.5f}, {16f, -8.5f}, {21f, -6f}};
    /**
     * A feathered wing, built once: every feather is {baseX, baseY, sideX, sideY, tipX, tipY, side2X, side2Y, shade}.
     * Model pixels, x outwards from the back, y downwards. Long flight feathers first, then two rows of coverts over them.
     */
    private static final float[][] FEATHER = feathers();

    private static float[] along(float t) {
        float f = t * (ARM.length - 1);
        int i = Math.min(ARM.length - 2, (int) f);
        float k = f - i;
        return new float[]{ARM[i][0] + (ARM[i + 1][0] - ARM[i][0]) * k, ARM[i][1] + (ARM[i + 1][1] - ARM[i][1]) * k};
    }

    private static float[][] feathers() {
        java.util.List<float[]> out = new java.util.ArrayList<float[]>();
        // {count, shortest, longest, width, shade}: flight feathers, then coverts lying over their roots
        float[][] rows = {{11, 8.5f, 17f, 0.30f, 0f}, {10, 5.5f, 9f, 0.42f, 0.5f}, {9, 3f, 4.5f, 0.62f, 1f}};
        for (float[] row : rows) {
            int n = (int) row[0];
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                float[] b = along(0.06f + t * 0.94f);
                // near the body the feathers hang straight down; towards the tip they sweep outwards
                double ang = Math.toRadians(100 - 62 * Math.pow(t, 1.25));
                float len = row[1] + (row[2] - row[1]) * (float) Math.pow(t, 0.8), w = len * row[3] * 0.5f;
                float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
                float mx = b[0] + dx * len * 0.55f, my = b[1] + dy * len * 0.55f;
                out.add(new float[]{b[0], b[1], mx - dy * w, my + dx * w, b[0] + dx * len, b[1] + dy * len, mx + dy * w, my - dx * w, row[4]});
            }
        }
        return out.toArray(new float[0][]);
    }

    private static final ResourceLocation DRAGON = new ResourceLocation("textures/entity/enderdragon/dragon.png");

    private final RenderPlayer renderer;
    private final DragonWing dragon = new DragonWing();

    public WingsLayer(RenderPlayer renderer) { this.renderer = renderer; }

    /** The Ender Dragon's own wing (bone + membrane, with a jointed tip), using the vanilla dragon texture. */
    private static final class DragonWing extends ModelBase {
        final ModelRenderer wing, tip;
        DragonWing() {
            textureWidth = 256; textureHeight = 256;
            setTextureOffset("wing.bone", 112, 88);
            setTextureOffset("wing.skin", -56, 88);
            setTextureOffset("wingtip.bone", 112, 136);
            setTextureOffset("wingtip.skin", -56, 144);
            wing = new ModelRenderer(this, "wing");
            wing.setTextureSize(256, 256);
            wing.setRotationPoint(-2.0F, 0.0F, 0.0F);
            wing.addBox("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8);
            wing.addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56);
            tip = new ModelRenderer(this, "wingtip");
            tip.setTextureSize(256, 256);
            tip.setRotationPoint(-56.0F, 0.0F, 0.0F);
            tip.addBox("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4);
            tip.addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56);
            wing.addChild(tip);
        }
    }

    private void renderDragon(AbstractClientPlayer p, int tint, float age) {
        float f = age * 0.1f * (p.onGround ? 1f : 2.2f);
        float k = 0.2f;
        GlStateManager.pushMatrix();
        if (p.isSneaking()) {
            GlStateManager.translate(0, 0.2f, 0);
            GlStateManager.rotate(28.6f, 1, 0, 0);
        }
        GlStateManager.translate(0, 0.2f, 0.14f);
        GlStateManager.scale(k, k, k);
        if (tint != 0) {
            int c = tint;
            GlStateManager.color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, 1f);
        } else GlStateManager.color(1, 1, 1, 1);
        renderer.bindTexture(DRAGON);
        GlStateManager.enableCull();
        for (int side = 0; side < 2; side++) {
            dragon.wing.rotateAngleX = (float) Math.toRadians(-80) - (float) Math.cos(f) * 0.2f;
            dragon.wing.rotateAngleY = (float) Math.toRadians(20) + (float) Math.sin(f) * 0.4f;
            dragon.wing.rotateAngleZ = (float) Math.toRadians(20);
            dragon.tip.rotateAngleZ = -(float) (Math.sin(f + 2.0f) + 0.5) * 0.75f;
            dragon.wing.render(0.0625f);
            GlStateManager.scale(-1, 1, 1);
            if (side == 0) GlStateManager.cullFace(GL11.GL_FRONT);
        }
        GlStateManager.cullFace(GL11.GL_BACK);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt, float age, float yaw, float pitch, float scale) {
        String id = dev.lumen.online.Online.wear(p, "wings");
        if (id == null || p.isInvisible()) return;
        if (id.equals("wings_dragon")) { renderDragon(p, 0, age); return; }
        if (id.equals("wings_shadow")) { renderDragon(p, 0xFF7A5BD6, age); return; }
        if (id.equals("wings_ruby")) { renderDragon(p, 0xFFFF3B55, age); return; }
        boolean angel = id.equals("wings_angel");
        // feather colour at the root and at the tip, the edge line, and how solid the wing is
        int c = angel ? 0xFFFFFF : id.equals("wings_rose") ? 0xFF6FB5 : 0x4C8DFF;
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        float[] root = angel ? new float[]{0.74f, 0.80f, 0.93f} : new float[]{r * 0.55f, g * 0.55f, b * 0.75f};
        float[] tip = angel ? new float[]{1f, 1f, 1f} : new float[]{r + (1 - r) * 0.7f, g + (1 - g) * 0.7f, b + (1 - b) * 0.7f};
        float[] edge = angel ? new float[]{0.60f, 0.68f, 0.86f} : new float[]{r + (1 - r) * 0.5f, g + (1 - g) * 0.5f, b + (1 - b) * 0.5f};
        float solid = angel ? 1f : 0.72f;
        boolean airborne = !p.onGround;
        float flap = (float) Math.sin(age * 0.12f * (airborne ? 2.2f : 1f));
        float angle = 30f + flap * (airborne ? 26f : 10f);
        float s = 0.0625f * 0.92f;

        GlStateManager.pushMatrix();
        if (p.isSneaking()) {
            GlStateManager.translate(0, 0.2f, 0);
            GlStateManager.rotate(28.6f, 1, 0, 0);
        }
        GlStateManager.translate(0, 0.17f, 0.135f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        if (!angel) GlStateManager.depthMask(false); // see-through wings must not hide what is behind them
        for (int side = -1; side <= 1; side += 2) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(side * 0.06f, 0, 0);
            GlStateManager.scale(side * s, s, s);
            GlStateManager.rotate(-angle, 0, 1, 0);
            GlStateManager.rotate(-6f - flap * 3f, 0, 0, 1);
            for (int i = 0; i < FEATHER.length; i++) {
                float[] f = FEATHER[i];
                // each row sits a hair further back so the coverts cover the roots of the row before
                float z = f[8] * 0.12f + (i % 2) * 0.02f, k = 1f - f[8] * 0.06f;
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                GL11.glColor4f(root[0] * k, root[1] * k, root[2] * k, solid);
                GL11.glVertex3f(f[0], f[1], z);
                GL11.glColor4f((root[0] + tip[0]) * 0.5f, (root[1] + tip[1]) * 0.5f, (root[2] + tip[2]) * 0.5f, solid);
                GL11.glVertex3f(f[2], f[3], z);
                GL11.glColor4f(tip[0], tip[1], tip[2], solid);
                GL11.glVertex3f(f[4], f[5], z);
                GL11.glColor4f(tip[0] * 0.94f, tip[1] * 0.94f, tip[2] * 0.94f, solid);
                GL11.glVertex3f(f[6], f[7], z);
                GL11.glEnd();
                // the edge and the shaft
                GL11.glLineWidth(1.2f);
                GL11.glColor4f(edge[0], edge[1], edge[2], angel ? 0.9f : 0.85f);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                GL11.glVertex3f(f[2], f[3], z + 0.01f); GL11.glVertex3f(f[4], f[5], z + 0.01f); GL11.glVertex3f(f[6], f[7], z + 0.01f);
                GL11.glEnd();
                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex3f(f[0], f[1], z + 0.01f); GL11.glVertex3f(f[4], f[5], z + 0.01f);
                GL11.glEnd();
            }
            // the arm of the wing, over the roots
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i < ARM.length; i++) {
                float w = 1.5f - i * 0.22f;
                GL11.glColor4f(tip[0], tip[1], tip[2], solid);
                GL11.glVertex3f(ARM[i][0], ARM[i][1] - w, 0.3f);
                GL11.glColor4f(root[0], root[1], root[2], solid);
                GL11.glVertex3f(ARM[i][0], ARM[i][1] + w * 0.6f, 0.3f);
            }
            GL11.glEnd();
            GL11.glLineWidth(1f);
            GlStateManager.popMatrix();
        }
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GlStateManager.resetColor();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    public boolean shouldCombineTextures() { return false; }
}
