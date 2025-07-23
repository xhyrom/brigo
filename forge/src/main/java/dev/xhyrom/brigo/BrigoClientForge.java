package dev.xhyrom.brigo;

import net.minecraftforge.fml.common.Mod;

@Mod(modid = BrigoClient.MOD_ID, useMetadata = true, acceptableRemoteVersions = "*")
public class BrigoClientForge {
    public BrigoClientForge() {
        BrigoClient.init();
    }
}
