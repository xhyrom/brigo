package dev.xhyrom.brigo;


import cpw.mods.fml.common.Mod;

@Mod(modid = BrigoClient.MOD_ID, useMetadata = true, acceptableRemoteVersions = "*")
public class BrigoClientForge {
    public BrigoClientForge() {
        BrigoClient.init();
    }
}
