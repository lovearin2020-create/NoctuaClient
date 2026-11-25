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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;

@Mixin(Hud.class)
public class HudMixin {
    private boolean lumen$titleScaled;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void lumen$hud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) { Hooks.hud(graphics); }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void lumen$crosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) { if (Hooks.crosshair(graphics)) ci.cancel(); }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void lumen$sidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) { if (Hooks.sidebar(graphics, objective)) ci.cancel(); }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void lumen$boss(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) { if (Hooks.hideBossBar()) ci.cancel(); }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void lumen$effects(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) { if (Hooks.hideVanillaEffects()) ci.cancel(); }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void lumen$titlePre(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        lumen$titleScaled = false;
        if (Hooks.hideTitles()) { ci.cancel(); return; }
        float k = Hooks.titleScale();
        if (k == 1f) return;
        float cx = graphics.guiWidth() / 2, cy = graphics.guiHeight() / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(k, k);
        graphics.pose().translate(-cx, -cy);
        lumen$titleScaled = true;
    }

    @Inject(method = "extractTitle", at = @At("RETURN"))
    private void lumen$titlePost(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (lumen$titleScaled) { graphics.pose().popMatrix(); lumen$titleScaled = false; }
    }

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void lumen$titleSeen(net.minecraft.network.chat.Component title, CallbackInfo ci) { Hooks.title(title); }
}
