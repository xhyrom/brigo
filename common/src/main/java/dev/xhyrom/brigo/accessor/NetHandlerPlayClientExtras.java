package dev.xhyrom.brigo.accessor;

import com.mojang.brigadier.CommandDispatcher;
import dev.xhyrom.brigo.client.ClientSuggestionProvider;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import org.jetbrains.annotations.NotNull;

public interface NetHandlerPlayClientExtras {
    ClientSuggestionProvider brigo$suggestionsProvider();
    @NotNull CommandDispatcher<ISuggestionProvider> brigo$commands();
}
