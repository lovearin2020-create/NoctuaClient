package dev.lumen.fabric.mixin;

import dev.lumen.fabric.Hooks;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Lets the Game Font mod draw the game's default text in another font. */
@Mixin(Font.class)
public class FontMixin {
    @ModifyVariable(method = "getGlyphSource", at = @At("HEAD"), argsOnly = true)
    private FontDescription lumen$gameFont(FontDescription asked) { return Hooks.gameFont(asked); }
}
