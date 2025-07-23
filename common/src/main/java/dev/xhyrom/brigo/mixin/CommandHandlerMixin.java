package dev.xhyrom.brigo.mixin;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.xhyrom.brigo.accessor.CommandHandlerExtras;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.command.CommandSource;
import dev.xhyrom.brigo.command.CommandsPacket;
import dev.xhyrom.brigo.util.SuggestionProviders;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(CommandHandler.class)
public class CommandHandlerMixin implements CommandHandlerExtras {
    @Unique
    private CommandDispatcher<CommandSource> brigo$dispatcher = new CommandDispatcher<>();

    @Inject(method = "registerCommand", at = @At("HEAD"))
    private void registerCommand(ICommand command, CallbackInfoReturnable<ICommand> cir) {
        brigo$dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal(command.getName())
                    .then(
                            RequiredArgumentBuilder.<CommandSource, String>argument("params", StringArgumentType.greedyString())
                                    .suggests((ctx, suggestions) -> suggestions.buildFuture())
                    )
        );
    }

    @Unique
    public void sendCommands(@NotNull EntityPlayerMP player) {
        Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> map = Maps.newHashMap();
        RootCommandNode<ISuggestionProvider> rootCommandNode = new RootCommandNode<>();
        map.put(this.brigo$dispatcher.getRoot(), rootCommandNode);
        this.brigo$fillUsableCommands(this.brigo$dispatcher.getRoot(), rootCommandNode, CommandSource.adapt(player), map);
        player.connection.sendPacket(CommandsPacket.create(rootCommandNode));
    }

    @Unique
    private void brigo$fillUsableCommands(CommandNode<CommandSource> pRootCommandSource, CommandNode<ISuggestionProvider> pRootSuggestion, CommandSource pSource, Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> pCommandNodeToSuggestionNode)
    {
        for (CommandNode<CommandSource> commandnode : pRootCommandSource.getChildren())
        {
            if (commandnode.canUse(pSource))
            {
                ArgumentBuilder<ISuggestionProvider, ?> argumentbuilder = (ArgumentBuilder) commandnode.createBuilder();
                argumentbuilder.requires((p_82126_) ->
                        true);

                if (argumentbuilder.getCommand() != null)
                {
                    argumentbuilder.executes((p_82102_) ->
                            0);
                }

                if (argumentbuilder instanceof RequiredArgumentBuilder)
                {
                    RequiredArgumentBuilder<ISuggestionProvider, ?> requiredargumentbuilder = (RequiredArgumentBuilder) argumentbuilder;

                    if (requiredargumentbuilder.getSuggestionsProvider() != null)
                    {
                        requiredargumentbuilder.suggests(SuggestionProviders.safelySwap(requiredargumentbuilder.getSuggestionsProvider()));
                    }
                }

                if (argumentbuilder.getRedirect() != null)
                {
                    argumentbuilder.redirect(pCommandNodeToSuggestionNode.get(argumentbuilder.getRedirect()));
                }

                CommandNode<ISuggestionProvider> commandnode1 = argumentbuilder.build();
                pCommandNodeToSuggestionNode.put(commandnode, commandnode1);
                pRootSuggestion.addChild(commandnode1);

                if (!commandnode.getChildren().isEmpty())
                {
                    this.brigo$fillUsableCommands(commandnode, commandnode1, pSource, pCommandNodeToSuggestionNode);
                }
            }
        }
    }

    @Unique
    @Override
    public CommandDispatcher<CommandSource> dispatcher() {
        return brigo$dispatcher;
    }
}
