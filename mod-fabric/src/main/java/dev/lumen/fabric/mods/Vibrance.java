package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.Category;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.core.Setting;
import net.minecraft.resources.Identifier;

/** Richer colours for the world, like the Digital Vibrance slider of a graphics driver. The HUD keeps its own colours. */
public class Vibrance extends Module {
    public final Setting.Num amount = num("Amount (%)", 50, 5, 100, 5);
    private Boolean available;

    public Vibrance() { super("Vibrance", "More vivid colours.", Category.VISUAL); }

    /** The post effect to run this frame, or null. One effect file per 5 % step ships in the mod's resources. */
    public Identifier effect() {
        if (available == null) {
            try { available = mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("lumen", "post_effect/vibrance_1.json")).isPresent(); }
            catch (Throwable t) { available = false; }
        }
        return available ? Identifier.fromNamespaceAndPath("lumen", "vibrance_" + Math.max(1, Math.min(20, Math.round(amount.f() / 5f)))) : null;
    }
}
