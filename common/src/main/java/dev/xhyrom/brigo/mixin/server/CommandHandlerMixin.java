package dev.xhyrom.brigo.mixin.server;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.xhyrom.brigo.accessor.CommandHandlerExtras;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.command.CommandSource;
import dev.xhyrom.brigo.command.CommandTreeConverter;
import dev.xhyrom.brigo.client.network.CommandsPacket;
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
    private final CommandDispatcher<CommandSource> brigo$dispatcher = new CommandDispatcher<>();

    @Inject(method = "registerCommand", at = @At("HEAD"))
    private void onRegisterCommand(ICommand command, CallbackInfoReturnable<ICommand> cir) {
        brigo$registerBrigoCommand(command);
    }

    @Unique
    private void brigo$registerBrigoCommand(ICommand command) {
        final LiteralCommandNode<CommandSource> node = brigo$dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal(command.getName())
                        .then(brigo$createParameterArgument())
        );

        command.getAliases().forEach(alias -> {
            if (alias.equals(command.getName())) return;

            brigo$dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal(alias)
                        .redirect(node)
            );
        });
    }

    @Unique
    private RequiredArgumentBuilder<CommandSource, String> brigo$createParameterArgument() {
        return RequiredArgumentBuilder.<CommandSource, String>argument("params", StringArgumentType.greedyString())
                .suggests((context, suggestions) -> suggestions.buildFuture());
    }

    @Unique
    @Override
    public void brigo$sendCommands(@NotNull EntityPlayerMP player) {
        CommandSource source = CommandSource.adapt(player);
        RootCommandNode<ISuggestionProvider> clientRoot = brigo$buildClientCommandTree(source);

        player.connection.sendPacket(CommandsPacket.create(clientRoot));
    }

    @Unique
    private RootCommandNode<ISuggestionProvider> brigo$buildClientCommandTree(CommandSource source) {
        Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> nodeMapping = Maps.newHashMap();
        RootCommandNode<ISuggestionProvider> clientRoot = new RootCommandNode<>();

        nodeMapping.put(brigo$dispatcher.getRoot(), clientRoot);

        new CommandTreeConverter(nodeMapping, source)
                .convertChildren(brigo$dispatcher.getRoot(), clientRoot);

        return clientRoot;
    }

    @Unique
    @Override
    public CommandDispatcher<CommandSource> brigo$dispatcher() {
        return brigo$dispatcher;
    }
}