package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.phys.HitResult;

/**
 * The parts of the 1.7 feel that still exist in current versions. Sword blocking was removed from the
 * game itself, so block-hitting cannot be brought back client-side.
 */
public class OldAnimations extends Module {
    public final Setting.Bool noCooldownDip = bool("No Attack Cooldown Dip", true);
    public final Setting.Bool noReequip = bool("No Re-equip Animation", true);
    private final Setting.Bool punching = bool("Punch While Using Item", true);

    public OldAnimations() { super("1.7 Animations", "Old-style held item: no cooldown dip or re-equip bob.", Category.VISUAL); }

    /** Purely visual swing while using an item and holding attack on a block. No packet is sent. */
    public void onTick() {
        var p = mc.player;
        if (!punching.on() || mc.gui.screen() != null || !mc.options.keyAttack.isDown() || !p.isUsingItem()) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        dev.lumen.fabric.compat.Compat.visualSwing(p);
    }
}
