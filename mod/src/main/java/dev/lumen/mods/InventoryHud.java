package dev.lumen.mods;

import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.R;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

public class InventoryHud extends HudModule {
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool slots = bool("Slot Grid", true);
    private final Setting.Num opacity = num("Background Opacity", 45, 0, 100, 5);

    public InventoryHud() { super("Inventory HUD", "Your inventory on screen.", CENTER, -84, START, 30); }

    protected boolean hasLumenStyle() { return false; }
    public float width() { return 9 * 18 + 6; }
    public float height() { return 3 * 18 + 6; }

    public void draw(boolean editing) {
        int a = (int) (opacity.value * 2.55) << 24;
        if (background.on()) R.roundRect(0, 0, width(), height(), 4, a);
        if (slots.on()) {
            for (int i = 0; i < 27; i++) R.roundRect(3 + (i % 9) * 18 + 0.5f, 3 + (i / 9) * 18 + 0.5f, 17, 17, 2, 0x22FFFFFF);
        }
        if (mc.thePlayer == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < 27; i++) {
            ItemStack s = mc.thePlayer.inventory.mainInventory[i + 9];
            if (s == null) continue;
            int x = 4 + (i % 9) * 18, y = 4 + (i / 9) * 18;
            mc.getRenderItem().renderItemAndEffectIntoGUI(s, x, y);
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, s, x, y);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
    }
}
