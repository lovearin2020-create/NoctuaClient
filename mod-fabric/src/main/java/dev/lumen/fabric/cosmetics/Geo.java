package dev.lumen.fabric.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Plain coloured geometry for the cosmetics, in model pixels (1/16 block): a white texture tinted per vertex. */
final class Geo {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("lumen", "white");
    private static boolean whiteReady;

    private Geo() {}

    static Identifier white() {
        if (!whiteReady) {
            NativeImage img = new NativeImage(1, 1, false);
            img.setPixel(0, 0, 0xFFFFFFFF);
            Minecraft.getInstance().getTextureManager().register(WHITE, new DynamicTexture(() -> "Lumen white", img));
            whiteReady = true;
        }
        return WHITE;
    }

    static int shade(int rgb, float k) {
        return 0xFF000000 | Math.min(255, (int) ((rgb >> 16 & 255) * k)) << 16 | Math.min(255, (int) ((rgb >> 8 & 255) * k)) << 8 | Math.min(255, (int) ((rgb & 255) * k));
    }

    /** Every vertex gets the same "up" normal, so the game lights the whole piece evenly and the face shading below gives it shape. */
    static void vertex(VertexConsumer b, PoseStack.Pose pose, int light, int argb, float x, float y, float z) {
        b.addVertex(pose, x, y, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
    }

    private static void lit(VertexConsumer b, PoseStack.Pose pose, int light, int argb, float x, float y, float z, float ny) {
        b.addVertex(pose, x, y, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, ny, 0);
    }

    /**
     * A flat piece that must look the same from both sides (a feather). The game lights the back of a face as if it
     * pointed the other way, which would leave one side of a wing dark. So the piece is sent once facing each way,
     * both lit from above, and drawn with a render type that skips whichever copy faces away from the camera.
     */
    static void twoSided(VertexConsumer b, PoseStack.Pose pose, int light, float z, int c0, float x0, float y0, int c1, float x1, float y1, int c2, float x2, float y2, int c3, float x3, float y3) {
        lit(b, pose, light, c0, x0, y0, z, -1); lit(b, pose, light, c1, x1, y1, z, -1); lit(b, pose, light, c2, x2, y2, z, -1); lit(b, pose, light, c3, x3, y3, z, -1);
        lit(b, pose, light, c3, x3, y3, z, -1); lit(b, pose, light, c2, x2, y2, z, -1); lit(b, pose, light, c1, x1, y1, z, -1); lit(b, pose, light, c0, x0, y0, z, -1);
    }

    static void quad(VertexConsumer b, PoseStack.Pose pose, int light, int argb, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(b, pose, light, argb, ax, ay, az); vertex(b, pose, light, argb, bx, by, bz);
        vertex(b, pose, light, argb, cx, cy, cz); vertex(b, pose, light, argb, dx, dy, dz);
    }

    /** A box between two corners (y grows downwards, like the player model). Faces are shaded a little so edges read. */
    static void box(VertexConsumer b, PoseStack.Pose p, int light, float x1, float y1, float z1, float x2, float y2, float z2, int rgb) {
        int top = shade(rgb, 1f), bottom = shade(rgb, 0.6f), front = shade(rgb, 0.86f), side = shade(rgb, 0.74f);
        quad(b, p, light, top, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        quad(b, p, light, bottom, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1);
        quad(b, p, light, front, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        quad(b, p, light, front, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2);
        quad(b, p, light, side, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1);
        quad(b, p, light, side, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }
}
