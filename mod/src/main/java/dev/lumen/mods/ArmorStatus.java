package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorStatus extends HudModule {
    private final Setting.Bool held = bool("Held Item", true);
    private final Setting.Mode show = mode("Durability", "Remaining", "Remaining", "Percent", "Hidden");
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);
    private int rows = 4;

    public ArmorStatus() { super("Armor Status", "Armor and durability.", END, 4, END, 4); optionalBackground(); }
    protected boolean showing(boolean editing) { return editing || rows > 0; }

    protected boolean ownPanel() { return lumen(); }
    public float width() { return lumen() ? (show.is("Hidden") ? 24 : 100) : show.is("Hidden") ? 18 : 48; }
    public float height() { return lumen() ? Math.max(1, rows) * 19 + 3 : Math.max(1, rows) * 17; }

    public void draw(boolean editing) {
        List<ItemStack> list = new ArrayList<ItemStack>();
        if (mc.thePlayer != null) {
            for (int i = 3; i >= 0; i--) {
                ItemStack s = mc.thePlayer.inventory.armorInventory[i];
                if (s != null) list.add(s);
            }
            if (held.on() && mc.thePlayer.getHeldItem() != null) list.add(mc.thePlayer.getHeldItem());
        }
        if (list.isEmpty() && editing) {
            list.add(new ItemStack(Items.diamond_helmet)); list.add(new ItemStack(Items.diamond_chestplate));
            list.add(new ItemStack(Items.diamond_leggings)); list.add(new ItemStack(Items.diamond_boots));
        }
        rows = list.size();
        if (lumen()) {
            if (list.isEmpty()) return;
            float w = width();
            if (panelOn()) panel(0, 0, w, height());
            float ly = 3; // 3 above the first icon, 3 below the last
            for (ItemStack s : list) {
                drawItem(s, 4, (int) ly);
                if (!show.is("Hidden")) {
                    String n = s.getItem() instanceof net.minecraft.item.ItemArmor ? new String[]{"Helmet", "Chestplate", "Leggings", "Boots"}[((net.minecraft.item.ItemArmor) s.getItem()).armorType] : "Held";
                    dev.lumen.util.Fnt.draw(n, 25, cap(ly + 3, 7), 7, color.get()); // name and bar together sit in the middle of the icon
                    if (s.isItemStackDamageable()) {
                        int left = s.getMaxDamage() - s.getItemDamage();
                        float f = left / (float) Math.max(1, s.getMaxDamage());
                        String t = show.is("Percent") ? Math.round(f * 100) + "%" : String.valueOf(left);
                        dev.lumen.util.Fnt.draw(t, w - 7 - dev.lumen.util.Fnt.width(t, 7), cap(ly + 3, 7), 7, 0xFF8891A5);
                        R.roundRect(25, ly + 10.5f, w - 32, 2.5f, 1.2f, 0x30FFFFFF);
                        R.roundRect(25, ly + 10.5f, Math.max(2.5f, (w - 32) * f), 2.5f, 1.2f, f > 0.5f ? 0xFF3DD68C : f > 0.2f ? 0xFFFFB454 : 0xFFE5484D);
                    } else if (s.stackSize > 1) {
                        String t = "x" + s.stackSize;
                        dev.lumen.util.Fnt.draw(t, w - 7 - dev.lumen.util.Fnt.width(t, 7), mid(ly, 16, 7), 7, 0xFF8891A5);
                    }
                }
                ly += 19;
            }
            return;
        }
        int y = 0;
        for (ItemStack s : list) {
            drawItem(s, 0, y);
            if (!show.is("Hidden")) {
                String t = null;
                if (s.isItemStackDamageable()) {
                    int left = s.getMaxDamage() - s.getItemDamage();
                    t = show.is("Percent") ? (left * 100 / Math.max(1, s.getMaxDamage())) + "%" : String.valueOf(left);
                } else if (s.stackSize > 1) t = "x" + s.stackSize;
                if (t != null) R.mcText(t, 19, y + 4, color.get(), true);
            }
            y += 17;
        }
    }

    static void drawItem(ItemStack s, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(s, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        R.reset();
    }
}
