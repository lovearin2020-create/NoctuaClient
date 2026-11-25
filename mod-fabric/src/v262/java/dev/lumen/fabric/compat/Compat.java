package dev.lumen.fabric.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Calls whose signature differs between game versions. */
public final class Compat {
    public static void submitPart(SubmitNodeCollector out, ModelPart part, PoseStack ps, RenderType type, int light, int tint) {
        out.submitModelPart(part, ps, type, light, OverlayTexture.NO_OVERLAY, null, tint, null);
    }

    /** Starts a purely visual arm swing; nothing is sent to the server. */
    public static void visualSwing(LocalPlayer p) {
        if (!p.swinging || p.swingTime >= 3 || p.swingTime < 0) { p.swingTime = -1; p.swinging = true; }
    }

    public static void openFile(java.io.File file) {
        net.minecraft.util.Util.getPlatform().openFile(file);
    }

    public static Screen optionsScreen(Screen parent, Options options) {
        return new OptionsScreen(parent, options, false);
    }

    /** Tells the game that a screen wants typed characters (needed for the search box and text settings). */
    public static void textInput(Object owner, boolean on) {
        // Typed characters always reach the open screen on this version.
    }

    /** The Lumen icon on the game window and task bar. */
    public static void windowIcon() {
        java.util.List<java.nio.ByteBuffer> pixels = new java.util.ArrayList<>();
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            String[] files = {"lumen-16.png", "lumen-32.png", "lumen-48.png"};
            org.lwjgl.glfw.GLFWImage.Buffer icons = org.lwjgl.glfw.GLFWImage.malloc(files.length, stack);
            for (int i = 0; i < files.length; i++) {
                java.awt.image.BufferedImage img;
                try (java.io.InputStream in = dev.lumen.fabric.Lumen.class.getResourceAsStream("/assets/lumen/icon/" + files[i])) { img = javax.imageio.ImageIO.read(in); }
                int w = img.getWidth(), h = img.getHeight();
                java.nio.ByteBuffer b = org.lwjgl.system.MemoryUtil.memAlloc(w * h * 4);
                pixels.add(b);
                for (int argb : img.getRGB(0, 0, w, h, null, 0, w)) b.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
                b.flip();
                icons.position(i).width(w).height(h).pixels(b);
            }
            icons.position(0);
            org.lwjgl.glfw.GLFW.glfwSetWindowIcon(net.minecraft.client.Minecraft.getInstance().getWindow().handle(), icons);
        } catch (Throwable ignored) {
            // Not every platform lets a window change its icon; the game keeps its own then.
        } finally {
            for (java.nio.ByteBuffer b : pixels) org.lwjgl.system.MemoryUtil.memFree(b);
        }
    }

    /** See-through entity geometry that is only drawn when it faces the camera (the name differs between game versions). */
    public static RenderType translucentCull(net.minecraft.resources.Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentCullItemTarget(texture);
    }
}
