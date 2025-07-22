package dev.xhyrom.brigo.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.xhyrom.brigo.accessor.NetHandlerPlayClientExtras;
import dev.xhyrom.brigo.client.ClientSuggestionProvider;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.command.CommandsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketTabComplete;
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
    private ClientSuggestionProvider suggestionsProvider;
    @Unique
    private CommandDispatcher<ISuggestionProvider> commands = new CommandDispatcher<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.suggestionsProvider = new ClientSuggestionProvider((NetHandlerPlayClient) (Object) this, client);
    }

    @Inject(method = "handleTabComplete", at = @At("TAIL"))
    private void handleTabComplete(SPacketTabComplete packetIn, CallbackInfo ci) {
        String[] strings = packetIn.getMatches();
        List<Suggestion> suggestions = Arrays.stream(strings)
                .map(str -> new Suggestion(StringRange.at(0), str))
                .collect(Collectors.toList());

        this.suggestionsProvider.completeCustomSuggestions(0, new Suggestions(StringRange.at(0), suggestions));
    }

    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void handleCustomPayload(SPacketCustomPayload packetIn, CallbackInfo ci) {
        System.out.println("Received custom payload: " + packetIn.getChannelName());
        if (!packetIn.getChannelName().equals(CommandsPacket.COMMANDS_CHANNEL)) return;

        this.commands = new CommandDispatcher<>(CommandsPacket.read(packetIn.getBufferData()));
    }

    @Unique
    @Override
    public ClientSuggestionProvider suggestionsProvider() {
        return suggestionsProvider;
    }

    @Unique
    @Override
    public CommandDispatcher<ISuggestionProvider> commands() {
        return commands;
    }
}
