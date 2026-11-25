package dev.lumen.mixin;

import dev.lumen.Hooks;
import dev.lumen.mods.GameFont;
import dev.lumen.online.Online;
import dev.lumen.util.FontAccess;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer implements FontAccess {
    @Shadow protected int[] charWidth;
    @Shadow @Final @Mutable protected ResourceLocation locationFontTexture;
    @Shadow protected float posX;
    @Shadow protected float posY;
    @Shadow private float red;
    @Shadow private float green;
    @Shadow private float blue;
    @Shadow private float alpha;

    @ModifyVariable(method = "renderString", at = @At("HEAD"), argsOnly = true)
    private String lumen$nickDraw(String s) { return Hooks.nick(s); }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), argsOnly = true)
    private String lumen$nickWidth(String s) { return Hooks.nick(s); }

    /** The Lumen mark in front of a Lumen user's name is one reserved character, drawn here as a picture. */
    @Inject(method = "renderChar", at = @At("HEAD"), cancellable = true)
    private void lumen$mark(char ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (ch != Online.MARK) return;
        Hooks.nameMark(posX, posY, red, green, blue, alpha);
        cir.setReturnValue(Online.MARK_WIDTH);
    }

    @Inject(method = "getCharWidth", at = @At("HEAD"), cancellable = true)
    private void lumen$markWidth(char ch, CallbackInfoReturnable<Integer> cir) {
        if (ch == Online.MARK) cir.setReturnValue((int) Online.MARK_WIDTH);
    }

    /** The game re-reads its own glyph sheet here; ours is not a file it can read, so it is taken off first. */
    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void lumen$beforeReload(IResourceManager manager, CallbackInfo ci) { GameFont.beforeReload((FontRenderer) (Object) this); }

    public ResourceLocation lumen$sheet() { return locationFontTexture; }
    public void lumen$sheet(ResourceLocation sheet) { locationFontTexture = sheet; }
    public int[] lumen$widths() { return charWidth; }
}
