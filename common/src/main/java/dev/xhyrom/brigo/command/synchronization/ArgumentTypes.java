package dev.xhyrom.brigo.command.synchronization;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.xhyrom.brigo.command.synchronization.brigadier.StringArgumentSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public class ArgumentTypes {
    private static final Map<Class<?>, ArgumentTypes.Entry<? >> BY_CLASS = Maps.newHashMap();
    private static final Map<ResourceLocation, ArgumentTypes.Entry<? >> BY_NAME = Maps.newHashMap();

    public static < T extends ArgumentType<? >> void register(String pName, Class<T> pClazz, ArgumentSerializer<T> pSerializer)
    {
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
        register("brigadier:string", StringArgumentType.class, new StringArgumentSerializer());
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ResourceLocation pType)
    {
        return BY_NAME.get(pType);
    }

    @Nullable
    private static ArgumentTypes.Entry<?> get(ArgumentType<?> pType)
    {
        return BY_CLASS.get(pType.getClass());
    }

    public static < T extends ArgumentType<? >> void serialize(PacketBuffer pBuffer, T pType)
    {
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
    public static ArgumentType<?> deserialize(PacketBuffer pBuffer)
    {
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

    private static < T extends ArgumentType<? >> void serializeToJson(JsonObject pJson, T pType)
    {
        ArgumentTypes.Entry<T> entry = (ArgumentTypes.Entry<T>)get(pType);

        if (entry == null)
        {
            pJson.addProperty("type", "unknown");
        }
        else
        {
            pJson.addProperty("type", "argument");
            pJson.addProperty("parser", entry.name.toString());
            JsonObject jsonobject = new JsonObject();
            entry.serializer.serializeToJson(pType, jsonobject);

            if (jsonobject.entrySet().size() > 0)
            {
                pJson.add("properties", jsonobject);
            }
        }
    }

    public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> pDispatcher, CommandNode<S> pNode)
    {
        JsonObject jsonobject = new JsonObject();

        if (pNode instanceof RootCommandNode)
        {
            jsonobject.addProperty("type", "root");
        }
        else if (pNode instanceof LiteralCommandNode)
        {
            jsonobject.addProperty("type", "literal");
        }
        else if (pNode instanceof ArgumentCommandNode)
        {
            serializeToJson(jsonobject, ((ArgumentCommandNode)pNode).getType());
        }
        else
        {
            jsonobject.addProperty("type", "unknown");
        }

        JsonObject jsonobject1 = new JsonObject();

        for (CommandNode<S> commandnode : pNode.getChildren())
        {
            jsonobject1.add(commandnode.getName(), serializeNodeToJson(pDispatcher, commandnode));
        }

        if (jsonobject1.entrySet().size() > 0)
        {
            jsonobject.add("children", jsonobject1);
        }

        if (pNode.getCommand() != null)
        {
            jsonobject.addProperty("executable", true);
        }

        if (pNode.getRedirect() != null)
        {
            Collection<String> collection = pDispatcher.getPath(pNode.getRedirect());

            if (!collection.isEmpty())
            {
                JsonArray jsonarray = new JsonArray();

                for (String s : collection)
                {
                    jsonarray.add(new JsonPrimitive(s));
                }

                jsonobject.add("redirect", jsonarray);
            }
        }

        return jsonobject;
    }

    static class Entry < T extends ArgumentType<? >>
    {
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
