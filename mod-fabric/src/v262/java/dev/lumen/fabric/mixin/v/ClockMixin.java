package dev.lumen.fabric.mixin.v;

import dev.lumen.fabric.Hooks;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Time Changer: the client-side world clock reports a fixed time. */
@Mixin(ClientClockManager.class)
public class ClockMixin {
    @Inject(method = "getTotalTicks", at = @At("RETURN"), cancellable = true)
    private void lumen$time(CallbackInfoReturnable<Long> cir) {
        long in = cir.getReturnValueJ(), out = Hooks.time(in);
        if (out != in) cir.setReturnValue(out);
    }
}
