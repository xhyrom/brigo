package dev.xhyrom.brigo.platform;

import dev.xhyrom.brigo.platform.services.Agnos;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

public class FabricAgnos implements Agnos {
    @Override
    public boolean isModLoaded(@NotNull String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
