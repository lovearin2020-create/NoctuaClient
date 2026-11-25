package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.resources.Identifier;

public class MotionBlur extends Module {
    public final Setting.Num amount = num("Amount", 4, 1, 8, 1);
    private Boolean available;

    public MotionBlur() { super("Motion Blur", "Blends frames together.", Category.VISUAL); }

    /** The post effect to run this frame, or null. The effect files ship in the mod's resources. */
    public Identifier effect() {
        if (available == null) {
            try { available = mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("lumen", "post_effect/motion_blur_1.json")).isPresent(); }
            catch (Throwable t) { available = false; }
        }
        return available ? Identifier.fromNamespaceAndPath("lumen", "motion_blur_" + amount.i()) : null;
    }
}
