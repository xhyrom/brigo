package dev.xhyrom.brigo.accessor;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public interface GuiTextFieldExtras {
    void suggestion(@Nullable String suggestion);
    void textFormatter(BiFunction<String, Integer, String> formatter);
    int screenX(int i);
}
