package dev.lumen.fabric.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Hats: small voxel models that follow the head. Head top is y = -8, the face looks towards -z. */
public class HatLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final int GOLD = 0xF5C542, GOLD_DARK = 0xC9972B, RUBY = 0xE0244A;

    public HatLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
        String id = ((OutfitState) state).lumen$hat();
        if (id == null || state.isInvisible) return;
        float age = state.ageInTicks;
        int lit = id.equals("hat_halo") ? 0xF000F0 : light; // the halo glows
        ps.pushPose();
        getParentModel().root().translateAndRotate(ps);
        getParentModel().head.translateAndRotate(ps);
        ps.scale(0.0625f, 0.0625f, 0.0625f);
        out.submitCustomGeometry(ps, RenderTypes.entityCutout(Geo.white()), (p, b) -> {
            if (id.equals("hat_tophat")) topHat(b, p, lit);
            else if (id.equals("hat_crown")) crown(b, p, lit);
            else if (id.equals("hat_halo")) halo(b, p, lit, age);
            else if (id.equals("hat_horns")) horns(b, p, lit);
            else if (id.equals("hat_catears")) catEars(b, p, lit);
            else if (id.equals("hat_wizard")) wizard(b, p, lit);
        });
        ps.popPose();
    }

    private static void topHat(VertexConsumer b, PoseStack.Pose p, int light) {
        Geo.box(b, p, light, -5.5f, -8.8f, -5.5f, 5.5f, -8f, 5.5f, 0x17181C);   // brim
        Geo.box(b, p, light, -3.6f, -15f, -3.6f, 3.6f, -8.8f, 3.6f, 0x1D1E24);   // crown
        Geo.box(b, p, light, -3.75f, -10.6f, -3.75f, 3.75f, -8.8f, 3.75f, RUBY); // band
        Geo.box(b, p, light, -0.9f, -10.4f, -3.95f, 0.9f, -9f, -3.75f, GOLD);    // buckle
    }

    private static void crown(VertexConsumer b, PoseStack.Pose p, int light) {
        float r = 4.4f, t = 0.8f;
        Geo.box(b, p, light, -r, -10.2f, -r, r, -8f, -r + t, GOLD); Geo.box(b, p, light, -r, -10.2f, r - t, r, -8f, r, GOLD);
        Geo.box(b, p, light, -r, -10.2f, -r + t, -r + t, -8f, r - t, GOLD); Geo.box(b, p, light, r - t, -10.2f, -r + t, r, -8f, r - t, GOLD);
        // points: corners and the middle of each side
        float[][] pts = {{-r, -r}, {r - 1.2f, -r}, {-r, r - 1.2f}, {r - 1.2f, r - 1.2f}, {-0.6f, -r}, {-0.6f, r - 1.2f}, {-r, -0.6f}, {r - 1.2f, -0.6f}};
        for (int i = 0; i < pts.length; i++) {
            boolean mid = i >= 4;
            Geo.box(b, p, light, pts[i][0], mid ? -13f : -12f, pts[i][1], pts[i][0] + 1.2f, -10.2f, pts[i][1] + 1.2f, mid ? GOLD : GOLD_DARK);
        }
        Geo.box(b, p, light, -0.7f, -9.7f, -r - 0.2f, 0.7f, -8.5f, -r, RUBY);
        Geo.box(b, p, light, -r - 0.2f, -9.7f, -0.7f, -r, -8.5f, 0.7f, 0x3B82F6); Geo.box(b, p, light, r, -9.7f, -0.7f, r + 0.2f, -8.5f, 0.7f, 0x3B82F6);
        Geo.box(b, p, light, -0.7f, -9.7f, r, 0.7f, -8.5f, r + 0.2f, 0x34D399);
    }

    private static void halo(VertexConsumer b, PoseStack.Pose p, int light, float age) {
        float y = -12.2f + (float) Math.sin(age * 0.08f) * 0.35f, r = 4.6f, t = 1.1f;
        Geo.box(b, p, light, -r + 1.5f, y, -r, r - 1.5f, y + 0.9f, -r + t, 0xFFE98A); Geo.box(b, p, light, -r + 1.5f, y, r - t, r - 1.5f, y + 0.9f, r, 0xFFE98A);
        Geo.box(b, p, light, -r, y, -r + 1.5f, -r + t, y + 0.9f, r - 1.5f, 0xFFE98A); Geo.box(b, p, light, r - t, y, -r + 1.5f, r, y + 0.9f, r - 1.5f, 0xFFE98A);
        float c = r - 1.5f;
        Geo.box(b, p, light, -c - 0.6f, y, -c - 0.6f, -c + 0.6f, y + 0.9f, -c + 0.6f, 0xFFF4B8); Geo.box(b, p, light, c - 0.6f, y, -c - 0.6f, c + 0.6f, y + 0.9f, -c + 0.6f, 0xFFF4B8);
        Geo.box(b, p, light, -c - 0.6f, y, c - 0.6f, -c + 0.6f, y + 0.9f, c + 0.6f, 0xFFF4B8); Geo.box(b, p, light, c - 0.6f, y, c - 0.6f, c + 0.6f, y + 0.9f, c + 0.6f, 0xFFF4B8);
    }

    private static void horns(VertexConsumer b, PoseStack.Pose p, int light) {
        for (int s = -1; s <= 1; s += 2) {
            float x = s * 3.2f;
            Geo.box(b, p, light, Math.min(x, x + s * 1.8f), -9.4f, -1f, Math.max(x, x + s * 1.8f), -7.6f, 1f, 0x7A1420);
            Geo.box(b, p, light, Math.min(x + s * 1.2f, x + s * 2.8f), -11.2f, -0.8f, Math.max(x + s * 1.2f, x + s * 2.8f), -9.4f, 0.8f, 0xA51C2C);
            Geo.box(b, p, light, Math.min(x + s * 2.2f, x + s * 3.4f), -13f, -0.6f, Math.max(x + s * 2.2f, x + s * 3.4f), -11.2f, 0.6f, 0xC9303F);
            Geo.box(b, p, light, Math.min(x + s * 2.0f, x + s * 2.9f), -14.2f, -0.45f, Math.max(x + s * 2.0f, x + s * 2.9f), -13f, 0.45f, 0xEFE2C4);
        }
    }

    private static void catEars(VertexConsumer b, PoseStack.Pose p, int light) {
        for (int s = -1; s <= 1; s += 2) {
            float in = s * 1.2f, out = s * 4.4f;
            Geo.box(b, p, light, Math.min(in, out), -9.2f, -1.2f, Math.max(in, out), -8f, 0.2f, 0x2B2D36);
            Geo.box(b, p, light, Math.min(s * 1.8f, s * 4.0f), -10.4f, -1.1f, Math.max(s * 1.8f, s * 4.0f), -9.2f, 0.1f, 0x2B2D36);
            Geo.box(b, p, light, Math.min(s * 2.5f, s * 3.6f), -11.5f, -1.0f, Math.max(s * 2.5f, s * 3.6f), -10.4f, 0f, 0x2B2D36);
            Geo.box(b, p, light, Math.min(s * 2.0f, s * 3.7f), -10.2f, -1.3f, Math.max(s * 2.0f, s * 3.7f), -8.3f, -1.1f, 0xFF8FB8); // inner ear
        }
    }

    private static void wizard(VertexConsumer b, PoseStack.Pose p, int light) {
        int cloth = 0x4B2A9E, dark = 0x3A1F80;
        Geo.box(b, p, light, -6.5f, -8.7f, -6.5f, 6.5f, -8f, 6.5f, dark);
        Geo.box(b, p, light, -4.2f, -11f, -4.2f, 4.2f, -8.7f, 4.2f, cloth);
        Geo.box(b, p, light, -4.35f, -10f, -4.35f, 4.35f, -8.7f, 4.35f, GOLD);
        Geo.box(b, p, light, -3.2f, -13.2f, -3.0f, 3.2f, -11f, 3.4f, cloth);
        Geo.box(b, p, light, -2.2f, -15.2f, -1.6f, 2.2f, -13.2f, 2.9f, dark);
        Geo.box(b, p, light, -1.3f, -16.8f, -0.2f, 1.3f, -15.2f, 2.6f, cloth);
        Geo.box(b, p, light, -0.7f, -17.8f, 1.0f, 0.7f, -16.8f, 2.6f, dark);
        Geo.box(b, p, light, -0.8f, -12.6f, -3.2f, 0.8f, -11.4f, -3.0f, 0xFFE98A); // star
    }
}
