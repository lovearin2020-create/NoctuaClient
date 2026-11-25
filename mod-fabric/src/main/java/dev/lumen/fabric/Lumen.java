package dev.lumen.fabric;

import dev.lumen.fabric.core.Config;
import dev.lumen.fabric.core.ModuleManager;
import net.fabricmc.api.ClientModInitializer;

public final class Lumen implements ClientModInitializer {
    public static final String VERSION = "2.5.3";
    /** Key code of the menu key (Right Shift). The numbering differs between game versions, so it is taken from the game. */
    public static ModuleManager modules;
    private static boolean loaded;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        System.out.println("[Noctua] " + modules.all.size() + " mods registered. Press the menu key (RSHIFT by default) in game.");
    }

    /** Settings are read on the first client tick, once the game directory and options exist. */
    public static void ensureLoaded() {
        if (loaded || modules == null) return;
        loaded = true;
        Config.load();
        try { dev.lumen.fabric.compat.Compat.windowIcon(); } catch (Throwable ignored) {}
    }
}
