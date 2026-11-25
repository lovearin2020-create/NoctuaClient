package dev.lumen.online;

/** What one player is wearing, as last heard from the backend. */
public final class Outfit {
    public static final String[] SLOTS = {"cape", "wings", "hat", "shield"};
    public String cape, wings, hat, shield;
    /** Lumen+ name colour: one Minecraft colour code character, or 0 for none. */
    public char color;
    long fetched;

    public String get(String slot) {
        if (slot.equals("cape")) return cape;
        if (slot.equals("wings")) return wings;
        if (slot.equals("hat")) return hat;
        return slot.equals("shield") ? shield : null;
    }

    void set(String slot, String id) {
        if (slot.equals("cape")) cape = id;
        else if (slot.equals("wings")) wings = id;
        else if (slot.equals("hat")) hat = id;
        else if (slot.equals("shield")) shield = id;
    }
}
