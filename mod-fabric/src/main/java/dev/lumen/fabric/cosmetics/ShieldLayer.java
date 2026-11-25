package dev.lumen.fabric.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Shields strapped to the outside of the left arm: pixel art, one model pixel thick.
 *  Like every player layer it is only drawn with the body, so you see your own in third person only. */
public class ShieldLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public ShieldLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

    // Each letter is one pixel; '.' is empty. Ten wide, drawn from the top of the back downwards.
    private static final String[] KNIGHT = {
            "SSSSSSSSSS", "SBBBWWBBBS", "SBBBWWBBBS", "SBBBWWBBBS", "SWWWWWWWWS", "SWWWWWWWWS",
            "SBBBWWBBBS", "SBBBWWBBBS", ".SBBWWBBS.", ".SBBWWBBS.", "..SBWWBS..", "...SWWS...", "....SS...."};
    private static final String[] ROUND = {
            "...IIII...", ".IIOOOOII.", ".IOOooOOI.", "IOOooooOOI", "IOooIIooOI", "IOoIGGIoOI",
            "IOoIGGIoOI", "IOooIIooOI", "IOOooooOOI", ".IOOooOOI.", ".IIOOOOII.", "...IIII..."};
    private static final String[] LUMEN = {
            "NNNNNNNNNN", "NDDDCCDDDN", "NDDDCCDDDN", "NDDCccCDDN", "NCCccccCCN", "NCCccccCCN",
            "NDDCccCDDN", "NDDDCCDDDN", ".NDDCCDDN.", ".NDDDDDDN.", "..NDDDDN..", "...NDDN...", "....NN...."};
    private static final String[] RUBY = {
            "GGGGGGGGGG", "GKKKKKKKKG", "GKKRRRRKKG", "GKRrrrrRKG", "GKRrwwrRKG", "GKRrwrrRKG",
            "GKRrrrrRKG", "GKKRrrRKKG", ".GKKRRKKG.", ".GKKKKKKG.", "..GKKKKG..", "...GKKG...", "....GG...."};

    private static int color(char c) {
        switch (c) {
            case 'S': return 0xB8C0CC; case 'B': return 0x2F5FD0; case 'W': return 0xF2F4F8;
            case 'I': return 0x8A93A3; case 'O': return 0x8B5A2B; case 'o': return 0xA9713A; case 'G': return 0xF5C542;
            case 'N': return 0x1B2A4A; case 'D': return 0x101A33; case 'C': return 0x38D9F5; case 'c': return 0xC8F6FF;
            case 'K': return 0x15161B; case 'R': return 0xC1121F; case 'r': return 0xFF3B55; case 'w': return 0xFFD0D8;
            default: return -1;
        }
    }

    private static String[] art(String id) {
        return id.equals("shield_knight") ? KNIGHT : id.equals("shield_round") ? ROUND : id.equals("shield_lumen") ? LUMEN : id.equals("shield_ruby") ? RUBY : null;
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
        String id = ((OutfitState) state).lumen$shield();
        if (id == null || state.isInvisible) return;
        String[] art = art(id);
        if (art == null) return;
        ps.pushPose();
        getParentModel().root().translateAndRotate(ps);
        getParentModel().leftArm.translateAndRotate(ps);
        // Arm space: shoulder at the origin, the arm hangs from y = -2 to 10 and its outer face is at x = 3.
        ps.translate(3.15f * 0.0625f, -1.6f * 0.0625f, 0f);
        Draw.rotate(ps, Axis.YP.rotationDegrees(90f)); // the art is drawn facing forwards; turn it to face outwards
        ps.scale(0.0625f * 0.9f, 0.0625f * 0.9f, 0.0625f);
        out.submitCustomGeometry(ps, RenderTypes.entityCutout(Geo.white()), (p, b) -> {
            for (int row = 0; row < art.length; row++) {
                for (int col = 0; col < art[row].length(); col++) {
                    int c = color(art[row].charAt(col));
                    if (c >= 0) Geo.box(b, p, light, col - 5f, row, 0f, col - 4f, row + 1f, 1f, c);
                }
            }
        });
        ps.popPose();
    }
}
