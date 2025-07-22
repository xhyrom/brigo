package dev.xhyrom.brigo.util;

import com.google.common.collect.Maps;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.command.CommandSource;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SuggestionProviders {
    private static final Map<ResourceLocation, SuggestionProvider<ISuggestionProvider>> REGISTRY = Maps.newHashMap();
    private static final ResourceLocation ASK_SERVER_ID = new ResourceLocation("minecraft:ask_server");
    public static final SuggestionProvider<ISuggestionProvider> ASK_SERVER;
    public static final SuggestionProvider<CommandSource> ALL_RECIPES;
    public static final SuggestionProvider<CommandSource> AVAILABLE_SOUNDS;

    public static <S extends ISuggestionProvider> SuggestionProvider<S> register(ResourceLocation resourceLocation, SuggestionProvider<ISuggestionProvider> suggestionProvider) {
        if (REGISTRY.containsKey(resourceLocation)) {
            throw new IllegalArgumentException("A command suggestion provider is already registered with the name " + resourceLocation);
        } else {
            REGISTRY.put(resourceLocation, suggestionProvider);
            return (SuggestionProvider<S>) new SuggestionProviders.Wrapper(resourceLocation, suggestionProvider);
        }
    }

    public static SuggestionProvider<ISuggestionProvider> get(ResourceLocation resourceLocation) {
        return (SuggestionProvider)REGISTRY.getOrDefault(resourceLocation, ASK_SERVER);
    }

    public static ResourceLocation getId(SuggestionProvider<ISuggestionProvider> suggestionProvider) {
        return suggestionProvider instanceof Wrapper ? ((Wrapper)suggestionProvider).id : ASK_SERVER_ID;
    }

    public static SuggestionProvider<ISuggestionProvider> safelySwap(SuggestionProvider<ISuggestionProvider> suggestionProvider) {
        return suggestionProvider instanceof Wrapper ? suggestionProvider : ASK_SERVER;
    }

    static {
        ASK_SERVER = register(ASK_SERVER_ID, (commandContext, suggestionsBuilder) -> ((ISuggestionProvider)commandContext.getSource()).getSuggestionsFromServer(commandContext, suggestionsBuilder));
        ALL_RECIPES = register(new ResourceLocation("minecraft:all_recipes"), (commandContext, suggestionsBuilder) -> ISuggestionProvider.suggestIterable(((ISuggestionProvider)commandContext.getSource()).getRecipeResourceLocations(), suggestionsBuilder));
        AVAILABLE_SOUNDS = register(new ResourceLocation("minecraft:available_sounds"), (commandContext, suggestionsBuilder) -> ISuggestionProvider.suggestIterable(((ISuggestionProvider)commandContext.getSource()).getSoundResourceLocations(), suggestionsBuilder));
    }

    public static class Wrapper implements SuggestionProvider<ISuggestionProvider> {
        private final SuggestionProvider<ISuggestionProvider> provider;
        private final ResourceLocation id;

        public Wrapper(ResourceLocation p_i47984_1, SuggestionProvider<ISuggestionProvider> p_i47984_2) {
            this.provider = p_i47984_2;
            this.id = p_i47984_1;
        }

        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ISuggestionProvider> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
            return this.provider.getSuggestions(commandContext, suggestionsBuilder);
        }
    }
}
