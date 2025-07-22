package dev.xhyrom.brigo.mixin;

import dev.xhyrom.brigo.accessor.GuiTextFieldExtras;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.BiFunction;

@Mixin(GuiTextField.class)
public abstract class GuiTextFieldMixin implements GuiTextFieldExtras {
    @Shadow private String text;
    @Shadow @Final private FontRenderer fontRenderer;
    @Shadow public int x;

    @Shadow public abstract void setText(String textIn);

    @Shadow public abstract boolean getVisible();

    @Shadow public abstract boolean getEnableBackgroundDrawing();

    @Shadow private boolean isEnabled;
    @Shadow private int enabledColor;
    @Shadow private int disabledColor;
    @Shadow public int y;
    @Final @Shadow private int width;
    @Final @Shadow private int height;

    @Shadow private int cursorPosition;
    @Shadow private int lineScrollOffset;
    @Shadow private int selectionEnd;
    @Shadow private boolean isFocused;
    @Shadow private int cursorCounter;

    @Shadow public abstract int getWidth();

    @Shadow private boolean enableBackgroundDrawing;

    @Shadow public abstract int getMaxStringLength();

    @Shadow protected abstract void drawSelectionBox(int startX, int startY, int endX, int endY);

    @Unique
    private String suggestion;
    @Unique
    private BiFunction<String, Integer, String> textFormatter = (string, integer) -> string;

    @Inject(method = "drawTextBox", at = @At("HEAD"), cancellable = true)
    private void drawTextBox(CallbackInfo ci) {
        if (this.getVisible()) {
            if (this.getEnableBackgroundDrawing()) {
                Gui.drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, -6250336);
                Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, -16777216);
            }

            int i = this.isEnabled ? this.enabledColor : this.disabledColor;
            int j = this.cursorPosition - this.lineScrollOffset;
            int k = this.selectionEnd - this.lineScrollOffset;
            String string = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
            boolean bl = j >= 0 && j <= string.length();
            boolean bl2 = this.isFocused && this.cursorCounter / 6 % 2 == 0 && bl;
            int l = this.enableBackgroundDrawing ? this.x + 4 : this.x;
            int m = this.enableBackgroundDrawing ? this.y + (this.height - 8) / 2 : this.y;
            int n = l;
            if (k > string.length()) {
                k = string.length();
            }

            if (!string.isEmpty()) {
                String string2 = bl ? string.substring(0, j) : string;
                n = this.fontRenderer.drawStringWithShadow(this.textFormatter.apply(string2, this.cursorPosition), (float)l, (float)m, i);
            }

            boolean bl3 = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
            int o = n;
            if (!bl) {
                o = j > 0 ? l + this.width : l;
            } else if (bl3) {
                o = n - 1;
                --n;
            }

            if (!string.isEmpty() && bl && j < string.length()) {
                n = this.fontRenderer.drawStringWithShadow(this.textFormatter.apply(string.substring(j), this.cursorPosition), (float)n, (float)m, i);
            }

            if (!bl3 && this.suggestion != null)
            {
                this.fontRenderer.drawStringWithShadow(this.suggestion, (float)(o - 1), (float)m, -8355712);
            }

            if (bl2) {
                if (bl3) {
                    Gui.drawRect(o, m - 1, o + 1, m + 1 + this.fontRenderer.FONT_HEIGHT, -3092272);
                } else {
                    this.fontRenderer.drawStringWithShadow("_", (float)o, (float)m, i);
                }
            }

            if (k != j) {
                int p = l + this.fontRenderer.getStringWidth(string.substring(0, k));
                this.drawSelectionBox(o, m - 1, p - 1, m + 1 + this.fontRenderer.FONT_HEIGHT);
            }
        }

        ci.cancel();
    }

    @Unique
    @Override
    public void suggestion(@Nullable String string) {
        this.suggestion = string;
    }

    @Unique
    @Override
    public void textFormatter(BiFunction<String, Integer, String> formatter) {
        this.textFormatter = formatter;
    }

    @Unique
    @Override
    public int screenX(int i) {
        return i > this.text.length() ? this.x : this.x + fontRenderer.getStringWidth(this.text.substring(0, i));
    }
}
