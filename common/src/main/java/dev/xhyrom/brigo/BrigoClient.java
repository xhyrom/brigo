package dev.xhyrom.brigo;

import dev.xhyrom.brigo.command.serialization.ArgumentTypes;
import dev.xhyrom.brigo.compat.CompatRegistry;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

public class BrigoClient {
    public static final String MOD_ID = "brigo";

    public static final TaggedLogger LOGGER = Logger.tag(MOD_ID);

    public static void init() {
        ArgumentTypes.init();
        CompatRegistry.init();

        LOGGER.info("Brigo initialized.");
    }
}
