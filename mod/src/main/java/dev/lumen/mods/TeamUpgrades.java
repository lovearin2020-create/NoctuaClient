package dev.lumen.mods;

import dev.lumen.core.Category;
import dev.lumen.core.HudModule;
import dev.lumen.core.Setting;
import dev.lumen.util.Bw;
import dev.lumen.util.Fnt;
import dev.lumen.util.R;
import dev.lumen.util.Util;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bed Wars: the team upgrades and traps your team has bought, read from the purchase messages
 * the server sends to the whole team, so you do not have to open the upgrade shop to check.
 */
public class TeamUpgrades extends HudModule {
    private final Setting.Bool background = bool("Background", true);
    private final Setting.Bool showTraps = bool("Show Traps", true);
    private final Setting.Col color = col("Text Color", 0xFFFFFFFF);

    private static final Pattern BOUGHT = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w{1,16}) purchased (.+?)!?\\s*$");
    private static final Pattern TIER = Pattern.compile("^(.+?) (I|II|III|IV|V)$");
    private static final String[] FORGES = {"Iron", "Golden", "Emerald", "Molten"};

    private final Map<String, String> upgrades = new LinkedHashMap<String, String>();
    private final List<String> traps = new ArrayList<String>();

    public TeamUpgrades() { super("Team Upgrades", "Bed Wars: the upgrades and traps your team has bought.", Category.HYPIXEL, START, 4, CENTER, 40); }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load e) { upgrades.clear(); traps.clear(); }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Util.onHypixel()) return;
        String msg = Util.strip(e.message.getUnformattedText()).trim();
        if (Bw.playerChat(msg)) return;
        if (msg.contains(Bw.START)) { upgrades.clear(); traps.clear(); return; }
        String low = msg.toLowerCase();
        if (low.contains("trap") && (low.contains("set off") || low.contains("triggered"))) { if (!traps.isEmpty()) traps.remove(0); return; }
        Matcher m = BOUGHT.matcher(msg);
        if (!m.matches() || m.group(1).equals("You")) return;
        bought(m.group(2).trim());
    }

    /** Files one purchase under the name it is known by in game. */
    void bought(String item) {
        String tier = "";
        Matcher t = TIER.matcher(item);
        if (t.matches()) { item = t.group(1); tier = t.group(2); }
        String low = item.toLowerCase();
        if (low.contains("trap")) {
            String name = low.startsWith("it's a trap") ? "It's a Trap" : item.replaceAll("(?i)\\s*trap$", "");
            if (traps.size() < 3) traps.add(name);
            return;
        }
        if (low.endsWith(" forge")) {
            String kind = item.substring(0, item.length() - 6);
            upgrades.put("Forge", kind);
            return;
        }
        if (low.equals("sharpened swords")) upgrades.put("Sharpness", "I");
        else if (low.equals("reinforced armor")) upgrades.put("Protection", tier.isEmpty() ? "I" : tier);
        else if (low.equals("maniac miner")) upgrades.put("Haste", tier.isEmpty() ? "I" : tier);
        else upgrades.put(item, tier.isEmpty() ? "✓" : tier);
    }

    private List<String[]> rows(boolean editing) {
        List<String[]> out = new ArrayList<String[]>();
        if (upgrades.isEmpty() && traps.isEmpty()) {
            if (!editing) return out;
            out.add(new String[]{"Sharpness", "I"}); out.add(new String[]{"Protection", "II"}); out.add(new String[]{"Forge", FORGES[1]});
            if (showTraps.on()) out.add(new String[]{"Trap 1", "Alarm"});
            return out;
        }
        for (Map.Entry<String, String> u : upgrades.entrySet()) out.add(new String[]{u.getKey(), u.getValue().equals("✓") ? "Yes" : u.getValue()});
        if (showTraps.on()) for (int i = 0; i < traps.size(); i++) out.add(new String[]{"Trap " + (i + 1), traps.get(i)});
        return out;
    }

    private int shown = 3;
    public float width() { return lumen() ? 112 : 96; }
    public float height() { return lumen() ? 18 + shown * 10 + 3 : 4 + (shown + 1) * 10; }
    protected boolean ownPanel() { return true; }
    protected boolean showing(boolean editing) { return editing || !upgrades.isEmpty() || !traps.isEmpty(); }

    public void draw(boolean editing) {
        List<String[]> rows = rows(editing);
        if (rows.isEmpty()) return;
        shown = rows.size();
        float w = width();
        if (lumen()) {
            if (background.on()) panel(0, 0, w, height());
            int dim = 0xFF8891A5;
            Fnt.draw("TEAM UPGRADES", 7, cap(6, 6), 6, Fnt.REGULAR, 0.5f, dim, dim);
            R.rect(6, 14, w - 12, 1, R.alpha(R.ACCENT, 0.75f));
            float y = 18;
            for (String[] r : rows) {
                boolean trap = r[0].startsWith("Trap ");
                Fnt.draw(r[0], 7, y, 7, dim);
                Fnt.drawB(r[1], w - 7 - Fnt.widthB(r[1], 7), y, 7, trap ? 0xFFFFB454 : color.get());
                y += 10;
            }
            return;
        }
        if (background.on()) R.roundRect(0, 0, w, height(), 3, 0x70000000);
        R.mcText("§lTeam Upgrades", 4, 4, color.get(), true);
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            R.mcText(r[0], 4, 14 + i * 10, 0xFFAAAAAA, true);
            R.mcText(r[1], w - 4 - mc.fontRendererObj.getStringWidth(r[1]), 14 + i * 10, r[0].startsWith("Trap ") ? 0xFFFFB454 : color.get(), true);
        }
    }
}
