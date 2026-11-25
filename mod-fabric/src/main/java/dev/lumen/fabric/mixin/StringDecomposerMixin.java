package dev.lumen.fabric.mixin;

import dev.lumen.fabric.Hooks;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public class StringDecomposerMixin {
    private static final String ITERATE = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z";

    @ModifyVariable(method = ITERATE, at = @At("HEAD"), argsOnly = true)
    private static String lumen$nick(String text, String sameText, int offset, Style current, Style reset, FormattedCharSink sink) {
        return offset == 0 ? Hooks.nick(text) : text;
    }

    /** The Lumen mark in front of a name is one reserved character; it is handed on in the font that draws it as a picture. */
    @ModifyVariable(method = ITERATE, at = @At("HEAD"), argsOnly = true)
    private static FormattedCharSink lumen$mark(FormattedCharSink sink, String text, int offset, Style current, Style reset, FormattedCharSink sameSink) {
        return Hooks.markSink(sink);
    }
}
