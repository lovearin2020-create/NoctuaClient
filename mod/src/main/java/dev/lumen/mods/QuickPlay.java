package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.gui.QuickPlayScreen;
import dev.lumen.util.Util;
import net.minecraft.util.ChatComponentText;

/** A menu of Hypixel games. Picking one sends the same /play command you could type yourself. */
public class QuickPlay extends Module {
    public static QuickPlay INSTANCE;

    /** {category, label, command, label, command, ...} */
    public static final String[][] GAMES = {
            {"Bed Wars", "Solo", "/play bedwars_eight_one", "Doubles", "/play bedwars_eight_two", "3v3v3v3", "/play bedwars_four_three", "4v4v4v4", "/play bedwars_four_four", "4v4", "/play bedwars_two_four", "Lobby", "/lobby bedwars"},
            {"SkyWars", "Solo Normal", "/play solo_normal", "Solo Insane", "/play solo_insane", "Doubles Normal", "/play teams_normal", "Doubles Insane", "/play teams_insane", "Lobby", "/lobby skywars"},
            {"Duels", "Classic", "/play duels_classic_duel", "Bridge", "/play duels_bridge_duel", "UHC", "/play duels_uhc_duel", "SkyWars", "/play duels_sw_duel", "Sumo", "/play duels_sumo_duel", "OP", "/play duels_op_duel", "Combo", "/play duels_combo_duel", "NoDebuff", "/play duels_potion_duel", "Bow", "/play duels_bow_duel", "Boxing", "/play duels_boxing_duel", "Lobby", "/lobby duels"},
            {"More", "UHC Solo", "/play uhc_solo", "UHC Teams", "/play uhc_teams", "Murder Mystery", "/play murder_classic", "Build Battle", "/play build_battle_solo_normal", "TNT Run", "/play tnt_tntrun", "SkyBlock", "/play sb", "The Pit", "/play pit", "Main Lobby", "/lobby main"},
    };

    public QuickPlay() { super("Quick Play", "Press the Quick Play key to pick a Hypixel game (key is set in Settings).", Category.HYPIXEL); INSTANCE = this; }

    public void open() {
        if (mc.thePlayer == null) return;
        if (!Util.onHypixel()) { mc.thePlayer.addChatMessage(new ChatComponentText("§7[Lumen] Quick Play only works on Hypixel.")); return; }
        mc.displayGuiScreen(new QuickPlayScreen());
    }
}
