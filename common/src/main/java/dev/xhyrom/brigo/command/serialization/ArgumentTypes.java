package dev.xhyrom.brigo.command.serialization;

import com.google.common.collect.Maps;
import com.mojang.brigadier.arguments.*;
import dev.xhyrom.brigo.command.serialization.serializers.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ArgumentTypes {
    private static final Map<Class<?>, ArgumentTypes.Entry<? >> BY_CLASS = Maps.newHashMap();
    private static final Map<ResourceLocation, ArgumentTypes.Entry<? >> BY_NAME = Maps.newHashMap();

    public static < T extends ArgumentType<? >> void register(String pName, Class<T> pClazz, ArgumentSerializer<T> pSerializer) {
        ResourceLocation resourcelocation = new ResourceLocation(pName);

        if (BY_CLASS.containsKey(pClazz))
        {
            throw new IllegalArgumentException("Class " + pClazz.getName() + " already has a serializer!");
        }
        else if (BY_NAME.containsKey(resourcelocation))
        {
            throw new IllegalArgumentException("'" + resourcelocation + "' is already a registered serializer!");
        }
        else
        {
            ArgumentTypes.Entry<T> entry = new ArgumentTypes.Entry<>(pClazz, pSerializer, resourcelocation);
            BY_CLASS.put(pClazz, entry);
            BY_NAME.put(resourcelocation, entry);
        }
    }

    public static void init() {
        register("brigadier:bool", BoolArgumentType.class, new EmptyArgumentSerializer<>(BoolArgumentType::bool));
        register("brigadier:float", FloatArgumentType.class, new FloatArgumentSerializer());
        register("brigadier:double", DoubleArgumentType.class, new DoubleArgumentSerializer());
        register("brigadier:integer", IntegerArgumentType.class, new IntegerArgumentSerializer());
        register("brigadier:long", LongArgumentType.class, new LongArgumentSerializer());
        register("brigadier:string", StringArgumentType.class, new StringArgumentSerializer());
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ResourceLocation pType) {
        return BY_NAME.get(pType);
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ArgumentType<?> pType) {
        return BY_CLASS.get(pType.getClass());
    }

    public static < T extends ArgumentType<? >> void serialize(PacketBuffer pBuffer, T pType) {
        ArgumentTypes.Entry<T> entry = (ArgumentTypes.Entry<T>)get(pType);

        if (entry == null)
        {
            pBuffer.writeResourceLocation(new ResourceLocation(""));
        }
        else
        {
            pBuffer.writeResourceLocation(entry.name);
            entry.serializer.serializeToNetwork(pType, pBuffer);
        }
    }

    @Nullable
    public static ArgumentType<?> deserialize(PacketBuffer pBuffer) {
        ResourceLocation resourcelocation = pBuffer.readResourceLocation();
        ArgumentTypes.Entry<?> entry = get(resourcelocation);

        if (entry == null)
        {
            return null;
        }
        else
        {
            return entry.serializer.deserializeFromNetwork(pBuffer);
        }
    }

    static class Entry < T extends ArgumentType<? >> {
        public final Class<T> clazz;
        public final ArgumentSerializer<T> serializer;
        public final ResourceLocation name;

        Entry(Class<T> pClazz, ArgumentSerializer<T> pSerializer, ResourceLocation pName)
        {
            this.clazz = pClazz;
            this.serializer = pSerializer;
            this.name = pName;
        }
    }
}
