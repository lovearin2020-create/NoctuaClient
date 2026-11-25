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
        out.submitModelPart(part, ps, type, light, OverlayTexture.NO_OVERLAY, null, tint);
    }

    /** Starts a purely visual arm swing; nothing is sent to the server. */
    public static void visualSwing(LocalPlayer p) {
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);
    }

    public static void openFile(java.io.File file) {
        com.mojang.blaze3d.Blaze3D.openPath(file.toPath());
    }

    public static Screen optionsScreen(Screen parent, Options options) {
        return new OptionsScreen(parent, options);
    }

    /** Tells the game that a screen wants typed characters (needed for the search box and text settings). */
    public static void textInput(Object owner, boolean on) {
        try {
            var input = net.minecraft.client.Minecraft.getInstance().textInputManager();
            if (on) input.startTextInput(owner); else input.stopTextInput(owner);
        } catch (Throwable ignored) {}
    }

    /** The Lumen icon on the game window and task bar. This version's window takes a list of PNGs, largest wanted first. */
    public static void windowIcon() {
        try {
            java.util.List<net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream>> icons = new java.util.ArrayList<>();
            for (String f : new String[]{"lumen-48.png", "lumen-32.png", "lumen-16.png"}) {
                icons.add(() -> dev.lumen.fabric.Lumen.class.getResourceAsStream("/assets/lumen/icon/" + f));
            }
            Object window = net.minecraft.client.Minecraft.getInstance().getWindow();
            java.lang.reflect.Method m = window.getClass().getDeclaredMethod("setIcon", java.util.List.class);
            m.setAccessible(true);
            m.invoke(window, icons);
        } catch (Throwable ignored) {
            // Not every platform lets a window change its icon; the game keeps its own then.
        }
    }

    /** See-through entity geometry that is only drawn when it faces the camera (the name differs between game versions). */
    public static RenderType translucentCull(net.minecraft.resources.Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentCull(texture);
    }
}
