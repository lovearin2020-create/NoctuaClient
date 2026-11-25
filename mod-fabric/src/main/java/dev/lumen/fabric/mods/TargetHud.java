package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.HudModule;
import dev.lumen.fabric.core.Setting;
import dev.lumen.fabric.util.Draw;
import dev.lumen.fabric.util.Servers;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

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

    private static float ratio(AbstractClientPlayer p) { return Math.max(0f, Math.min(1f, p.getHealth() / Math.max(1f, p.getMaxHealth()))); }

    private void set(Entity e) {
        if (!(e instanceof AbstractClientPlayer p) || e == mc.player || e.isInvisible()) return;
        if (p != target) bar = trail = ratio(p);
        target = p;
        seenAt = System.currentTimeMillis();
    }

    @Override public void onAttack(Entity e) { set(e); }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (onAim.on() && mc.hitResult instanceof EntityHitResult hit) set(hit.getEntity());
        if (target != null && fade < 0.02f && (target.isRemoved() || target.level() != mc.level || System.currentTimeMillis() - seenAt > (long) (keep.value * 1000))) target = null;
    }

    public void draw(boolean editing) {
        if (mc.player == null) return;
        AbstractClientPlayer t = editing ? mc.player : target;
        boolean live = editing || (!Servers.onHypixel() && t != null && !t.isRemoved() && System.currentTimeMillis() - seenAt <= (long) (keep.value * 1000));
        fade = Draw.lerp(fade, live ? 1f : 0f, 0.25f);
        if (t == null || fade < 0.02f) return;

        float hp = t.getHealth() + t.getAbsorptionAmount(), r = ratio(t);
        bar = Draw.lerp(bar, r, 0.25f);
        trail = trail < bar ? bar : Draw.lerp(trail, bar, 0.06f); // the pale part that lags behind shows the last hit
        float a = fade;

        if (background.on()) {
            Draw.roundRect(0, 0, W, H, 7, Draw.alpha(0xFF0E1016, 0.78f * a));
            Draw.roundOutline(0, 0, W, H, 7, Draw.alpha(0xFFFFFFFF, 0.08f * a));
        }
        // face, with the hat layer, flashing red when hit
        float hurt = Math.min(1f, t.hurtTime / 10f);
        int gb = Math.round(255 * (1f - hurt * 0.55f));
        PlayerFaceExtractor.extractRenderState(Draw.g(), t.getSkin(), 6, 6, 32, Math.round(255 * a) << 24 | 0xFF0000 | gb << 8 | gb);

        float x = 44, right = W - 8;
        String hpText = String.format("%.1f", hp);
        float hw = Draw.uiWidth(hpText, 9, true);
        int hc = r > 0.6f ? 0xFF3DDC84 : r > 0.3f ? 0xFFFFC83D : 0xFFFF5A5A;
        Draw.uiB(Draw.fit(t.getName().getString(), 9, right - x - hw - 6), x, 6, 9, Draw.alpha(0xFFFFFFFF, a));
        Draw.uiB(hpText, right - hw, 6, 9, Draw.alpha(hc, a));

        float bw = right - x, by = 21;
        Draw.roundRect(x, by, bw, 6, 3, Draw.alpha(0xFFFFFFFF, 0.14f * a));
        if (trail > bar + 0.005f) Draw.roundRect(x, by, Math.max(6, bw * trail), 6, 3, Draw.alpha(0xFFFFFFFF, 0.55f * a));
        int main = r > 0.6f ? accent.get() : hc;
        if (bar > 0.01f) Draw.roundRect(x, by, Math.max(6, bw * bar), 6, 3, Draw.alpha(main, a));
        if (t.getAbsorptionAmount() > 0) {
            float ab = Math.min(1f, t.getAbsorptionAmount() / Math.max(1f, t.getMaxHealth()));
            Draw.rect(x, by + 4, Math.max(4, bw * ab), 2, Draw.alpha(0xFFFFC83D, a));
        }

        if (distance.on()) Draw.ui(String.format("%.1f m", mc.player.distanceTo(t)), x, 31, 7, Draw.alpha(0xFF9AA3B5, a));
        if (compare.on()) {
            float mine = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            boolean win = !editing && mine > hp + 0.5f, lose = !editing && mine < hp - 0.5f;
            // how far ahead or behind you are, in health points (2 = one heart)
            String s = win ? String.format("WINNING +%.1f", mine - hp) : lose ? String.format("LOSING -%.1f", hp - mine) : "EVEN";
            Draw.uiB(s, right - Draw.uiWidth(s, 7, true), 31, 7, Draw.alpha(win ? 0xFF3DDC84 : lose ? 0xFFFF5A5A : 0xFF9AA3B5, a));
        }
    }
}
