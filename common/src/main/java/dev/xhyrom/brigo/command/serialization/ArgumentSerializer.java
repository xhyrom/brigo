package dev.xhyrom.brigo.command.serialization;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.network.PacketBuffer;

public interface ArgumentSerializer < T extends ArgumentType<? >> {
    void serializeToNetwork(T pArgument, PacketBuffer pBuffer);
    T deserializeFromNetwork(PacketBuffer pBuffer);
}
