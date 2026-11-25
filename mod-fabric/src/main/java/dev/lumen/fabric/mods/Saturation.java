package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.Setting;
import dev.lumen.fabric.core.TextHud;
import dev.lumen.fabric.util.Draw;
import net.minecraft.world.entity.LivingEntity;

/** Your hidden saturation, outlined in gold over the hunger bar (one outlined icon = 2 saturation), and as a number if you like. */
public class Saturation extends TextHud {
    private final Setting.Bool overlay = bool("Show On Hunger Bar", true);
    private final Setting.Bool number = bool("Show Number", false);
    private final Setting.Col gold = col("Outline Color", 0xFFFFC83D);
    private float shown;

    public Saturation() { super("Saturation", "Your hidden food saturation.", CENTER, 146, END, 34); }

    protected String text(boolean editing) {
        if (mc.player == null || !(number.on() || editing)) return null;
        return String.format("Saturation %.1f", mc.player.getFoodData().getSaturationLevel());
    }

    @Override
    public void render(int sw, int sh, boolean editing) {
        if (overlay.on() && !editing && mc.player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer() && !(mc.player.getVehicle() instanceof LivingEntity)) {
            float sat = mc.player.getFoodData().getSaturationLevel();
            shown += (sat - shown) * 0.25f; // eases instead of jumping when you eat
            if (Math.abs(sat - shown) < 0.02f) shown = sat;
            int right = sw / 2 + 91, y = sh - 39, c = gold.get();
            for (int i = 0; i < 10; i++) {
                float part = Math.max(0f, Math.min(1f, (shown - i * 2f) / 2f));
                if (part <= 0.01f) break;
                // The bar fills from the right, so a part-filled icon is outlined from its right edge.
                boolean full = part > 0.99f;
                float x1 = right - i * 8, w = Math.round(9 * part), x0 = x1 - w;
                Draw.rect(x0, y - 0.5f, w, 1, c);
                Draw.rect(x0, y + 8.5f, w, 1, c);
                Draw.rect(x1 - 1, y + 0.5f, 1, 8, c);
                if (full) Draw.rect(x0, y + 0.5f, 1, 8, c);
                Draw.rect(x0 + (full ? 1 : 0), y + 0.5f, w - 1 - (full ? 1 : 0), 8, Draw.alpha(c, 0.16f));
            }
        }
        super.render(sw, sh, editing);
    }
}
