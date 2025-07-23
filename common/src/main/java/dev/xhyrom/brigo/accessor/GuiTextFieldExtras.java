package dev.xhyrom.brigo.accessor;

import org.jetbrains.annotations.Nullable;
import java.util.function.BiFunction;

public interface GuiTextFieldExtras {
    void brigo$suggestion(@Nullable String suggestion);
    void brigo$textFormatter(BiFunction<String, Integer, String> formatter);
    int brigo$screenX(int position);
}