package dev.xhyrom.brigo.compat;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface CompatMod {
    @NotNull String identifier();
    Collection<String> commands();
}
