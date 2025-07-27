package dev.xhyrom.brigo.command.serialization.serializers;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.xhyrom.brigo.command.serialization.ArgumentSerializer;
import net.minecraft.network.PacketBuffer;

import java.util.function.Supplier;

public class EmptyArgumentSerializer<T extends ArgumentType<?>> implements ArgumentSerializer<T> {
    private final Supplier<T> constructor;

    public EmptyArgumentSerializer(Supplier<T> pConstructor) {
        this.constructor = pConstructor;
    }

    public void serializeToNetwork(T pArgument, PacketBuffer pBuffer) {}

    public T deserializeFromNetwork(PacketBuffer pBuffer) {
        return this.constructor.get();
    }
}