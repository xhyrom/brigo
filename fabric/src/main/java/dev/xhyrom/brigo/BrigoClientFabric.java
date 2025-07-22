package dev.xhyrom.brigo;

import net.fabricmc.api.ModInitializer;

public class BrigoClientFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BrigoClient.init();
    }
}
