package dev.lumen.util;

import net.minecraft.util.ResourceLocation;

/** Implemented on the game's font renderer (by mixin) so the Game Font mod can swap its glyph sheet. */
public interface FontAccess {
    ResourceLocation lumen$sheet();
    void lumen$sheet(ResourceLocation sheet);
    int[] lumen$widths();
}
