package dev.lumen.mixin;

import dev.lumen.Hooks;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {
    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void lumen$scoreboard(ScoreObjective objective, ScaledResolution res, CallbackInfo ci) {
        if (Hooks.scoreboard(objective, res)) ci.cancel();
    }

    @Inject(method = "displayTitle", at = @At("HEAD"))
    private void lumen$title(String title, String subTitle, int in, int stay, int out, CallbackInfo ci) { Hooks.title(title); }
}
