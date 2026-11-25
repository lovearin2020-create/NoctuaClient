package dev.lumen.fabric.mixin;

import dev.lumen.fabric.Hooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class GameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void lumen$attack(Player player, Entity target, CallbackInfo ci) { Hooks.attack(target); }
}
