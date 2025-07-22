package dev.xhyrom.brigo.mixin;

import dev.xhyrom.brigo.client.gui.CommandSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
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
    private void initGui(CallbackInfo ci) {
        this.inputField.setGuiResponder(new GuiPageButtonList.GuiResponder() {
            @Override
            public void setEntryValue(int id, boolean value) {

            }
            @Override
            public void setEntryValue(int id, float value) {

            }

            @Override
            public void setEntryValue(int id, String value) {
                String s = inputField.getText();

                brigo$commandSuggestions.setAllowSuggestions(!s.equals(defaultInputFieldText));
                brigo$commandSuggestions.updateCommandInfo();
            }
        });
        this.brigo$commandSuggestions = new CommandSuggestions(this.mc, this, this.inputField, this.fontRenderer, false, false, 1, 10, true, -805306368);
        this.brigo$commandSuggestions.updateCommandInfo();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void keyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (this.brigo$commandSuggestions.keyPressed(typedChar, keyCode, 0)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    private void handleMouseInput(CallbackInfo ci) {
        int scroll = Mouse.getEventDWheel();
        if (scroll == 0) return;

        ScaledResolution scaled = new ScaledResolution(mc); // TODO: cache
        double mouseX = Mouse.getEventX() * scaled.getScaledWidth() / mc.displayWidth;
        double mouseY = scaled.getScaledHeight() - Mouse.getEventY() * scaled.getScaledHeight() / mc.displayHeight - 1;


        System.out.println("Mouse scrolled at: " + mouseX + ", " + mouseY);
        this.brigo$commandSuggestions.mouseScrolled(mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (this.brigo$commandSuggestions.mouseClicked(mouseX, mouseY, mouseButton)) {
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
    private void getSentHistory(int msgPos, CallbackInfo ci) {
        this.brigo$commandSuggestions.setAllowSuggestions(false);
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        this.brigo$commandSuggestions.render(mouseX, mouseY);
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        String string = this.inputField.getText();
        this.setWorldAndResolution(mcIn, w, h);
        this.inputField.setText(string);
        this.brigo$commandSuggestions.updateCommandInfo();
    }
}
