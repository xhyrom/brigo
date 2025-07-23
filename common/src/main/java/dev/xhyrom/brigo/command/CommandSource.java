package dev.xhyrom.brigo.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class CommandSource implements ISuggestionProvider {
    private final ICommandSender sender;

    public CommandSource(ICommandSender sender) {
        this.sender = sender;
    }

    public static CommandSource adapt(final ICommandSender sender) {
        return new CommandSource(sender);
    }

    @Override
    public Collection<String> getPlayerNames() {
        return Arrays.asList(this.sender.getServer().getOnlinePlayerNames());
    }

    @Override
    public Collection<String> getTeamNames() {
        return this.sender.getServer().getWorld(0).getScoreboard().getTeamNames();
    }

    @Override
    public Collection<ResourceLocation> getSoundResourceLocations() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ResourceLocation> getRecipeResourceLocations() {
        return Collections.emptyList();
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context, SuggestionsBuilder suggestionsBuilder) {
        return null;
    }

    @Override
    public Collection<Coordinates> getCoordinates(boolean allowFloatCoords) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermissionLevel(int i) {
        return this.sender.canUseCommand(i, "");
    }
}
