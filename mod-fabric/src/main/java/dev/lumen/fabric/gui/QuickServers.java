package dev.lumen.fabric.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumen.fabric.core.Config;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** The servers on the title screen's Quick Play list: yours to name, change and remove. Kept in quickplay.json. */
public final class QuickServers {
    public static final int MAX = 6;
    public static final class Entry { public String name, address; Entry(String n, String a) { name = n; address = a; } }

    private static List<Entry> list;
    private QuickServers() {}

    private static File file() { return new File(Config.dir(), "quickplay.json"); }

    public static List<Entry> all() {
        if (list == null) {
            list = new ArrayList<Entry>();
            try {
                File f = file();
                if (f.isFile()) {
                    for (JsonElement e : new JsonParser().parse(new String(Files.readAllBytes(f.toPath()), Charset.forName("UTF-8"))).getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();
                        if (list.size() < MAX) list.add(new Entry(o.get("name").getAsString(), o.get("address").getAsString()));
                    }
                } else list.add(new Entry("Hypixel", "mc.hypixel.net"));
            } catch (Throwable t) { list.clear(); list.add(new Entry("Hypixel", "mc.hypixel.net")); }
        }
        return list;
    }

    public static void save() {
        try {
            JsonArray a = new JsonArray();
            for (Entry e : all()) { JsonObject o = new JsonObject(); o.addProperty("name", e.name); o.addProperty("address", e.address); a.add(o); }
            File f = file();
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), a.toString().getBytes(Charset.forName("UTF-8")));
        } catch (Throwable t) { System.err.println("[Noctua] Could not save the Quick Play list: " + t); }
    }

    /** Adds or changes an entry (index -1 adds). A blank address is not kept; a blank name becomes the address. */
    public static void put(int index, String name, String address) {
        address = address.trim(); name = name.trim();
        if (address.isEmpty()) return;
        if (name.isEmpty()) name = address;
        if (index >= 0 && index < all().size()) { all().get(index).name = name; all().get(index).address = address; }
        else if (all().size() < MAX) all().add(new Entry(name, address));
        save();
    }

    public static void remove(int index) { if (index >= 0 && index < all().size()) { all().remove(index); save(); } }
}
