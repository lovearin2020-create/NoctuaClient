package dev.lumen.cosmetics;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Untextured, world-lit boxes for hats and shields, in model pixels (1/16 block). */
final class Shapes {
    private Shapes() {}

    static void begin() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableCull();
        GL11.glBegin(GL11.GL_QUADS);
    }

    static void end() {
        GL11.glEnd();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GL11.glColor4f(1f, 1f, 1f, 1f); // the boxes set colours directly, so put the real state back too
        GlStateManager.resetColor();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void shade(int rgb, float k) {
        GL11.glColor4f((rgb >> 16 & 255) / 255f * k, (rgb >> 8 & 255) / 255f * k, (rgb & 255) / 255f * k, 1f);
    }

    /** A box between two corners (y grows downwards, like the player model). Faces are shaded a little so edges read. */
    static void box(float x1, float y1, float z1, float x2, float y2, float z2, int rgb) {
        // top (smaller y)
        shade(rgb, 1f); GL11.glNormal3f(0, -1, 0);
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x2, y1, z1); GL11.glVertex3f(x2, y1, z2); GL11.glVertex3f(x1, y1, z2);
        // bottom
        shade(rgb, 0.6f); GL11.glNormal3f(0, 1, 0);
        GL11.glVertex3f(x1, y2, z2); GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x2, y2, z1); GL11.glVertex3f(x1, y2, z1);
        // front and back
        shade(rgb, 0.86f); GL11.glNormal3f(0, 0, -1);
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x1, y2, z1); GL11.glVertex3f(x2, y2, z1); GL11.glVertex3f(x2, y1, z1);
        GL11.glNormal3f(0, 0, 1);
        GL11.glVertex3f(x2, y1, z2); GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x1, y2, z2); GL11.glVertex3f(x1, y1, z2);
        // sides
        shade(rgb, 0.74f); GL11.glNormal3f(-1, 0, 0);
        GL11.glVertex3f(x1, y1, z2); GL11.glVertex3f(x1, y2, z2); GL11.glVertex3f(x1, y2, z1); GL11.glVertex3f(x1, y1, z1);
        GL11.glNormal3f(1, 0, 0);
        GL11.glVertex3f(x2, y1, z1); GL11.glVertex3f(x2, y2, z1); GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x2, y1, z2);
    }
}
