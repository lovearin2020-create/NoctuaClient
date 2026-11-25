package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/** Shows how far the bow is drawn while you hold right click. */
public class BowCharge extends HudModule {
    private final Setting.Bool percent = bool("Show Percent", true);
    private final Setting.Col color = col("Bar Color", 0xFFFFD84D);
    private final Setting.Col full = col("Full Charge Color", 0xFF4ADE80);

    public BowCharge() { super("Bow Charge", "Bow draw strength next to the crosshair.", CENTER, -20, CENTER, 12); optionalBackground(); }
    protected boolean showing(boolean editing) { return editing || (mc.player != null && mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof BowItem); }

    public float width() { return 40; }
    public float height() { return percent.on() ? 14 : 4; }

    public void draw(boolean editing) {
        float p;
        if (editing) p = 0.7f;
        else {
            if (mc.player == null || !mc.player.isUsingItem()) return;
            ItemStack s = mc.player.getUseItem();
            if (s.isEmpty() || !(s.getItem() instanceof BowItem)) return;
            p = BowItem.getPowerForTime(mc.player.getTicksUsingItem());
        }
        int c = p >= 1f ? full.get() : color.get();
        Draw.rect(0, 0, 40, 4, 0x90000000);
        Draw.rect(1, 1, 38 * p, 2, c);
        if (percent.on()) {
            String s = Math.round(p * 100) + "%";
            Draw.text(s, 20 - mc.font.width(s) / 2f, 6, c, true);
        }
    }
}
