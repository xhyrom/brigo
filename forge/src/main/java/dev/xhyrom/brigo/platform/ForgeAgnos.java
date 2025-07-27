package dev.xhyrom.brigo.platform;

import dev.xhyrom.brigo.platform.services.Agnos;
import net.minecraftforge.fml.common.Loader;
import org.jetbrains.annotations.NotNull;

public class ForgeAgnos implements Agnos {
    @Override
    public boolean isModLoaded(@NotNull String modId) {
        return Loader.isModLoaded(modId);
    }
}
