package dev.xhyrom.brigo.compat;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CompatMod {
    @NotNull String identifier();
    List<String> commands();
}
