package dev.xhyrom.brigo.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.util.SuggestionProviders;

import java.util.Map;

public class CommandTreeConverter {
    private final Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> nodeMapping;
    private final CommandSource source;

    public CommandTreeConverter(Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> nodeMapping,
                         CommandSource source) {
        this.nodeMapping = nodeMapping;
        this.source = source;
    }

    public void convertChildren(CommandNode<CommandSource> serverNode,
                                CommandNode<ISuggestionProvider> clientNode) {
        for (CommandNode<CommandSource> child : serverNode.getChildren()) {
            if (child.canUse(source)) {
                CommandNode<ISuggestionProvider> convertedChild = convertNode(child);
                nodeMapping.put(child, convertedChild);
                clientNode.addChild(convertedChild);

                convertChildren(child, convertedChild);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private CommandNode<ISuggestionProvider> convertNode(CommandNode<CommandSource> serverNode) {
        ArgumentBuilder<ISuggestionProvider, ?> builder =
                (ArgumentBuilder) serverNode.createBuilder();

        return configureBuilder(builder, serverNode).build();
    }

    private ArgumentBuilder<ISuggestionProvider, ?> configureBuilder(
            ArgumentBuilder<ISuggestionProvider, ?> builder,
            CommandNode<CommandSource> serverNode) {

        builder.requires(client -> true);

        if (builder.getCommand() != null) {
            builder.executes(context -> 0);
        }

        if (builder instanceof RequiredArgumentBuilder) {
            configureArgumentBuilder((RequiredArgumentBuilder<ISuggestionProvider, ?>) builder);
        }

        if (builder.getRedirect() != null) {
            builder.redirect(nodeMapping.get(builder.getRedirect()));
        }

        return builder;
    }

    private void configureArgumentBuilder(RequiredArgumentBuilder<ISuggestionProvider, ?> builder) {
        if (builder.getSuggestionsProvider() != null) {
            builder.suggests(SuggestionProviders.safelySwap(builder.getSuggestionsProvider()));
        } else {
            builder.suggests(SuggestionProviders.ASK_SERVER);
        }
    }
}
