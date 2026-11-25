package dev.lumen.fabric.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumen.fabric.Lumen;
import dev.lumen.fabric.util.Draw;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static File dir() {
        // The Lumen launcher points every version at one shared folder; otherwise the game folder is used.
        String shared = System.getProperty("lumen.config.dir");
        if (shared != null && !shared.trim().isEmpty()) {
            File s = new File(shared.trim());
            if (s.isDirectory() || s.mkdirs()) return s;
        }
        File d = new File(Minecraft.getInstance().gameDirectory, "config/lumen");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static File file() { return new File(dir(), "config.json"); }

    /** Name of the profile the current settings were last loaded from or saved to ("" = none). */
    public static String activeProfile = "";

    private static File profileDir() {
        File d = new File(dir(), "profiles");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static String clean(String name) {
        String s = name.replaceAll("[^A-Za-z0-9 _\\-]", "").trim();
        return s.length() > 24 ? s.substring(0, 24) : s;
    }

    /** Saved profile names, alphabetical. Each is a full copy of the settings in config/lumen/profiles/NAME.json. */
    public static java.util.List<String> profiles() {
        java.util.List<String> out = new java.util.ArrayList<>();
        File[] files = profileDir().listFiles();
        if (files != null) for (File f : files) if (f.isFile() && f.getName().endsWith(".json")) out.add(f.getName().substring(0, f.getName().length() - 5));
        java.util.Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static boolean saveProfile(String name) {
        name = clean(name);
        if (name.isEmpty()) return false;
        activeProfile = name;
        save();
        write(new File(profileDir(), name + ".json"), file());
        return true;
    }

    public static boolean loadProfile(String name) {
        File f = new File(profileDir(), clean(name) + ".json");
        if (!f.exists()) return false;
        read(f);
        activeProfile = clean(name);
        // The profile's entries for mods and settings this version does not have are carried over as well.
        write(file(), f);
        return true;
    }

    public static void deleteProfile(String name) {
        File f = new File(profileDir(), clean(name) + ".json");
        if (f.exists()) f.delete();
        if (activeProfile.equals(clean(name))) { activeProfile = ""; save(); }
    }

    public static void save() { write(file(), file()); }

    /** If a save was interrupted before the swap, the finished temporary file is used. */
    private static void recover(File f) {
        try {
            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            if (!f.exists() && tmp.exists()) tmp.renameTo(f);
        } catch (Throwable ignored) {}
    }

    private static JsonObject parse(File f) {
        if (f != null) recover(f);
        if (f == null || !f.exists()) return null;
        try {
            Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
            try { return JsonParser.parseReader(r).getAsJsonObject(); } finally { r.close(); }
        } catch (Throwable t) {
            return null;
        }
    }

    private static JsonObject child(JsonObject parent, String key) {
        JsonElement e = parent.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    /**
     * Writes the settings on top of what {@code base} already contains. The same file is shared by the
     * 1.8.9, 26.2 and 26.3 versions of Lumen, so mods and settings this version does not know are kept as they are.
     */
    private static void write(File target, File base) {
        try {
            JsonObject root = parse(base);
            if (root == null) root = new JsonObject();
            JsonObject client = child(root, "_client");
            Client.save(client);
            client.addProperty("profile", activeProfile);
            root.add("_client", client);
            for (Module m : Lumen.modules.all) {
                JsonObject o = child(root, m.name);
                o.addProperty("enabled", m.isEnabled());
                if (m instanceof HudModule) {
                    HudModule h = (HudModule) m;
                    o.addProperty("ax", h.ax); o.addProperty("ay", h.ay); o.addProperty("ox", h.ox); o.addProperty("oy", h.oy); o.addProperty("scale", h.scale);
                }
                JsonObject s = child(o, "settings");
                for (Setting st : m.settings) s.add(st.name, st.save());
                o.add("settings", s);
                root.add(m.name, o);
            }
            File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
            Writer w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
            try { GSON.toJson(root, w); } finally { w.close(); }
            // Swap the finished file in with a single rename, so closing the game mid-save can never leave no file at all.
            try {
                java.nio.file.Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Throwable notAtomic) {
                java.nio.file.Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Throwable t) {
            System.err.println("[Noctua] Could not save config: " + t);
        }
    }

    public static void load() {
        read(file());
        try {
            JsonObject root = JsonParser.parseString(new String(java.nio.file.Files.readAllBytes(file().toPath()), "UTF-8")).getAsJsonObject();
            activeProfile = root.getAsJsonObject("_client").get("profile").getAsString();
        } catch (Throwable ignored) {}
    }

    private static void read(File f) {
        recover(f);
        JsonObject root = null;
        if (f.exists()) {
            try {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                try { root = JsonParser.parseReader(r).getAsJsonObject(); } finally { r.close(); }
            } catch (Throwable t) {
                System.err.println("[Noctua] Config unreadable, using defaults: " + t);
            }
        }
        try {
            if (root != null && root.has("_client")) Client.load(root.getAsJsonObject("_client"));
        } catch (Throwable ignored) {}
        for (Module m : Lumen.modules.all) {
            boolean enabled = Lumen.modules.defaultOn.contains(m);
            if (root != null && root.has(m.name)) {
                try {
                    JsonObject o = root.getAsJsonObject(m.name);
                    if (o.has("enabled")) enabled = o.get("enabled").getAsBoolean();
                    if (m instanceof HudModule) {
                        HudModule h = (HudModule) m;
                        if (o.has("ax")) h.ax = Math.max(0, Math.min(2, o.get("ax").getAsInt()));
                        if (o.has("ay")) h.ay = Math.max(0, Math.min(2, o.get("ay").getAsInt()));
                        if (o.has("ox")) h.ox = o.get("ox").getAsFloat();
                        if (o.has("oy")) h.oy = o.get("oy").getAsFloat();
                        if (o.has("scale")) h.scale = Math.max(0.5f, Math.min(2.5f, o.get("scale").getAsFloat()));
                    }
                    if (o.has("settings")) {
                        JsonObject s = o.getAsJsonObject("settings");
                        for (Setting st : m.settings) {
                            JsonElement e = s.get(st.name);
                            if (e != null) try { st.load(e); } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }
            m.setEnabled(enabled);
        }
    }
}
