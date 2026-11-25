package dev.lumen.cosmetics;

import dev.lumen.online.Online;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/** Shields strapped to the outside of the left arm: pixel art, one model pixel thick, compiled once into a display list.
 *  Like every player layer it is only drawn with the body, so you see your own in third person only. */
public class ShieldLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final Map<String, Integer> lists = new HashMap<String, Integer>();
    private final RenderPlayer renderer;

    public ShieldLayer(RenderPlayer renderer) { this.renderer = renderer; }

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

    private static int list(String id) {
        Integer known = lists.get(id);
        if (known != null) return known;
        String[] art = id.equals("shield_knight") ? KNIGHT : id.equals("shield_round") ? ROUND : id.equals("shield_lumen") ? LUMEN : id.equals("shield_ruby") ? RUBY : null;
        int l = -1;
        if (art != null) {
            l = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(l, GL11.GL_COMPILE);
            GL11.glBegin(GL11.GL_QUADS);
            for (int row = 0; row < art.length; row++) {
                for (int col = 0; col < art[row].length(); col++) {
                    int c = color(art[row].charAt(col));
                    if (c >= 0) Shapes.box(col - 5f, row, 0f, col - 4f, row + 1f, 1f, c);
                }
            }
            GL11.glEnd();
            GL11.glEndList();
        }
        lists.put(id, l);
        return l;
    }

    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt, float age, float yaw, float pitch, float scale) {
        String id = Online.wear(p, "shield");
        if (id == null || p.isInvisible()) return;
        int l = list(id);
        if (l < 0) return;
        GlStateManager.pushMatrix();
        if (p.isSneaking()) GlStateManager.translate(0f, 0.2f, 0f);
        renderer.getMainModel().bipedLeftArm.postRender(0.0625f);
        // Arm space: shoulder at the origin, the arm hangs from y = -2 to 10 and its outer face is at x = 3.
        GlStateManager.translate(3.15f * 0.0625f, -1.6f * 0.0625f, 0f);
        GlStateManager.rotate(90f, 0f, 1f, 0f); // the art is drawn facing forwards; turn it to face outwards
        GlStateManager.scale(0.0625f * 0.9f, 0.0625f * 0.9f, 0.0625f);
        GlStateManager.disableTexture2D();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableCull();
        GL11.glCallList(l);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GlStateManager.resetColor();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    public boolean shouldCombineTextures() { return false; }
}
