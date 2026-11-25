package dev.lumen.fabric.cosmetics;

/** What a player wears, copied onto their render state when it is filled in, so the layers know it when they draw later. */
public interface OutfitState {
    void lumen$outfit(String wings, String hat, String shield, boolean airborne);
    String lumen$wings();
    String lumen$hat();
    String lumen$shield();
    boolean lumen$airborne();
}
