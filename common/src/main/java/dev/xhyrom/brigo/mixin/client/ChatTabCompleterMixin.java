package dev.xhyrom.brigo.mixin.client;

import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.ChatTabCompleter.class)
public class ChatTabCompleterMixin {
    @Inject(method = "complete", at = @At("HEAD"), cancellable = true)
    private void onComplete(CallbackInfo ci) {
        ci.cancel();
    }
}
