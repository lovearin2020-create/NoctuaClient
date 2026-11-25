package dev.lumen.fabric.mixin;

import dev.lumen.fabric.Hooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Shadow private Component header;
    @Shadow private Component footer;
    private Component lumen$header, lumen$footer;
    private boolean lumen$swapped;

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void lumen$ping(GuiGraphicsExtractor graphics, int slotWidth, int x, int y, PlayerInfo info, CallbackInfo ci) {
        if (Hooks.tabPing(graphics, slotWidth, x, y, info)) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void lumen$pre(GuiGraphicsExtractor graphics, int screenWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        lumen$header = header; lumen$footer = footer; lumen$swapped = true;
        if (Hooks.tabHideHeader()) header = null;
        if (Hooks.tabHideFooter()) footer = null;
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void lumen$post(GuiGraphicsExtractor graphics, int screenWidth, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        if (lumen$swapped) { header = lumen$header; footer = lumen$footer; lumen$swapped = false; }
    }
}
