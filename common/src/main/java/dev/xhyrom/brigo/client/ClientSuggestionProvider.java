package dev.xhyrom.brigo.client;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
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

    public Collection<String> getTargetedEntity() {
        return this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == Type.ENTITY
                ? Collections.singleton(this.mc.objectMouseOver.entityHit.getCachedUniqueIdString())
                : Collections.emptyList();
    }

    public Collection<String> getTeamNames() {
        return this.connection.world.getScoreboard().getTeamNames();
    }

    public Collection<ResourceLocation> getSoundResourceLocations() {
        return this.mc.getSoundHandler().soundRegistry.getKeys();
    }

    public Collection<ResourceLocation> getRecipeResourceLocations() {
        return CraftingManager.REGISTRY.getKeys();
    }

    public boolean hasPermissionLevel(int i) {
        EntityPlayerSP entityPlayerSP = this.mc.player;
        return entityPlayerSP != null ? entityPlayerSP.canUseCommand(i, "") : i == 0;
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

    private static String prettyPrint(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    private static String prettyPrint(int i) {
        return Integer.toString(i);
    }

    public Collection<ISuggestionProvider.Coordinates> getCoordinates(boolean allowFloatCoords) {
        if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == Type.BLOCK) {
            if (allowFloatCoords) {
                Vec3d vec3d = this.mc.objectMouseOver.hitVec;
                return Collections.singleton(new ISuggestionProvider.Coordinates(prettyPrint(vec3d.x), prettyPrint(vec3d.y), prettyPrint(vec3d.z)));
            } else {
                BlockPos blockPos = this.mc.objectMouseOver.getBlockPos();
                return Collections.singleton(new ISuggestionProvider.Coordinates(prettyPrint(blockPos.getX()), prettyPrint(blockPos.getY()), prettyPrint(blockPos.getZ())));
            }
        } else {
            return Collections.singleton(Coordinates.DEFAULT_GLOBAL);
        }
    }

    public void completeCustomSuggestions(Suggestions suggestions) {
        if (this.pendingSuggestionsFuture == null) return;

        this.pendingSuggestionsFuture.complete(suggestions);
        this.pendingSuggestionsFuture = null;
    }
}

