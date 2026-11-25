package dev.lumen.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lumen.mods.MenuBlur;
import dev.lumen.util.Fnt;

import java.util.Arrays;
import java.util.List;

/** Settings of the client itself (the Settings tab), as opposed to settings of one mod. */
public final class Client {
    public static final Setting.Mode theme = new Setting.Mode("Theme", "Dark", "Dark", "Glass");
    public static final Setting.Mode corners = new Setting.Mode("Corners", "Rounded", "Rounded", "Square");
    public static final Setting.Num menuSize = new Setting.Num("Menu Size (%)", 100, 60, 100, 5);
    public static final Setting.Bool blur = new Setting.Bool("Menu Blur", false);
    public static final Setting.Num blurStrength = new Setting.Num("Blur Strength", 5, 1, 10, 1);
    public static final Setting.Mode font = new Setting.Mode("Font", "Smooth", "Smooth", "Minecraft");
    public static final Setting.Bool nameIcon = new Setting.Bool("Noctua Icon Next To Names", true);
    public static final Setting.Key keyMenu = new Setting.Key("Key: Open This Menu", "RSHIFT");
    public static final Setting.Key keyZoom = new Setting.Key("Key: Zoom (hold)", "C");
    public static final Setting.Key keyQuickPlay = new Setting.Key("Key: Quick Play", "G");
    public static final Setting.Key keyKorean = new Setting.Key("Key: Korean / English", "RALT");
    /** Shown in the Settings tab, in this order. Blur strength is fixed by the shader on 1.8.9, so it is saved but not shown. */
    public static final List<Setting> shown = Arrays.<Setting>asList(theme, corners, menuSize, blur, font, nameIcon, keyMenu, keyZoom, keyQuickPlay, keyKorean);
    private static final Setting[] saved = {theme, corners, menuSize, blurStrength, nameIcon, keyMenu, keyZoom, keyQuickPlay, keyKorean};

    private Client() {}

    /** Copies the on/off state of the hidden Menu Blur mod into its switch. */
    public static void sync() {
        if (MenuBlur.INSTANCE != null) blur.value = MenuBlur.INSTANCE.isEnabled();
        font.index = Fnt.minecraft ? 1 : 0;
    }

    /** Pushes the switches into the things they control. */
    public static void apply() {
        if (MenuBlur.INSTANCE != null && MenuBlur.INSTANCE.isEnabled() != blur.value) MenuBlur.INSTANCE.setEnabled(blur.value);
        Fnt.minecraft = font.is("Minecraft");
        dev.lumen.gui.Theme.apply();
    }

    public static void save(JsonObject client) {
        client.addProperty("font", Fnt.minecraft ? "minecraft" : "smooth");
        for (Setting s : saved) client.add(key(s), s.save());
    }

    public static void load(JsonObject client) {
        try { if (client.has("font")) Fnt.minecraft = "minecraft".equals(client.get("font").getAsString()); } catch (Throwable ignored) {}
        for (Setting s : saved) {
            JsonElement e = client.get(key(s));
            if (e != null) try { s.load(e); } catch (Throwable ignored) {}
        }
        dev.lumen.gui.Theme.apply();
    }

    private static String key(Setting s) {
        return s == nameIcon ? "nameIcon" : s == theme ? "theme" : s == corners ? "corners" : s == menuSize ? "menuSize" : s == blurStrength ? "blurStrength"
                : s == keyMenu ? "keyMenu" : s == keyZoom ? "keyZoom" : s == keyQuickPlay ? "keyQuickPlay" : "keyKorean";
    }
}
