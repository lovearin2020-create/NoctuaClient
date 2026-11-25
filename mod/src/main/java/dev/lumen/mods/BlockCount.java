package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** While you hold a block, shows how many of that block you have left. */
public class BlockCount extends HudModule {
    private final Setting.Num low = num("Low Warning Below", 16, 0, 64, 1);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private final Setting.Col lowColor = col("Low Color", 0xFFFF5C5C);

    public BlockCount() { super("Block Count", "Blocks left of the type you are holding.", CENTER, 8, CENTER, 8); optionalBackground(); }
    protected boolean showing(boolean editing) { return editing || (mc.thePlayer != null && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock); }

    public float width() { return 18; }
    public float height() { return 9; }

    public void draw(boolean editing) {
        int n = 64;
        if (!editing) {
            if (mc.thePlayer == null) return;
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBlock)) return;
            n = 0;
            for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
                if (s != null && s.getItem() == held.getItem() && s.getMetadata() == held.getMetadata()) n += s.stackSize;
            }
        }
        if (lumen()) { Fnt.centerB(String.valueOf(n), 9, mid(0, 9, 9), 9, n < low.i() ? lowColor.get() : color.get()); return; }
        R.mcText(String.valueOf(n), 0, 0, n < low.i() ? lowColor.get() : color.get(), true);
    }
}
