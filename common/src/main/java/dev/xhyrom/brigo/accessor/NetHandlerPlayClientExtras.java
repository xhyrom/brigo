package dev.xhyrom.brigo.accessor;

import com.mojang.brigadier.CommandDispatcher;
import dev.xhyrom.brigo.client.ClientSuggestionProvider;
import dev.xhyrom.brigo.client.ISuggestionProvider;

public interface NetHandlerPlayClientExtras {
    ClientSuggestionProvider suggestionsProvider();
    CommandDispatcher<ISuggestionProvider> commands();
}
