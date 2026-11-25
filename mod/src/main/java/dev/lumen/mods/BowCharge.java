package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Shows how far the bow is drawn while you hold right click. */
public class BowCharge extends HudModule {
    private final Setting.Bool percent = bool("Show Percent", true);
    private final Setting.Col color = col("Bar Color", 0xFFFFD84D);
    private final Setting.Col full = col("Full Charge Color", 0xFF4ADE80);

    public BowCharge() { super("Bow Charge", "Bow draw strength next to the crosshair.", CENTER, -20, CENTER, 12); optionalBackground(); }
    protected boolean showing(boolean editing) {
        return editing || (mc.thePlayer != null && mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse() != null && mc.thePlayer.getItemInUse().getItem() == Items.bow);
    }

    protected boolean hasLumenStyle() { return false; }
    public float width() { return 40; }
    public float height() { return percent.on() ? 14 : 4; }

    /** Same curve the game uses for arrow speed: full power after one second. */
    static float power(int ticks) {
        float f = ticks / 20f;
        f = (f * f + f * 2f) / 3f;
        return Math.min(1f, f);
    }

    public void draw(boolean editing) {
        float p;
        if (editing) p = 0.7f;
        else {
            if (mc.thePlayer == null || !mc.thePlayer.isUsingItem()) return;
            ItemStack s = mc.thePlayer.getItemInUse();
            if (s == null || s.getItem() != Items.bow) return;
            p = power(mc.thePlayer.getItemInUseDuration());
        }
        int c = p >= 1f ? full.get() : color.get();
        R.rect(0, 0, 40, 4, 0x90000000);
        R.rect(1, 1, 38 * p, 2, c);
        if (percent.on()) {
            String s = Math.round(p * 100) + "%";
            R.mcText(s, 20 - mc.fontRendererObj.getStringWidth(s) / 2f, 6, c, true);
        }
    }
}
