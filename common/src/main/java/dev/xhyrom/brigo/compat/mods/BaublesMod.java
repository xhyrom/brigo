package dev.xhyrom.brigo.compat.mods;

import dev.xhyrom.brigo.compat.CompatMod;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class BaublesMod implements CompatMod {
    @Override
    public @NotNull String identifier() {
        return "baubles";
    }

    @Override
    public Collection<String> commands() {
        return Collections.singletonList("baubles.common.event.CommandBaubles");
    }
}
