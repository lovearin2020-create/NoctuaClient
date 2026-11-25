package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * A card for the player you are fighting: face, name, health and distance. Hypixel does not allow showing other
 * players' health or distance, so the card stays hidden there.
 */
public class TargetHud extends HudModule {
    private static final int W = 150, H = 44;
    private final Setting.Bool onAim = bool("Show When Aiming", true);
    private final Setting.Num keep = num("Stay For (seconds)", 3, 1, 10, 0.5);
    private final Setting.Bool distance = bool("Show Distance", true);
    private final Setting.Bool compare = bool("Show Winning / Losing", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col accent = col("Bar Color", 0xFF3DDC84);

    private AbstractClientPlayer target;
    private long seenAt;
    private float fade, bar = 1f, trail = 1f;

    public TargetHud() { super("Target HUD", "The player you are fighting. Off on Hypixel (not allowed there).", CENTER, 14, CENTER, 26); }

    protected boolean hasLumenStyle() { return false; }
    public float width() { return W; }
    public float height() { return H; }

    private void set(Entity e) {
        if (!(e instanceof AbstractClientPlayer) || e == mc.thePlayer || e.isInvisible()) return;
        if (e != target) { bar = trail = ratio((AbstractClientPlayer) e); }
        target = (AbstractClientPlayer) e;
        seenAt = System.currentTimeMillis();
    }

    private static float ratio(AbstractClientPlayer p) { return Math.max(0f, Math.min(1f, p.getHealth() / Math.max(1f, p.getMaxHealth()))); }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) { if (e.entityPlayer == mc.thePlayer) set(e.target); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        if (onAim.on() && mc.pointedEntity != null) set(mc.pointedEntity);
        if (target != null && (target.isDead || target.worldObj != mc.theWorld || System.currentTimeMillis() - seenAt > (long) (keep.value * 1000))) {
            if (fade < 0.02f) target = null;
        }
    }

    protected boolean allowed() { return !Util.onHypixel(); }

    public void draw(boolean editing) {
        if (mc.thePlayer == null) return;
        AbstractClientPlayer t = editing ? mc.thePlayer : target;
        boolean live = editing || (allowed() && t != null && !t.isDead && System.currentTimeMillis() - seenAt <= (long) (keep.value * 1000));
        fade = Util.lerp(fade, live ? 1f : 0f, 0.25f);
        if (t == null || fade < 0.02f) return;

        float hp = t.getHealth() + t.getAbsorptionAmount(), r = ratio(t);
        bar = Util.lerp(bar, r, 0.25f);
        trail = trail < bar ? bar : Util.lerp(trail, bar, 0.06f); // the pale part that lags behind shows the last hit
        float a = fade;

        GlStateManager.pushMatrix();
        GlStateManager.translate(W / 2f, H / 2f, 0);
        GlStateManager.scale(0.92f + 0.08f * a, 0.92f + 0.08f * a, 1f);
        GlStateManager.translate(-W / 2f, -H / 2f, 0);
        if (background.on()) {
            R.roundRect(0, 0, W, H, 7, R.alpha(0xFF0E1016, 0.78f * a));
            R.roundOutline(0, 0, W, H, 7, 1f, R.alpha(0xFFFFFFFF, 0.08f * a));
        }
        // face, with the hat layer, flashing red when hit
        float hurt = t.hurtTime / 10f;
        mc.getTextureManager().bindTexture(t.getLocationSkin());
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f - hurt * 0.55f, 1f - hurt * 0.55f, a);
        Gui.drawScaledCustomSizeModalRect(6, 6, 8, 8, 8, 8, 32, 32, 64, 64);
        Gui.drawScaledCustomSizeModalRect(6, 6, 40, 8, 8, 8, 32, 32, 64, 64);
        R.reset();

        float x = 44, right = W - 8;
        String hpText = String.format("%.1f", hp);
        float hw = Fnt.widthB(hpText, 9);
        int hc = r > 0.6f ? 0xFF3DDC84 : r > 0.3f ? 0xFFFFC83D : 0xFFFF5A5A;
        Fnt.drawB(Fnt.fit(t.getName(), 9, right - x - hw - 6), x, 6, 9, R.alpha(0xFFFFFFFF, a));
        Fnt.drawB(hpText, right - hw, 6, 9, R.alpha(hc, a));

        float bw = right - x, by = 21;
        R.roundRect(x, by, bw, 6, 3, R.alpha(0xFFFFFFFF, 0.14f * a));
        if (trail > bar + 0.005f) R.roundRect(x, by, Math.max(6, bw * trail), 6, 3, R.alpha(0xFFFFFFFF, 0.55f * a));
        int main = r > 0.6f ? accent.get() : hc;
        if (bar > 0.01f) R.roundRect(x, by, Math.max(6, bw * bar), 6, 3, R.alpha(main, a));
        if (t.getAbsorptionAmount() > 0) {
            float ab = Math.min(1f, t.getAbsorptionAmount() / Math.max(1f, t.getMaxHealth()));
            R.roundRect(x, by + 4, Math.max(4, bw * ab), 2, 1, R.alpha(0xFFFFC83D, a));
        }

        if (distance.on()) Fnt.draw(String.format("%.1f m", mc.thePlayer.getDistanceToEntity(t)), x, 31, 7, R.alpha(0xFF9AA3B5, a));
        if (compare.on() && !editing) {
            float mine = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
            // how far ahead or behind you are, in health points (2 = one heart)
            String s = mine > hp + 0.5f ? String.format("WINNING +%.1f", mine - hp) : mine < hp - 0.5f ? String.format("LOSING -%.1f", hp - mine) : "EVEN";
            int c = mine > hp + 0.5f ? 0xFF3DDC84 : mine < hp - 0.5f ? 0xFFFF5A5A : 0xFF9AA3B5;
            Fnt.draw(s, right - Fnt.width(s, 7, Fnt.BOLD, 0.8f), 31, 7, Fnt.BOLD, 0.8f, R.alpha(c, a), R.alpha(c, a));
        } else if (compare.on()) {
            Fnt.draw("EVEN", right - Fnt.width("EVEN", 7, Fnt.BOLD, 0.8f), 31, 7, Fnt.BOLD, 0.8f, 0xFF9AA3B5, 0xFF9AA3B5);
        }
        GlStateManager.popMatrix();
    }
}
