package dev.xhyrom.brigo.platform;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import dev.xhyrom.brigo.platform.services.Agnos;

import java.nio.file.Path;

public class ForgeAgnos implements Agnos {
    @Override
    public boolean isClient() {
        return FMLCommonHandler.instance().getSide() == Side.CLIENT;
    }

    @Override
    public Path configDir() {
        return Loader.instance().getConfigDir().toPath();
    }
}
