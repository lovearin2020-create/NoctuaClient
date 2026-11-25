package dev.lumen.fabric.mods;

import dev.lumen.fabric.core.*;
import dev.lumen.fabric.core.Module;
import dev.lumen.fabric.util.*;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Comparator;
import java.util.List;

public class ScoreboardMod extends Module {
    private static final Comparator<PlayerScoreEntry> ORDER = Comparator.comparing(PlayerScoreEntry::value).reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);
    public final Setting.Bool hide = bool("Hide Scoreboard", false);
    private final Setting.Bool numbers = bool("Hide Red Numbers", true);
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool shadow = bool("Text Shadow", false);
    private final Setting.Num scale = num("Scale", 1.0, 0.5, 1.5, 0.05);

    public ScoreboardMod() { super("Scoreboard", "Restyle, resize or hide the sidebar.", Category.HUD); }

    /** Replacement for the vanilla sidebar. Same layout, anchored to the right edge and vertically centred. */
    public void render(GuiGraphicsExtractor g, Objective objective) {
        if (hide.on()) return;
        var font = mc.font;
        Scoreboard sb = objective.getScoreboard();
        NumberFormat format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<PlayerScoreEntry> list = sb.listPlayerScores(objective).stream().filter(e -> !e.isHidden()).sorted(ORDER).limit(15).toList();
        boolean nums = !numbers.on();
        Component title = objective.getDisplayName();
        int n = list.size(), titleW = font.width(title), w = titleW, spacer = font.width(": ");
        Component[] names = new Component[n], scores = new Component[n];
        for (int i = 0; i < n; i++) {
            PlayerScoreEntry e = list.get(i);
            PlayerTeam team = sb.getPlayersTeam(e.owner());
            names[i] = PlayerTeam.formatNameForTeam(team, e.ownerName());
            scores[i] = e.formatValue(format);
            int sw = nums ? font.width(scores[i]) : 0;
            w = Math.max(w, font.width(names[i]) + (sw > 0 ? spacer + sw : 0));
        }
        float k = scale.f();
        g.pose().pushMatrix();
        g.pose().translate(g.guiWidth(), g.guiHeight() / 2f);
        g.pose().scale(k, k);
        int bottom = n * 9 / 3, left = -w - 3, right = -1, headerY = bottom - n * 9;
        if (background.on()) {
            g.fill(left - 2, headerY - 10, right, headerY - 1, 0x66000000);
            g.fill(left - 2, headerY - 1, right, bottom, 0x4D000000);
        }
        g.text(font, title, left + w / 2 - titleW / 2, headerY - 9, -1, shadow.on());
        for (int i = 0; i < n; i++) {
            int y = bottom - (n - i) * 9;
            g.text(font, names[i], left, y, -1, shadow.on());
            if (nums) g.text(font, scores[i], right - font.width(scores[i]), y, -1, shadow.on());
        }
        g.pose().popMatrix();
    }
}
