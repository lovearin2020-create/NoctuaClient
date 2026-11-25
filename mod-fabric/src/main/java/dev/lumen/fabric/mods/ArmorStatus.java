package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArmorStatus extends HudModule {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final Setting.Bool hand = bool("Held Item", true);
    private final Setting.Mode show = mode("Durability", "Remaining", "Remaining", "Percent", "Hidden");
    private final Setting.Mode layout = mode("Layout", "Vertical", "Vertical", "Horizontal");
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private int rows = 5;

    public ArmorStatus() { super("Armor Status", "Shows your armor and its durability.", CENTER, 96, END, 2); optionalBackground(); }

    private boolean vertical() { return layout.is("Vertical"); }
    public float width() { return vertical() ? (show.is("Hidden") ? 16 : 46) : rows * (show.is("Hidden") ? 18 : 44); }
    public float height() { return vertical() ? rows * 17 : 16; }

    private String dura(ItemStack s) {
        if (show.is("Hidden") || !s.isDamageableItem()) return s.getCount() > 1 && !show.is("Hidden") ? "x" + s.getCount() : null;
        int left = s.getMaxDamage() - s.getDamageValue();
        return show.is("Percent") ? left * 100 / Math.max(1, s.getMaxDamage()) + "%" : String.valueOf(left);
    }

    public void draw(boolean editing) {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (EquipmentSlot slot : SLOTS) { ItemStack s = mc.player.getItemBySlot(slot); if (!s.isEmpty()) list.add(s); }
        if (hand.on() && !mc.player.getMainHandItem().isEmpty()) list.add(mc.player.getMainHandItem());
        if (list.isEmpty() && editing) {
            list.add(new ItemStack(Items.DIAMOND_HELMET)); list.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            list.add(new ItemStack(Items.DIAMOND_LEGGINGS)); list.add(new ItemStack(Items.DIAMOND_BOOTS)); list.add(new ItemStack(Items.DIAMOND_SWORD));
        }
        rows = Math.max(1, list.size());
        int i = 0;
        for (ItemStack s : list) {
            int x = vertical() ? 0 : i * (show.is("Hidden") ? 18 : 44), y = vertical() ? i * 17 : 0;
            Draw.item(s, x, y, false);
            String d = dura(s);
            if (d != null) Draw.text(d, x + 19, y + 4, color.get(), true);
            i++;
        }
    }
}
