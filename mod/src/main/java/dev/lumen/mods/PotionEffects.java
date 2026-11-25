package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class PotionEffects extends HudModule {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private final Setting.Bool names = bool("Effect Names", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private int rows = 1;

    public PotionEffects() { super("Potion Effects", "Active effects and durations.", END, 4, START, 4); optionalBackground(); }
    protected boolean showing(boolean editing) { return editing || rows > 0; }

    private float lumenW = 112;
    public float width() { return lumen() ? (names.on() ? lumenW : 62) : names.on() ? 100 : 50; }
    public float height() { return lumen() ? Math.max(1, rows) * 22 + 4 : Math.max(1, rows) * 21; }
    /** The longest each effect has been seen to last, for the bar under it. */
    private final java.util.Map<Integer, Integer> longest = new java.util.HashMap<Integer, Integer>();

    public void draw(boolean editing) {
        List<PotionEffect> list = new ArrayList<PotionEffect>();
        if (mc.thePlayer != null) list.addAll(mc.thePlayer.getActivePotionEffects());
        if (list.isEmpty() && editing) list.add(new PotionEffect(Potion.moveSpeed.id, 1800, 1));
        rows = list.size();
        if (lumen()) { drawLumen(list); return; }
        int y = 0;
        for (PotionEffect e : list) {
            Potion p = Potion.potionTypes[e.getPotionID()];
            if (p == null) continue;
            if (p.hasStatusIcon()) {
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.enableBlend();
                mc.getTextureManager().bindTexture(INVENTORY);
                int idx = p.getStatusIconIndex();
                mc.ingameGUI.drawTexturedModalRect(0, y + 1, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18);
            }
            String time = Potion.getDurationString(e);
            if (names.on()) {
                String n = I18n.format(p.getName());
                if (e.getAmplifier() > 0 && e.getAmplifier() < 10) n += " " + (e.getAmplifier() + 1);
                R.mcText(n, 21, y + 2, color.get(), true);
                R.mcText(time, 21, y + 11, 0xFFB8C0D0, true);
            } else {
                R.mcText(time, 21, y + 6, color.get(), true);
            }
            y += 21;
        }
    }

    private void drawLumen(List<PotionEffect> list) {
        if (list.isEmpty()) return;
        // wide enough for the longest name, so none is cut short
        float need = 112;
        for (PotionEffect e : list) {
            Potion q = Potion.potionTypes[e.getPotionID()];
            if (q == null) continue;
            String n = I18n.format(q.getName()) + (e.getAmplifier() > 0 && e.getAmplifier() < 10 ? " " + (e.getAmplifier() + 1) : "");
            need = Math.max(need, 26 + dev.lumen.util.Fnt.width(n, 8) + 10 + dev.lumen.util.Fnt.width(Potion.getDurationString(e), 8) + 7);
        }
        lumenW = Math.min(170, need);
        float w = width();
        panel(0, 0, w, height());
        float y = 2;
        for (PotionEffect e : list) {
            Potion p = Potion.potionTypes[e.getPotionID()];
            if (p == null) continue;
            if (p.hasStatusIcon()) {
                GlStateManager.color(1, 1, 1, 1);
                GlStateManager.enableBlend();
                mc.getTextureManager().bindTexture(INVENTORY);
                int idx = p.getStatusIconIndex();
                mc.ingameGUI.drawTexturedModalRect(4, (int) y + 2, idx % 8 * 18, 198 + idx / 8 * 18, 18, 18);
            }
            Integer top = longest.get(e.getPotionID());
            if (top == null || e.getDuration() > top) { top = e.getDuration(); longest.put(e.getPotionID(), top); }
            boolean low = e.getDuration() < 200;
            String time = Potion.getDurationString(e);
            float tw = dev.lumen.util.Fnt.width(time, 8);
            dev.lumen.util.Fnt.draw(time, w - 7 - tw, cap(y + 5.75f, 8), 8, low ? 0xFFFFB454 : 0xFFFFFFFF); // name and bar together sit in the middle of the icon
            if (names.on()) {
                String n = I18n.format(p.getName());
                if (e.getAmplifier() > 0 && e.getAmplifier() < 10) n += " " + (e.getAmplifier() + 1);
                dev.lumen.util.Fnt.draw(dev.lumen.util.Fnt.fit(n, 8, w - 36 - tw), 26, cap(y + 5.75f, 8), 8, color.get());
            }
            float bw = w - 33, f = Math.max(0, Math.min(1, e.getDuration() / (float) Math.max(1, top)));
            R.roundRect(26, y + 14.25f, bw, 2, 1, 0x30FFFFFF);
            R.roundRect(26, y + 14.25f, Math.max(2, bw * f), 2, 1, low ? 0xFFFFB454 : 0xFF000000 | p.getLiquidColor());
            y += 22;
        }
    }
}
