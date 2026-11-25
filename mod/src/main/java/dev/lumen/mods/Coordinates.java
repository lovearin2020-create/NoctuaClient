package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class Coordinates extends HudModule {
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool direction = bool("Direction", true);
    private final Setting.Bool biome = bool("Biome", false);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private float w = 70;

    public Coordinates() { super("Coordinates", "Position and facing direction.", START, 4, START, 40); }

    public float width() { return lumen() ? 124 : w; }
    public float height() { return lumen() ? (direction.on() || biome.on() ? 32 : 18) : 6 + 30 + (biome.on() ? 10 : 0); }

    public void draw(boolean editing) {
        if (mc.thePlayer == null) return;
        int x = MathHelper.floor_double(mc.thePlayer.posX), y = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY), z = MathHelper.floor_double(mc.thePlayer.posZ);
        String[] lines = {"X: " + x, "Y: " + y, "Z: " + z};
        String b = null;
        if (biome.on() && mc.theWorld != null) {
            try { b = mc.theWorld.getBiomeGenForCoords(new BlockPos(x, y, z)).biomeName; } catch (Throwable t) { b = "Unknown"; }
        }
        if (lumen()) {
            if (background.on()) panel(0, 0, 124, height());
            String[] k = {"X", "Y", "Z"}; int[] v = {x, y, z};
            for (int i = 0; i < 3; i++) {
                float cx = 8 + i * 39;
                // same margin above the first line as below the last; label and number share one middle
                Fnt.draw(k[i], cx, mid(6, 6.3f, 6), 6, 0xFF8891A5);
                Fnt.drawB(String.valueOf(v[i]), cx + 8, cap(6, 9), 9, color.get());
            }
            if (direction.on() || b != null) {
                R.rect(6, 16, 112, 1, R.alpha(R.ACCENT, 0.35f));
                float ty = cap(20.8f, 7);
                if (b != null) Fnt.draw(Fnt.fit(b, 7, 60), 8, ty, 7, 0xFF8891A5);
                if (direction.on()) {
                    String d = DIRS[MathHelper.floor_double(mc.thePlayer.rotationYaw * 8.0F / 360.0F + 0.5D) & 7];
                    // beside the biome it sits on the right; alone it starts the line
                    float dx = b != null ? 116 - Fnt.width("Facing " + d, 7) : 8;
                    Fnt.draw("Facing", dx, ty, 7, 0xFF8891A5);
                    Fnt.drawB(d, dx + Fnt.width("Facing ", 7), ty, 7, 0xFFDDE3F0);
                }
            }
            return;
        }
        float max = 58;
        for (String s : lines) max = Math.max(max, mc.fontRendererObj.getStringWidth(s) + 26);
        if (b != null) max = Math.max(max, mc.fontRendererObj.getStringWidth(b) + 10);
        w = max;
        if (background.on()) R.roundRect(0, 0, w, height(), 3, 0x70000000);
        for (int i = 0; i < 3; i++) R.mcText(lines[i], 5, 4 + i * 10, color.get(), true);
        if (b != null) R.mcText(b, 5, 34, 0xFFB8C0D0, true);
        if (direction.on()) {
            String d = DIRS[MathHelper.floor_double(mc.thePlayer.rotationYaw * 8.0F / 360.0F + 0.5D) & 7];
            R.mcText(d, w - 5 - mc.fontRendererObj.getStringWidth(d), 14, R.ACCENT, true);
        }
    }
}
