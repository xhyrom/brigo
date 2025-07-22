package dev.xhyrom.brigo;

import net.fabricmc.api.ModInitializer;

public class BrigoClientOrnithe implements ModInitializer {
    @Override
    public void onInitialize() {
        BrigoClient.init();
    }
}
