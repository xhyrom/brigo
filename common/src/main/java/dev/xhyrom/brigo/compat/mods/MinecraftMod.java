package dev.xhyrom.brigo.compat.mods;

import dev.xhyrom.brigo.compat.CompatMod;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public class MinecraftMod implements CompatMod {
    @Override
    public @NotNull String identifier() {
        return "minecraft";
    }

    @Override
    public Collection<String> commands() {
        return Arrays.asList(
                "net.minecraft.command.AdvancementCommand",
                "net.minecraft.command.CommandDefaultGameMode",
                "net.minecraft.command.CommandDifficulty",
                "net.minecraft.command.CommandGameMode",
                "net.minecraft.command.CommandGameRule",
                "net.minecraft.command.CommandTime",
                "net.minecraft.command.CommandWeather",
                "net.minecraft.command.server.CommandWhitelist"
        );
    }
}
