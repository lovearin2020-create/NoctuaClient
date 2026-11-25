package dev.lumen.mods;

import dev.lumen.core.Setting;
import dev.lumen.core.TextHud;
import dev.lumen.util.R;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;

/** Your hidden saturation, outlined in gold over the hunger bar (one outlined icon = 2 saturation), and as a number if you like. */
public class Saturation extends TextHud {
    private final Setting.Bool overlay = bool("Show On Hunger Bar", true);
    private final Setting.Bool number = bool("Show Number", false);
    private final Setting.Col gold = col("Outline Color", 0xFFFFC83D);
    private float shown;

    public Saturation() { super("Saturation", "Your hidden food saturation.", CENTER, 100, END, 26); }

    protected String text(boolean editing) {
        if (mc.thePlayer == null || !(number.on() || editing)) return null;
        return String.format("Saturation %.1f", mc.thePlayer.getFoodStats().getSaturationLevel());
    }

    public void render(ScaledResolution sr, boolean editing) {
        if (overlay.on() && !editing && mc.thePlayer != null && mc.playerController != null && mc.playerController.shouldDrawHUD()
                && !(mc.thePlayer.ridingEntity instanceof EntityLivingBase)) {
            float sat = mc.thePlayer.getFoodStats().getSaturationLevel();
            shown += (sat - shown) * 0.25f; // eases instead of jumping when you eat
            if (Math.abs(sat - shown) < 0.02f) shown = sat;
            int right = sr.getScaledWidth() / 2 + 91, y = sr.getScaledHeight() - 39, c = gold.get();
            for (int i = 0; i < 10; i++) {
                float part = Math.max(0f, Math.min(1f, (shown - i * 2f) / 2f));
                if (part <= 0.01f) break;
                // The bar fills from the right, so a part-filled icon is outlined from its right edge.
                float x1 = right - i * 8, w = Math.round(9 * part), x0 = x1 - w;
                R.rect(x0, y - 0.5f, w, 1, c);
                R.rect(x0, y + 8.5f, w, 1, c);
                R.rect(x1 - 1, y + 0.5f, 1, 8, c);
                if (part > 0.99f) R.rect(x0, y + 0.5f, 1, 8, c);
                R.rect(x0 + (part > 0.99f ? 1 : 0), y + 0.5f, w - 1 - (part > 0.99f ? 1 : 0), 8, R.alpha(c, 0.16f));
            }
            R.reset();
        }
        super.render(sr, editing);
    }
}
