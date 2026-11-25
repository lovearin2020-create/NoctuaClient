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
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Sees every line added to the chat window (system and player messages), and applies Chat Tweaks. */
@Mixin(ChatComponent.class)
public abstract class ChatListenerMixin implements dev.lumen.fabric.util.ChatAccess {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.client.Minecraft minecraft;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private java.util.List<net.minecraft.client.multiplayer.chat.GuiMessage> allMessages;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private java.util.List<net.minecraft.client.multiplayer.chat.GuiMessage.Line> trimmedMessages;
    @org.spongepowered.asm.mixin.Shadow private int chatScrollbarPos;
    @org.spongepowered.asm.mixin.Shadow private double getScale() { return 1; }
    @org.spongepowered.asm.mixin.Shadow private int getWidth() { return 0; }
    @org.spongepowered.asm.mixin.Shadow public abstract int getLinesPerPage();

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true)
    private Component lumen$compact(Component message) { return Hooks.chatCompact(message, allMessages, trimmedMessages); }

    /** Mirrors the layout in extractRenderState. */
    public String lumen$messageAt(double mouseX, double mouseY, int screenHeight) {
        float scale = (float) getScale();
        double x = mouseX / scale - 4, y = mouseY / scale;
        int maxWidth = net.minecraft.util.Mth.ceil(getWidth() / scale);
        int chatBottom = net.minecraft.util.Mth.floor((screenHeight - 40) / scale);
        double spacing = minecraft.options.chatLineSpacing().get();
        int entryHeight = (int) (9 * (spacing + 1.0));
        if (entryHeight <= 0 || x < -4 || x > maxWidth + 8 || y > chatBottom) return null;
        int i = (int) Math.floor((chatBottom - y) / entryHeight);
        int shown = Math.min(trimmedMessages.size() - chatScrollbarPos, getLinesPerPage());
        if (i < 0 || i >= shown) return null;
        return trimmedMessages.get(i + chatScrollbarPos).parent().content().getString();
    }

    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    private void lumen$chat(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (message != null && Hooks.chat(message.getString())) ci.cancel();
    }

    @ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
    private int lumen$history(int original) { return Hooks.chatHistory(original); }

    @ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("STORE"), ordinal = 2)
    private float lumen$background(float backgroundOpacity) { return Hooks.chatBackground(backgroundOpacity); }
}
