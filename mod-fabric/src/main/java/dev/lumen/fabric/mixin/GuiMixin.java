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
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;

@Mixin(Gui.class)
public class GuiMixin {
    /** Main Menu: a title screen passed in by the game is swapped for the Lumen one. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen lumen$mainMenu(Screen next) { return Hooks.screen(next); }

    /** The same for the title screen the method creates itself when asked to close the current screen. */
    @ModifyVariable(method = "setScreen", at = @At("STORE"), argsOnly = true)
    private Screen lumen$mainMenuCreated(Screen next) { return Hooks.screen(next); }
}
