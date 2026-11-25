package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemCounter extends HudModule {
    private final Setting.Mode item = mode("Item", "Blocks", "Blocks", "Held Item", "Arrows", "Ender Pearls", "Golden Apples", "TNT", "Fireballs");
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);

    public ItemCounter() { super("Item Counter", "Counts an item in your inventory.", CENTER, -70, CENTER, 24); }

    public float width() { return 44; }
    public float height() { return 20; }

    private boolean matches(ItemStack s, ItemStack held) {
        Item i = s.getItem();
        if (item.is("Blocks")) return i instanceof ItemBlock && i != Item.getItemFromBlock(Blocks.tnt);
        if (item.is("Held Item")) return held != null && i == held.getItem() && s.getMetadata() == held.getMetadata();
        if (item.is("Arrows")) return i == Items.arrow;
        if (item.is("Ender Pearls")) return i == Items.ender_pearl;
        if (item.is("Golden Apples")) return i == Items.golden_apple;
        if (item.is("TNT")) return i == Item.getItemFromBlock(Blocks.tnt);
        return i == Items.fire_charge;
    }

    public void draw(boolean editing) {
        if (mc.thePlayer == null) return;
        ItemStack held = mc.thePlayer.getHeldItem(), icon = null;
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s == null || !matches(s, held)) continue;
            n += s.stackSize;
            if (icon == null || s == held) icon = s;
        }
        if (icon == null) {
            if (!editing) return;
            icon = new ItemStack(Blocks.wool);
        }
        if (background.on()) box(0, 0, 44, 20);
        ArmorStatus.drawItem(icon, 2, 2);
        if (lumen()) Fnt.drawB(String.valueOf(n), 23, mid(0, 20, 9), 9, color.get());
        else R.mcText(String.valueOf(n), 22, 6, color.get(), true);
    }
}
