package dev.lumen.fabric.mixin;

import dev.lumen.fabric.Hooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

@Mixin(EditBox.class)
public class EditBoxMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void lumen$typed(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.textTyped((EditBox) (Object) this, event.codepoint())) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void lumen$key(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.textKey((EditBox) (Object) this, event.key())) cir.setReturnValue(true);
    }

    @Inject(method = "extractWidgetRenderState", at = @At("TAIL"))
    private void lumen$badge(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) { Hooks.textBoxDrawn((EditBox) (Object) this, graphics); }
}
