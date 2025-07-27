package dev.xhyrom.brigo.compat.mods;

import dev.xhyrom.brigo.compat.CompatMod;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BaublesMod implements CompatMod {
    @Override
    public @NotNull String identifier() {
        return "baubles";
    }

    @Override
    public List<String> commands() {
        return Collections.singletonList("baubles.common.event.CommandBaubles");
    }
}
