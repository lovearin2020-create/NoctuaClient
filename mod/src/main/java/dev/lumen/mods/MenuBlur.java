package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;

public class MenuBlur extends Module {
    public static MenuBlur INSTANCE;

    public MenuBlur() {
        super("Menu Blur", "Blurs the game behind open menus.", Category.VISUAL);
        INSTANCE = this;
        hidden = true;
    }
}
