package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemCounter extends HudModule {
    private final Setting.Bool totems = bool("Totems", true), gapples = bool("Golden Apples", true), pearls = bool("Ender Pearls", true),
            crystals = bool("End Crystals", true), obsidian = bool("Obsidian", false), arrows = bool("Arrows", true), xp = bool("XP Bottles", false);
    private final Setting.Bool hideZero = bool("Hide Zero Counts", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private int shown = 1;

    public ItemCounter() { super("Item Counter", "Counts key PvP items in your inventory.", CENTER, 202, END, 4); optionalBackground(); }

    public float width() { return shown * 20; }
    public float height() { return 26; }

    private int count(Item item) {
        var inv = mc.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); if (!s.isEmpty() && s.getItem() == item) n += s.getCount(); }
        return n;
    }

    public void draw(boolean editing) {
        Setting.Bool[] on = {totems, gapples, pearls, crystals, obsidian, arrows, xp};
        Item[] items = {Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENDER_PEARL, Items.END_CRYSTAL, Items.OBSIDIAN, Items.ARROW, Items.EXPERIENCE_BOTTLE};
        int i = 0;
        for (int k = 0; k < items.length; k++) {
            if (!on[k].on()) continue;
            int n = count(items[k]);
            if (n == 0 && hideZero.on() && !editing) continue;
            Draw.item(new ItemStack(items[k]), i * 20 + 2, 0, false);
            String s = String.valueOf(n);
            if (lumen()) Draw.uiCenterB(s, i * 20 + 10, cap(18, 7), 7, n == 0 ? 0xFFFF5C5C : color.get());
            else Draw.text(s, i * 20 + 10 - mc.font.width(s) / 2f, 17, n == 0 ? 0xFFFF5C5C : color.get(), true);
            i++;
        }
        shown = Math.max(1, i);
    }
}
