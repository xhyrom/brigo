package dev.xhyrom.brigo.compat;

import com.google.common.collect.Maps;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.xhyrom.brigo.BrigoClient;
import dev.xhyrom.brigo.command.CommandSource;
import dev.xhyrom.brigo.compat.mods.*;
import dev.xhyrom.brigo.platform.Services;
import me.lucko.commodore.file.CommodoreFileReader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class CompatRegistry {
    private static final Map<String, LiteralCommandNode<CommandSource>> REGISTERED_COMMANDS = Maps.newHashMap();

    public static void init() {
        register("minecraft", MinecraftMod::new);
        register("baubles", BaublesMod::new);
        register("thaumcraft", ThaumcraftMod::new);
    }

    public static boolean hasCompatFor(Class<?> clazz) {
        return REGISTERED_COMMANDS.containsKey(clazz.getName());
    }

    public static LiteralCommandNode<CommandSource> getCompatCommand(Class<?> clazz) {
        return REGISTERED_COMMANDS.get(clazz.getName());
    }

    private static InputStream getResource(final String path) {
        return CompatRegistry.class.getResourceAsStream("/assets/brigo/commands/" + path);
    }

    private static void register(final @NotNull String modId, final @NotNull Supplier<CompatMod> lazyMod) {
        if (!Services.AGNOS.isModLoaded(modId)) return;

        BrigoClient.LOGGER.info("Loading compatibility for mod: {}", modId);

        CompatMod mod = lazyMod.get();
        mod.commands().forEach(clazz -> {
            BrigoClient.LOGGER.info("Registering compatibility command: {}", clazz);

            try {
                REGISTERED_COMMANDS.put(clazz, CommodoreFileReader.INSTANCE.parse(getResource(mod.identifier() + "/" + clazz + ".commodore")));
                BrigoClient.LOGGER.info("Registered compatibility command: {}", clazz);
            } catch (IOException e) {
                BrigoClient.LOGGER.error("Failed to load compatibility for {} in mod {}", clazz, mod.identifier(), e);
            }
        });
    }
}
