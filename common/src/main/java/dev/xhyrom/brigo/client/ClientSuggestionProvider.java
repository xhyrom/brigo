package dev.xhyrom.brigo.client;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class ClientSuggestionProvider implements ISuggestionProvider {
    private final NetHandlerPlayClient connection;
    private final Minecraft mc;
    private @Nullable CompletableFuture<Suggestions> pendingSuggestionsFuture;

    public static String command;

    public ClientSuggestionProvider(NetHandlerPlayClient connection, Minecraft mc) {
        this.connection = connection;
        this.mc = mc;
    }

    public Collection<String> getPlayerNames() {
        List<String> list = Lists.newArrayList();

        for(NetworkPlayerInfo networkPlayerInfo : this.connection.getPlayerInfoMap()) {
            list.add(networkPlayerInfo.getGameProfile().getName());
        }

        return list;
    }

    public Collection<ResourceLocation> getSoundResourceLocations() {
        return this.mc.getSoundHandler().soundRegistry.getKeys();
    }

    public Collection<ResourceLocation> getRecipeResourceLocations() {
        return CraftingManager.REGISTRY.getKeys();
    }

    public CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context, SuggestionsBuilder suggestionsBuilder) {
        if (this.pendingSuggestionsFuture != null) {
            this.pendingSuggestionsFuture.cancel(false);
        }

        this.pendingSuggestionsFuture = new CompletableFuture<>();

        command = context.getInput();
        this.connection.sendPacket(new CPacketTabComplete(context.getInput(), null, false));

        return this.pendingSuggestionsFuture;
    }

    public CompletableFuture<Suggestions> getSuggestionsFromServer() {
        if (this.pendingSuggestionsFuture != null) {
            this.pendingSuggestionsFuture.cancel(false);
        }

        this.pendingSuggestionsFuture = new CompletableFuture<>();

        command = "/";
        this.connection.sendPacket(new CPacketTabComplete("/", null, false));

        return this.pendingSuggestionsFuture;
    }

    public void completeCustomSuggestions(Suggestions suggestions) {
        if (this.pendingSuggestionsFuture == null) return;

        this.pendingSuggestionsFuture.complete(suggestions);
        this.pendingSuggestionsFuture = null;
    }
}

