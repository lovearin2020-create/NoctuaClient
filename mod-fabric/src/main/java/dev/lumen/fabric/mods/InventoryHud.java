package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.item.ItemStack;

public class InventoryHud extends HudModule {
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool hideEmpty = bool("Hide When Empty", false);

    public InventoryHud() { super("Inventory HUD", "Shows your inventory on screen.", END, 4, END, 4); }

    public float width() { return 9 * 18 + 6; }
    public float height() { return 3 * 18 + 6; }

    public void draw(boolean editing) {
        var inv = mc.player.getInventory();
        if (hideEmpty.on() && !editing) {
            boolean any = false;
            for (int i = 9; i < 36 && !any; i++) any = !inv.getItem(i).isEmpty();
            if (!any) return;
        }
        if (background.on()) Draw.roundRect(0, 0, width(), height(), 4, 0x70000000);
        for (int i = 9; i < 36; i++) {
            int col = (i - 9) % 9, row = (i - 9) / 9;
            int x = 4 + col * 18, y = 4 + row * 18;
            if (background.on()) Draw.rect(x - 1, y - 1, 18, 18, 0x18FFFFFF);
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) Draw.item(s, x, y, true);
        }
    }
}
