package dev.lumen.fabric.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.lumen.fabric.compat.Compat;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Draws the equipped Lumen wings on any player (yours, or another Lumen user's as reported by the backend). */
public class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Identifier DRAGON = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
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

    private final ModelPart[] wing = new ModelPart[2], tip = new ModelPart[2];

    public WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
        for (int i = 0; i < 2; i++) {
            // The Ender Dragon's own wing (bone + membrane, with a jointed tip), using the vanilla dragon texture.
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition w = mesh.getRoot().addOrReplaceChild("wing", CubeListBuilder.create()
                    .texOffs(112, 88).addBox(-56.0F, -4.0F, -4.0F, 56, 8, 8)
                    .texOffs(-56, 88).addBox(-56.0F, 0.0F, 2.0F, 56, 0, 56), PartPose.offset(-2.0F, 0.0F, 0.0F));
            w.addOrReplaceChild("tip", CubeListBuilder.create()
                    .texOffs(112, 136).addBox(-56.0F, -2.0F, -2.0F, 56, 4, 4)
                    .texOffs(-56, 144).addBox(-56.0F, 0.0F, 2.0F, 56, 0, 56), PartPose.offset(-56.0F, 0.0F, 0.0F));
            wing[i] = LayerDefinition.create(mesh, 256, 256).bakeRoot().getChild("wing");
            tip[i] = wing[i].getChild("tip");
        }
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
        OutfitState o = (OutfitState) state;
        String id = o.lumen$wings();
        if (id == null || state.isInvisible) return;
        boolean airborne = o.lumen$airborne();
        float age = state.ageInTicks;
        ps.pushPose();
        if (state.isCrouching) {
            ps.translate(0, 0.2f, 0);
            Draw.rotate(ps, Axis.XP.rotationDegrees(28.6f));
        }
        if (id.equals("wings_dragon")) dragon(ps, out, light, -1, age, airborne);
        else if (id.equals("wings_shadow")) dragon(ps, out, light, 0xFF7A5BD6, age, airborne);
        else if (id.equals("wings_ruby")) dragon(ps, out, light, 0xFFFF3B55, age, airborne);
        else feathered(ps, out, light, id, age, airborne);
        ps.popPose();
    }

    private void dragon(PoseStack ps, SubmitNodeCollector out, int light, int tint, float age, boolean airborne) {
        float f = age * 0.1f * (airborne ? 2.2f : 1f);
        float k = 0.2f;
        ps.translate(0, 0.2f, 0.14f);
        ps.scale(k, k, k);
        for (int side = 0; side < 2; side++) {
            ModelPart p = wing[side];
            p.xRot = (float) Math.toRadians(-80) - (float) Math.cos(f) * 0.2f;
            p.yRot = (float) Math.toRadians(20) + (float) Math.sin(f) * 0.4f;
            p.zRot = (float) Math.toRadians(20);
            tip[side].zRot = -(float) (Math.sin(f + 2.0f) + 0.5) * 0.75f;
            // The game resets parts to their initial pose before drawing, so the animation is stored there.
            p.setInitialPose(p.storePose());
            tip[side].setInitialPose(tip[side].storePose());
            ps.pushPose();
            if (side == 1) ps.scale(-1, 1, 1);
            Compat.submitPart(out, p, ps, RenderTypes.entityCutout(DRAGON), light, tint);
            ps.popPose();
        }
    }

    private static int argb(float r, float g, float b, float a) {
        return Math.round(a * 255f) << 24 | Math.round(Math.min(1f, r) * 255f) << 16 | Math.round(Math.min(1f, g) * 255f) << 8 | Math.round(Math.min(1f, b) * 255f);
    }

    /** Angel and crystal wings: three rows of feathers, shaded from root to tip. */
    private void feathered(PoseStack ps, SubmitNodeCollector out, int light, String id, float age, boolean airborne) {
        boolean angel = id.equals("wings_angel");
        int c = angel ? 0xFFFFFF : id.equals("wings_rose") ? 0xFF6FB5 : 0x4C8DFF;
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        float solid = angel ? 1f : 0.72f;
        final float[] root = angel ? new float[]{0.74f, 0.80f, 0.93f} : new float[]{r * 0.55f, g * 0.55f, b * 0.75f};
        final float[] tip = angel ? new float[]{1f, 1f, 1f} : new float[]{r + (1 - r) * 0.7f, g + (1 - g) * 0.7f, b + (1 - b) * 0.7f};
        final int edge = angel ? argb(0.60f, 0.68f, 0.86f, 0.9f) : argb(r + (1 - r) * 0.5f, g + (1 - g) * 0.5f, b + (1 - b) * 0.5f, 0.85f);
        final int tipC = argb(tip[0], tip[1], tip[2], solid), tip2 = argb(tip[0] * 0.94f, tip[1] * 0.94f, tip[2] * 0.94f, solid);
        final int midC = argb((root[0] + tip[0]) * 0.5f, (root[1] + tip[1]) * 0.5f, (root[2] + tip[2]) * 0.5f, solid);
        final int rootC = argb(root[0], root[1], root[2], solid);
        final int lit = angel ? light : 0xF000F0; // crystal wings glow
        float flap = (float) Math.sin(age * 0.12f * (airborne ? 2.2f : 1f));
        float angle = 30f + flap * (airborne ? 26f : 10f);
        float s = 0.0625f * 0.92f;
        ps.translate(0, 0.17f, 0.135f);
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 0.06f, 0, 0);
            ps.scale(side * s, s, s);
            Draw.rotate(ps, Axis.YP.rotationDegrees(-angle));
            Draw.rotate(ps, Axis.ZP.rotationDegrees(-6f - flap * 3f));
            // Culling types on purpose: every piece is sent facing both ways (see Geo.twoSided) and only the near one is drawn.
            out.submitCustomGeometry(ps, angel ? RenderTypes.entityCutoutCull(Geo.white()) : Compat.translucentCull(Geo.white()), (pose, buf) -> {
                for (int i = 0; i < FEATHER.length; i++) {
                    float[] f = FEATHER[i];
                    // each row sits a hair further back so the coverts cover the roots of the row before
                    float z = f[8] * 0.12f + (i % 2) * 0.02f, k = 1f - f[8] * 0.06f;
                    Geo.twoSided(buf, pose, lit, z, argb(root[0] * k, root[1] * k, root[2] * k, solid), f[0], f[1], midC, f[2], f[3], tipC, f[4], f[5], tip2, f[6], f[7]);
                    // the edge and the shaft, on both faces of the feather
                    line(buf, pose, lit, edge, f[2], f[3], f[4], f[5], 0.07f, z + 0.01f); line(buf, pose, lit, edge, f[2], f[3], f[4], f[5], 0.07f, z - 0.01f);
                    line(buf, pose, lit, edge, f[4], f[5], f[6], f[7], 0.07f, z + 0.01f); line(buf, pose, lit, edge, f[4], f[5], f[6], f[7], 0.07f, z - 0.01f);
                    line(buf, pose, lit, edge, f[0], f[1], f[4], f[5], 0.05f, z + 0.01f); line(buf, pose, lit, edge, f[0], f[1], f[4], f[5], 0.05f, z - 0.01f);
                }
                // the arm of the wing, over the roots
                for (int i = 0; i + 1 < ARM.length; i++) {
                    float w0 = 1.5f - i * 0.22f, w1 = 1.5f - (i + 1) * 0.22f;
                    Geo.twoSided(buf, pose, lit, 0.3f, tipC, ARM[i][0], ARM[i][1] - w0, rootC, ARM[i][0], ARM[i][1] + w0 * 0.6f,
                            rootC, ARM[i + 1][0], ARM[i + 1][1] + w1 * 0.6f, tipC, ARM[i + 1][0], ARM[i + 1][1] - w1);
                }
            });
            ps.popPose();
        }
    }

    private static void line(VertexConsumer b, PoseStack.Pose pose, int light, int color, float x0, float y0, float x1, float y1, float half, float z) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        float nx = -dy / len * half, ny = dx / len * half;
        Geo.twoSided(b, pose, light, z, color, x0 - nx, y0 - ny, color, x1 - nx, y1 - ny, color, x1 + nx, y1 + ny, color, x0 + nx, y0 + ny);
    }
}
