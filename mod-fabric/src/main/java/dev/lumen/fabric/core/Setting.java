package dev.lumen.fabric.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.awt.Color;

/** A single configurable value belonging to a module. */
public abstract class Setting {
    public final String name;

    protected Setting(String name) { this.name = name; }

    public abstract JsonElement save();
    public abstract void load(JsonElement e);

    public static class Bool extends Setting {
        public boolean value;
        public Bool(String name, boolean def) { super(name); this.value = def; }
        public boolean on() { return value; }
        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { value = e.getAsBoolean(); }
    }

    public static class Num extends Setting {
        public double value;
        public final double min, max, step;
        public Num(String name, double def, double min, double max, double step) {
            super(name); this.value = def; this.min = min; this.max = max; this.step = step;
        }
        public float f() { return (float) value; }
        public int i() { return (int) Math.round(value); }
        public void set(double v) {
            v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }
        public String display() {
            return step >= 1 ? String.valueOf(i()) : String.format("%.2f", value).replaceAll("0$", "");
        }
        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { set(e.getAsDouble()); }
    }

    public static class Mode extends Setting {
        public final String[] options;
        public int index;
        public Mode(String name, String def, String... options) {
            super(name); this.options = options;
            for (int i = 0; i < options.length; i++) if (options[i].equals(def)) index = i;
        }
        public String get() { return options[index]; }
        public boolean is(String s) { return options[index].equals(s); }
        public void cycle(int dir) { index = (index + dir + options.length) % options.length; }
        public JsonElement save() { return new JsonPrimitive(get()); }
        public void load(JsonElement e) {
            String s = e.getAsString();
            for (int i = 0; i < options.length; i++) if (options[i].equals(s)) index = i;
        }
    }

    public static class Text extends Setting {
        public String value;
        public final int max;
        public Text(String name, String def, int max) { super(name); this.value = def; this.max = max; }
        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { String s = e.getAsString(); value = s.length() > max ? s.substring(0, max) : s; }
    }

    /** A key, stored by a name that is the same in every game version (see Keys). */
    public static class Key extends Setting {
        public String key;
        private int code = Integer.MIN_VALUE;
        public Key(String name, String def) { super(name); this.key = def; }
        public int code() { if (code == Integer.MIN_VALUE) code = dev.lumen.fabric.util.Keys.code(key); return code; }
        public void set(String k) { key = k == null ? "NONE" : k; code = Integer.MIN_VALUE; }
        public boolean is(int pressed) { return pressed > 0 && pressed == code(); }
        public JsonElement save() { return new JsonPrimitive(key); }
        public void load(JsonElement e) { set(e.getAsString()); }
    }

    public static class Col extends Setting {
        public static final int[] PALETTE = {
                0xFFFFFFFF, 0xFF9AA3B5, 0xFF000000, 0xFFFF5C5C, 0xFFFF9F43, 0xFFFFD84D,
                0xFF4ADE80, 0xFF38D9F5, 0xFF4C8DFF, 0xFFA06BFF, 0xFFFF6BCB
        };
        public int color;
        public boolean chroma;
        public Col(String name, int def) { super(name); this.color = def; }
        public int get() {
            if (chroma) return 0xFF000000 | Color.HSBtoRGB((System.currentTimeMillis() % 4000L) / 4000f, 0.75f, 1f);
            return color;
        }
        public JsonElement save() { return new JsonPrimitive(chroma ? "chroma" : Integer.toHexString(color)); }
        public void load(JsonElement e) {
            String s = e.getAsString();
            if (s.equals("chroma")) chroma = true;
            else { chroma = false; color = (int) Long.parseLong(s, 16); }
        }
    }
}
