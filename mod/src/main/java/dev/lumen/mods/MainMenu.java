package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.gui.LumenMainMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MainMenu extends Module {
    public MainMenu() { super("Main Menu", "Animated Noctua title screen.", Category.VISUAL); }

    /** Set by the "Accounts" button: the next title screen is the vanilla one, where other mods (Essential) put their buttons. */
    public static boolean vanillaOnce;

    @SubscribeEvent
    public void onOpen(GuiOpenEvent e) {
        if (e.gui == null || e.gui.getClass() != GuiMainMenu.class) return;
        if (vanillaOnce) { vanillaOnce = false; return; }
        e.gui = new LumenMainMenu();
    }
}
