package dev.xhyrom.brigo;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

public class BrigoClient {
    public static final String MOD_ID = "brigo";

    public static final TaggedLogger LOGGER = Logger.tag(MOD_ID);

    public static void init() {
        Config.INSTANCE.id();
    }
}
