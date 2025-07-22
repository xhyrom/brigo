package dev.xhyrom.brigo;

import dev.xhyrom.brigo.platform.Services;
import folk.sisby.kaleido.api.ReflectiveConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;

public class Config extends ReflectiveConfig {
    public static final Config INSTANCE = Config.createToml(Services.AGNOS.configDir(), BrigoClient.MOD_ID, BrigoClient.MOD_ID, Config.class);

    @Comment("debug mode")
    public final TrackedValue<Boolean> debug = this.value(false);
}