package dev.xhyrom.brigo.util;

import com.google.common.collect.Maps;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SuggestionProviders {
    private static final Map<ResourceLocation, SuggestionProvider<ISuggestionProvider>> REGISTRY = Maps.newHashMap();
    private static final ResourceLocation ASK_SERVER_ID = new ResourceLocation("minecraft:ask_server");
    public static final SuggestionProvider<ISuggestionProvider> ASK_SERVER;

    @SuppressWarnings("unchecked")
    public static <S extends ISuggestionProvider> SuggestionProvider<S> register(ResourceLocation resourceLocation, SuggestionProvider<ISuggestionProvider> suggestionProvider) {
        if (REGISTRY.containsKey(resourceLocation)) {
            throw new IllegalArgumentException("A command suggestion provider is already registered with the name " + resourceLocation);
        } else {
            REGISTRY.put(resourceLocation, suggestionProvider);
            return (SuggestionProvider<S>) new SuggestionProviders.Wrapper(resourceLocation, suggestionProvider);
        }
    }

    public static SuggestionProvider<ISuggestionProvider> get(ResourceLocation resourceLocation) {
        return REGISTRY.getOrDefault(resourceLocation, ASK_SERVER);
    }

    public static ResourceLocation getId(SuggestionProvider<ISuggestionProvider> suggestionProvider) {
        return suggestionProvider instanceof Wrapper ? ((Wrapper)suggestionProvider).id : ASK_SERVER_ID;
    }

    public static SuggestionProvider<ISuggestionProvider> safelySwap(SuggestionProvider<ISuggestionProvider> suggestionProvider) {
        return suggestionProvider instanceof Wrapper ? suggestionProvider : ASK_SERVER;
    }

    static {
        ASK_SERVER = register(ASK_SERVER_ID, (commandContext, suggestionsBuilder) -> commandContext.getSource().getSuggestionsFromServer(commandContext, suggestionsBuilder));
    }

    public static class Wrapper implements SuggestionProvider<ISuggestionProvider> {
        private final SuggestionProvider<ISuggestionProvider> provider;
        private final ResourceLocation id;

        public Wrapper(ResourceLocation id, SuggestionProvider<ISuggestionProvider> provider) {
            this.provider = provider;
            this.id = id;
        }

        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ISuggestionProvider> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
            return this.provider.getSuggestions(commandContext, suggestionsBuilder);
        }
    }
}
