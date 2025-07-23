package dev.xhyrom.brigo.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.xhyrom.brigo.accessor.NetHandlerPlayClientExtras;
import dev.xhyrom.brigo.client.ClientSuggestionProvider;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.command.CommandsPacket;
import dev.xhyrom.brigo.util.SuggestionProviders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketTabComplete;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(NetHandlerPlayClient.class)
public class NetHandlerPlayClientMixin implements NetHandlerPlayClientExtras {
    @Shadow private Minecraft client;

    @Unique
    private ClientSuggestionProvider brigo$suggestionsProvider;
    @Unique
    private CommandDispatcher<ISuggestionProvider> brigo$commands = new CommandDispatcher<>();
    @Unique
    private boolean brigo$entityStatusReceived = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.brigo$suggestionsProvider = new ClientSuggestionProvider((NetHandlerPlayClient) (Object) this, client);
    }

    @Inject(method = "handleTabComplete", at = @At("TAIL"))
    private void handleTabComplete(SPacketTabComplete packetIn, CallbackInfo ci) {
        String command = ClientSuggestionProvider.command;
        String[] matches = packetIn.getMatches();

        int lastSpace = command.lastIndexOf(' ');
        int replaceStart = (lastSpace == -1) ? 0 : lastSpace + 1;
        int replaceEnd = command.length();
        StringRange replacementRange = StringRange.between(replaceStart, replaceEnd);

        List<Suggestion> suggestions = Arrays.stream(matches)
                .map(match -> new Suggestion(replacementRange, match))
                .collect(Collectors.toList());

        this.brigo$suggestionsProvider.completeCustomSuggestions(
                new Suggestions(replacementRange, suggestions)
        );
    }


    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void handleCustomPayload(SPacketCustomPayload packetIn, CallbackInfo ci) {
        if (!packetIn.getChannelName().equals(CommandsPacket.COMMANDS_CHANNEL)) return;

        this.brigo$commands = new CommandDispatcher<>(CommandsPacket.read(packetIn.getBufferData()));
    }

    // Register all commands from vanilla server
    @Inject(method = "handleEntityStatus", at = @At("TAIL"))
    private void handleEntityStatus(SPacketEntityStatus packetIn, CallbackInfo ci) {
        if (!this.brigo$commands.getRoot().getChildren().isEmpty() || brigo$entityStatusReceived) return;

        brigo$entityStatusReceived = true;
        suggestionsProvider().getSuggestionsFromServer().thenAccept(commands -> {
            for (Suggestion suggestion : commands.getList()) {
                brigo$commands.register(
                        LiteralArgumentBuilder.<ISuggestionProvider>literal(suggestion.getText().substring(1))
                                .then(
                                        RequiredArgumentBuilder.<ISuggestionProvider, String>argument("params", StringArgumentType.greedyString())
                                                .suggests(SuggestionProviders.ASK_SERVER)
                                )
                );
            }
        });
    }

    @Unique
    @Override
    public ClientSuggestionProvider suggestionsProvider() {
        return brigo$suggestionsProvider;
    }

    @Unique
    @Override
    public @NotNull CommandDispatcher<ISuggestionProvider> commands() {
        return brigo$commands;
    }
}
