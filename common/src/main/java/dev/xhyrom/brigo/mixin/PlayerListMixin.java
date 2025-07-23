package dev.xhyrom.brigo.mixin;

import dev.xhyrom.brigo.accessor.CommandHandlerExtras;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "sendPlayerPermissionLevel", at = @At("HEAD"))
    public void sendPlayerPermissionLevel(EntityPlayerMP player, int permLevel, CallbackInfo ci) {
        ((CommandHandlerExtras) this.server.getCommandManager()).sendCommands(player);
    }
}
