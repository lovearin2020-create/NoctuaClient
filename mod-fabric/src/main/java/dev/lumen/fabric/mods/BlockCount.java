package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** While you hold a block, shows how many of that block you have left. */
public class BlockCount extends HudModule {
    private final Setting.Num low = num("Low Warning Below", 16, 0, 64, 1);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private final Setting.Col lowColor = col("Low Color", 0xFFFF5C5C);

    public BlockCount() { super("Block Count", "Blocks left of the type you are holding.", CENTER, 8, CENTER, 8); optionalBackground(); }
    protected boolean showing(boolean editing) { return editing || (mc.player != null && mc.player.getMainHandItem().getItem() instanceof BlockItem); }

    public float width() { return 18; }
    public float height() { return 9; }

    public void draw(boolean editing) {
        int n = 64;
        if (!editing) {
            if (mc.player == null) return;
            ItemStack held = mc.player.getMainHandItem();
            if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) return;
            n = 0;
            var inv = mc.player.getInventory();
            for (int k = 0; k < inv.getContainerSize(); k++) {
                ItemStack s = inv.getItem(k);
                if (!s.isEmpty() && s.getItem() == held.getItem()) n += s.getCount();
            }
        }
        Draw.text(String.valueOf(n), 0, 0, n < low.i() ? lowColor.get() : color.get(), true);
    }
}
