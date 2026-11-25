package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Client-side timer that starts when you throw an ender pearl. Set the length to your server's cooldown. */
public class Cooldowns extends HudModule {
    private final Setting.Num pearl = num("Ender Pearl Cooldown (s)", 16, 1, 30, 1);
    private final Setting.Col color = col("Bar Color", 0xFF38D9F5);
    private long until, length;
    private boolean wasDown;

    private final Setting.Bool background = bool("Background", true);
    public Cooldowns() { super("Cooldowns", "Ender pearl cooldown timer.", CENTER, 20, CENTER, 24); }

    public float width() { return 62; }
    public float height() { return 20; }

    public void onTick() {
        boolean down = mc.gui.screen() == null && mc.options.keyUse.isDown();
        ItemStack held = mc.player.getMainHandItem();
        if (down && !wasDown && !held.isEmpty() && held.getItem() == Items.ENDER_PEARL && System.currentTimeMillis() >= until) {
            length = (long) (pearl.value * 1000);
            until = System.currentTimeMillis() + length;
        }
        wasDown = down;
    }

    public void draw(boolean editing) {
        long left = until - System.currentTimeMillis();
        if (left <= 0 && !editing) return;
        float f = left <= 0 ? 0.6f : left / (float) Math.max(1, length);
        if (background.on()) Draw.roundRect(0, 0, 62, 20, 3, 0x70000000);
        Draw.item(new ItemStack(Items.ENDER_PEARL), 2, 2, false);
        Draw.text(String.format("%.1fs", left <= 0 ? pearl.value * 0.6 : left / 1000.0), 22, 3, 0xFFFFFFFF, true);
        Draw.rect(22, 14, 36, 2, 0x50FFFFFF);
        Draw.rect(22, 14, 36 * f, 2, color.get());
    }
}
