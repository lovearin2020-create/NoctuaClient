package dev.lumen.fabric.mixin.v;

import dev.lumen.fabric.Hooks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.7 Animations: the held item neither dips after an attack nor bobs when its stack changes. */
@Mixin(FirstPersonHandsAndItems.class)
public class HandMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float lumen$swap(LocalPlayer player, float partial) { return Hooks.swapScale(player.getItemSwapScale(partial)); }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"), cancellable = true)
    private void lumen$instant(CallbackInfoReturnable<Boolean> cir) { if (!cir.getReturnValueZ() && Hooks.instantReplace()) cir.setReturnValue(true); }
}
