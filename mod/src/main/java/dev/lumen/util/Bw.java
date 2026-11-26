package dev.lumen.util;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/** What the Bed Wars mods read off the server: the sidebar and team colours. */
public final class Bw {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final String START = "Protect your bed and destroy the enemy beds";

    /** Team colour codes in Hypixel's order, and the colour each one is drawn in. */
    public static final String CODES = "c9aebfd8";
    private static final int[] RGB = {0xD63A3A, 0x3A5BD6, 0x45C545, 0xEAD53A, 0x35C3D6, 0xF2F2F2, 0xF07FB4, 0x6A6A6A};
    public static final String[] NAMES = {"Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"};

    public static int rgb(char code) { int i = CODES.indexOf(code); return i < 0 ? 0 : RGB[i]; }

    /** The sidebar's lines without colour codes or the filler characters Hypixel pads them with. */
    public static List<String> sidebar() {
        List<String> out = new ArrayList<String>();
        if (mc.theWorld == null) return out;
        Scoreboard sb = mc.theWorld.getScoreboard();
        ScoreObjective o = sb.getObjectiveInDisplaySlot(1);
        if (o == null) return out;
        out.add(clean(o.getDisplayName()));
        for (Score s : sb.getSortedScores(o)) {
            ScorePlayerTeam t = sb.getPlayersTeam(s.getPlayerName());
            out.add(clean(ScorePlayerTeam.formatPlayerName(t, s.getPlayerName())));
        }
        return out;
    }

    private static String clean(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : Util.strip(s).toCharArray()) if (c >= 32 && c < 127) b.append(c);
        return b.toString().trim();
    }

    /** The map named on the waiting-lobby sidebar, or null when it is not shown. */
    public static String mapOnSidebar() {
        for (String l : sidebar()) if (l.startsWith("Map: ") && l.length() > 5) return l.substring(5).trim();
        return null;
    }

    /** The colour code of a team, taken from the last colour in its name prefix. */
    public static char colour(Team team) {
        if (!(team instanceof ScorePlayerTeam)) return 0;
        String p = ((ScorePlayerTeam) team).getColorPrefix();
        for (int i = p.length() - 2; i >= 0; i--) {
            if (p.charAt(i) == '§' && CODES.indexOf(Character.toLowerCase(p.charAt(i + 1))) >= 0) return Character.toLowerCase(p.charAt(i + 1));
        }
        return 0;
    }

    /** True for chat lines typed by players, which must never be read as game messages. */
    public static boolean playerChat(String msg) { return msg.indexOf(": ") > 0 && !msg.startsWith(" "); }

    private Bw() {}
}
