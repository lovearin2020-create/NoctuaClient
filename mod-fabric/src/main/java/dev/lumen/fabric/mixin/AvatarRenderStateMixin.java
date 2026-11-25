package dev.lumen.fabric.mixin;

import dev.lumen.fabric.cosmetics.OutfitState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements OutfitState {
    @Unique private String lumen$wings, lumen$hat, lumen$shield;
    @Unique private boolean lumen$airborne;

    @Override public void lumen$outfit(String wings, String hat, String shield, boolean airborne) { lumen$wings = wings; lumen$hat = hat; lumen$shield = shield; lumen$airborne = airborne; }
    @Override public String lumen$wings() { return lumen$wings; }
    @Override public String lumen$hat() { return lumen$hat; }
    @Override public String lumen$shield() { return lumen$shield; }
    @Override public boolean lumen$airborne() { return lumen$airborne; }
}
