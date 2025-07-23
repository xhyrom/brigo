package dev.xhyrom.brigo.accessor;

import com.mojang.brigadier.CommandDispatcher;
import dev.xhyrom.brigo.command.CommandSource;
import net.minecraft.entity.player.EntityPlayerMP;
import org.jetbrains.annotations.NotNull;

public interface CommandHandlerExtras {
    void brigo$sendCommands(@NotNull EntityPlayerMP player);
    CommandDispatcher<CommandSource> brigo$dispatcher();
}
