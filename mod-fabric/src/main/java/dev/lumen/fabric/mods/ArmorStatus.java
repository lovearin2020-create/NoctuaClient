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
    private boolean noctua() { return lumen() && vertical(); }
    protected boolean ownPanel() { return noctua(); }
    public float width() { return noctua() ? (show.is("Hidden") ? 24 : 100) : vertical() ? (show.is("Hidden") ? 16 : 46) : rows * (show.is("Hidden") ? 18 : 44); }
    public float height() { return noctua() ? rows * 19 + 3 : vertical() ? rows * 17 : 16; }

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
        if (noctua()) {
            if (list.isEmpty()) return;
            float w = width();
            if (panelOn()) panel(0, 0, w, height());
            float ly = 3;
            String[] names = {"Helmet", "Chestplate", "Leggings", "Boots"};
            for (ItemStack s : list) {
                Draw.item(s, 4, (int) ly, false);
                if (!show.is("Hidden")) {
                    String n = "Held";
                    for (int k = 0; k < SLOTS.length; k++) if (mc.player.getItemBySlot(SLOTS[k]) == s) n = names[k];
                    if (editing && mc.player.getMainHandItem() != s && n.equals("Held")) n = names[Math.min(3, list.indexOf(s))];
                    Draw.ui(n, 25, cap(ly + 3, 7), 7, color.get());
                    if (s.isDamageableItem()) {
                        int left = s.getMaxDamage() - s.getDamageValue();
                        float f = left / (float) Math.max(1, s.getMaxDamage());
                        String t = show.is("Percent") ? Math.round(f * 100) + "%" : String.valueOf(left);
                        Draw.ui(t, w - 7 - Draw.uiWidth(t, 7), cap(ly + 3, 7), 7, 0xFF8891A5);
                        Draw.roundRect(25, ly + 10.5f, w - 32, 2.5f, 1.2f, 0x30FFFFFF);
                        Draw.roundRect(25, ly + 10.5f, Math.max(2.5f, (w - 32) * f), 2.5f, 1.2f, f > 0.5f ? 0xFF3DD68C : f > 0.2f ? 0xFFFFB454 : 0xFFE5484D);
                    } else if (s.getCount() > 1) {
                        String t = "x" + s.getCount();
                        Draw.ui(t, w - 7 - Draw.uiWidth(t, 7), mid(ly, 16, 7), 7, 0xFF8891A5);
                    }
                }
                ly += 19;
            }
            return;
        }
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
