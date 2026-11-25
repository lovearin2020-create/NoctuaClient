package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PotCounter extends TextHud {
    private final Setting.Mode item = mode("Item", "Splash Potions", "Splash Potions", "Soup", "Golden Apples");
    public PotCounter() { super("Pot Counter", "Healing items left.", CENTER, -28, CENTER, 70); }
    protected String text(boolean editing) {
        Item want = item.is("Splash Potions") ? Items.SPLASH_POTION : item.is("Soup") ? Items.MUSHROOM_STEW : Items.GOLDEN_APPLE;
        int n = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); if (!s.isEmpty() && s.getItem() == want) n += s.getCount(); }
        return n + (item.is("Splash Potions") ? " Pots" : item.is("Soup") ? " Soups" : " Gapples");
    }
}
