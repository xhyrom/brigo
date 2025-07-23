package dev.xhyrom.brigo.mixin;

import com.google.common.base.Predicate;
import dev.xhyrom.brigo.accessor.GuiTextFieldExtras;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiTextField;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.BiFunction;

@Mixin(GuiTextField.class)
public abstract class GuiTextFieldMixin implements GuiTextFieldExtras {
    @Shadow private String text;
    @Shadow @Final private FontRenderer fontRenderer;
    @Shadow public int x;
    @Shadow public int y;
    @Shadow @Final private int width;
    @Shadow @Final private int height;
    @Shadow private int cursorPosition;
    @Shadow private int lineScrollOffset;
    @Shadow private int selectionEnd;
    @Shadow private boolean isFocused;
    @Shadow private int cursorCounter;
    @Shadow private boolean isEnabled;
    @Shadow private int enabledColor;
    @Shadow private int disabledColor;
    @Shadow private boolean enableBackgroundDrawing;
    @Shadow private Predicate<String> validator;
    @Shadow private GuiPageButtonList.GuiResponder guiResponder;
    @Shadow @Final private int id;

    @Shadow public abstract void setText(String textIn);
    @Shadow public abstract boolean getVisible();
    @Shadow public abstract boolean getEnableBackgroundDrawing();
    @Shadow public abstract int getWidth();
    @Shadow public abstract int getMaxStringLength();
    @Shadow protected abstract void drawSelectionBox(int startX, int startY, int endX, int endY);
    @Shadow public abstract void setResponderEntryValue(int idIn, String textIn);

    @Unique private String brigo$currentSuggestion;
    @Unique private BiFunction<String, Integer, String> brigo$textFormatter = (text, pos) -> text;

    @Inject(method = "setText", at = @At("TAIL"))
    private void onTextSet(String text, CallbackInfo ci) {
        if (this.validator.apply(text)) {
            this.setResponderEntryValue(this.id, text);
        }
    }

    @Inject(method = "drawTextBox", at = @At("HEAD"), cancellable = true)
    private void renderCustomTextBox(CallbackInfo ci) {
        if (!this.getVisible()) {
            ci.cancel();
            return;
        }

        this.brigo$renderBackground();

        int textColor = this.isEnabled ? this.enabledColor : this.disabledColor;
        int cursorPos = this.cursorPosition - this.lineScrollOffset;
        int selectionEnd = this.selectionEnd - this.lineScrollOffset;

        String visibleText = this.fontRenderer.trimStringToWidth(
                this.text.substring(this.lineScrollOffset),
                this.getWidth()
        );

        boolean shouldShowCursor = cursorPos >= 0 && cursorPos <= visibleText.length();
        boolean showBlinkingCursor = this.isFocused && this.cursorCounter / 6 % 2 == 0 && shouldShowCursor;

        int textX = this.enableBackgroundDrawing ? this.x + 4 : this.x;
        int textY = this.enableBackgroundDrawing ? this.y + (this.height - 8) / 2 : this.y;
        int renderX = textX;

        selectionEnd = Math.min(selectionEnd, visibleText.length());

        if (!visibleText.isEmpty()) {
            String textBeforeCursor = shouldShowCursor ? visibleText.substring(0, cursorPos) : visibleText;
            renderX = this.fontRenderer.drawStringWithShadow(
                    this.brigo$textFormatter.apply(textBeforeCursor, this.cursorPosition),
                    (float) textX,
                    (float) textY,
                    textColor
            );
        }

        boolean isAtEnd = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
        int cursorX = shouldShowCursor ? (isAtEnd ? renderX - 1 : renderX) :
                (cursorPos > 0 ? textX + this.width : textX);

        if (!visibleText.isEmpty() && shouldShowCursor && cursorPos < visibleText.length()) {
            this.fontRenderer.drawStringWithShadow(
                    this.brigo$textFormatter.apply(visibleText.substring(cursorPos), this.cursorPosition),
                    (float) renderX,
                    (float) textY,
                    textColor
            );
        }

        if (!isAtEnd && this.brigo$currentSuggestion != null) {
            this.fontRenderer.drawStringWithShadow(
                    this.brigo$currentSuggestion,
                    (float) (cursorX - 1),
                    (float) textY,
                    -8355712
            );
        }

        if (showBlinkingCursor) {
            if (isAtEnd) {
                Gui.drawRect(cursorX, textY - 1, cursorX + 1, textY + 1 + this.fontRenderer.FONT_HEIGHT, -3092272);
            } else {
                this.fontRenderer.drawStringWithShadow("_", (float) cursorX, (float) textY, textColor);
            }
        }

        if (selectionEnd != cursorPos) {
            int selectionX = textX + this.fontRenderer.getStringWidth(visibleText.substring(0, selectionEnd));
            this.drawSelectionBox(cursorX, textY - 1, selectionX - 1, textY + 1 + this.fontRenderer.FONT_HEIGHT);
        }

        ci.cancel();
    }

    @Unique
    private void brigo$renderBackground() {
        if (this.getEnableBackgroundDrawing()) {
            Gui.drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, -6250336);
            Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, -16777216);
        }
    }

    @Unique
    @Override
    public void suggestion(@Nullable String suggestion) {
        this.brigo$currentSuggestion = suggestion;
    }

    @Unique
    @Override
    public void textFormatter(BiFunction<String, Integer, String> formatter) {
        this.brigo$textFormatter = formatter;
    }

    @Unique
    @Override
    public int screenX(int position) {
        return position > this.text.length() ?
                this.x :
                this.x + fontRenderer.getStringWidth(this.text.substring(0, position));
    }
}