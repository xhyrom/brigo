package dev.xhyrom.brigo.mixin;

import dev.xhyrom.brigo.client.gui.CommandSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public class GuiChatMixin extends GuiScreen {
    @Shadow protected GuiTextField inputField;
    @Shadow private String defaultInputFieldText;

    @Unique private CommandSuggestions brigo$commandSuggestions;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        this.brigo$setupInputResponder();
        this.brigo$initializeCommandSuggestions();
    }

    @Unique
    private void brigo$setupInputResponder() {
        this.inputField.setGuiResponder(new GuiPageButtonList.GuiResponder() {
            @Override
            public void setEntryValue(int id, boolean value) {}

            @Override
            public void setEntryValue(int id, float value) {}

            @Override
            public void setEntryValue(int id, String value) {
                String currentText = inputField.getText();

                brigo$commandSuggestions.allowSuggestions(!currentText.equals(defaultInputFieldText));
                brigo$commandSuggestions.updateCommandInfo();
            }
        });
    }

    @Unique
    private void brigo$initializeCommandSuggestions() {
        CommandSuggestions.CommandSuggestionsConfig config =
                new CommandSuggestions.CommandSuggestionsConfig(
                        false,
                        false,
                        1,
                        10,
                        true,
                        -805306368
                );

        this.brigo$commandSuggestions = new CommandSuggestions(
                this.mc, this, this.inputField, this.fontRenderer, config
        );
        this.brigo$commandSuggestions.updateCommandInfo();
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    private void onHandleMouseInput(CallbackInfo ci) {
        this.brigo$commandSuggestions.handleMouseScroll(Mouse.getEventDWheel());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (this.brigo$commandSuggestions.handleMouseClick(mouseX, mouseY, mouseButton)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "getSentHistory",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiTextField;setText(Ljava/lang/String;)V",
                    ordinal = 1
            )
    )
    private void onHistoryNavigation(int msgPos, CallbackInfo ci) {
        this.brigo$commandSuggestions.allowSuggestions(false);
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        this.brigo$commandSuggestions.render(mouseX, mouseY);
    }

    @Override
    public void handleKeyboardInput() {
        if (Keyboard.getEventKeyState() && this.brigo$commandSuggestions.handleKeyPress(Keyboard.getEventKey())) {
            this.mc.dispatchKeypresses();
            return;
        }

        super.handleKeyboardInput();
    }

    @Override
    public void onResize(Minecraft mc, int width, int height) {
        String currentText = this.inputField.getText();

        this.setWorldAndResolution(mc, width, height);
        this.inputField.setText(currentText);
        this.brigo$commandSuggestions.updateCommandInfo();
    }
}