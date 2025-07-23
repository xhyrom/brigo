package dev.xhyrom.brigo.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public interface ISuggestionProvider {
    Collection<String> getPlayerNames();

    CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context, SuggestionsBuilder suggestionsBuilder);
    CompletableFuture<Suggestions> getSuggestionsFromServer();

    static CompletableFuture<Suggestions> suggest(Iterable<String> iterable, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(String string2 : iterable) {
            if (string2.toLowerCase(Locale.ROOT).startsWith(string)) {
                suggestionsBuilder.suggest(string2);
            }
        }

        return suggestionsBuilder.buildFuture();
    }
}
