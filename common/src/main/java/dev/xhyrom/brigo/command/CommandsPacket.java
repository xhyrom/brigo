package dev.xhyrom.brigo.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.xhyrom.brigo.BrigoClient;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.util.SuggestionProviders;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class CommandsPacket {
    public static final String COMMANDS_CHANNEL = BrigoClient.MOD_ID + ":commands";

    public static SPacketCustomPayload create(RootCommandNode<ISuggestionProvider> root) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());

        Object2IntMap<CommandNode<ISuggestionProvider>> object2intmap = enumerateNodes(root);
        List<CommandNode<ISuggestionProvider>> list = getNodesInIdOrder(object2intmap);
        writeCollection(buf, list, (p_178810_, p_178811_) ->
        {
            writeNode(p_178810_, p_178811_, object2intmap);
        });
        buf.writeVarInt(object2intmap.get(root));

        return new SPacketCustomPayload(COMMANDS_CHANNEL, buf);
    }

    public static RootCommandNode<ISuggestionProvider> read(PacketBuffer buf) {
        List<Entry> list = readList(buf, CommandsPacket::readNode);
        resolveEntries(list);
        int i = buf.readVarInt();

        return (RootCommandNode)(list.get(i)).node;
    }

    public static  <T, C extends Collection<T>> C readCollection(PacketBuffer buf, IntFunction<C> pCollectionFactory, Function<PacketBuffer, T> pElementReader)
    {
        int i = buf.readVarInt();
        C c = pCollectionFactory.apply(i);

        for (int j = 0; j < i; ++j)
        {
            c.add(pElementReader.apply(buf));
        }

        return c;
    }

    public static <T> void writeCollection(PacketBuffer buf, Collection<T> pCollection, BiConsumer<PacketBuffer, T> pElementWriter)
    {
        buf.writeVarInt(pCollection.size());

        for (T t : pCollection)
        {
            pElementWriter.accept(buf, t);
        }
    }

    public static <T> List<T> readList(PacketBuffer buf, Function<PacketBuffer, T> pElementReader)
    {
        return readCollection(buf, Lists::newArrayListWithCapacity, pElementReader);
    }

    private static void resolveEntries(List<Entry> pEntries)
    {
        List<Entry> list = Lists.newArrayList(pEntries);

        while (!list.isEmpty())
        {
            boolean flag = list.removeIf((p_178816_) ->
            {
                return p_178816_.build(pEntries);
            });

            if (!flag)
            {
                throw new IllegalStateException("Server sent an impossible command tree");
            }
        }
    }

    private static Object2IntMap<CommandNode<ISuggestionProvider>> enumerateNodes(RootCommandNode<ISuggestionProvider> pRootNode)
    {
        Object2IntMap<CommandNode<ISuggestionProvider>> object2intmap = new Object2IntOpenHashMap<>();
        Queue<CommandNode<ISuggestionProvider>> queue = Queues.newArrayDeque();
        queue.add(pRootNode);
        CommandNode<ISuggestionProvider> commandnode;

        while ((commandnode = queue.poll()) != null)
        {
            if (!object2intmap.containsKey(commandnode))
            {
                int i = object2intmap.size();
                object2intmap.put(commandnode, i);
                queue.addAll(commandnode.getChildren());

                if (commandnode.getRedirect() != null)
                {
                    queue.add(commandnode.getRedirect());
                }
            }
        }

        return object2intmap;
    }

    private static List<CommandNode<ISuggestionProvider>> getNodesInIdOrder(Object2IntMap<CommandNode<ISuggestionProvider>> pNodeToIdMap)
    {
        ObjectArrayList<CommandNode<ISuggestionProvider>> objectarraylist = new ObjectArrayList<>(pNodeToIdMap.size());
        objectarraylist.size(pNodeToIdMap.size());

        for (Object2IntMap.Entry<CommandNode<ISuggestionProvider>> entry : pNodeToIdMap.object2IntEntrySet())
        {
            objectarraylist.set(entry.getIntValue(), entry.getKey());
        }

        return objectarraylist;
    }

    private static Entry readNode(PacketBuffer p_131888_)
    {
        byte b0 = p_131888_.readByte();
        int[] aint = p_131888_.readVarIntArray();
        int i = (b0 & 8) != 0 ? p_131888_.readVarInt() : 0;
        ArgumentBuilder < ISuggestionProvider, ? > argumentbuilder = createBuilder(p_131888_, b0);
        return new Entry(argumentbuilder, b0, i, aint);
    }

    @Nullable
    private static ArgumentBuilder < ISuggestionProvider, ? > createBuilder(PacketBuffer pBuffer, byte pFlags)
    {
        int i = pFlags & 3;

        if (i == 2)
        {
            String s = pBuffer.readString(32767);
            ArgumentType<?> argumenttype = null;//ArgumentTypes.deserialize(pBuffer);

            if (argumenttype == null)
            {
                return null;
            }
            else
            {
                RequiredArgumentBuilder < ISuggestionProvider, ? > requiredargumentbuilder = RequiredArgumentBuilder.argument(s, argumenttype);

                if ((pFlags & 16) != 0)
                {
                    requiredargumentbuilder.suggests(SuggestionProviders.get(pBuffer.readResourceLocation()));
                }

                return requiredargumentbuilder;
            }
        }
        else
        {
            return i == 1 ? LiteralArgumentBuilder.literal(pBuffer.readString(32767)) : null;
        }
    }


    private static void writeNode(PacketBuffer pBuffer, CommandNode<ISuggestionProvider> pNode, Map<CommandNode<ISuggestionProvider>, Integer> pNodeIds)
    {
        byte b0 = 0;

        if (pNode.getRedirect() != null)
        {
            b0 = (byte)(b0 | 8);
        }

        if (pNode.getCommand() != null)
        {
            b0 = (byte)(b0 | 4);
        }

        if (pNode instanceof RootCommandNode)
        {
            b0 = (byte)(b0 | 0);
        }
        else if (pNode instanceof ArgumentCommandNode)
        {
            b0 = (byte)(b0 | 2);

            if (((ArgumentCommandNode)pNode).getCustomSuggestions() != null)
            {
                b0 = (byte)(b0 | 16);
            }
        }
        else
        {
            if (!(pNode instanceof LiteralCommandNode))
            {
                throw new UnsupportedOperationException("Unknown node type " + pNode);
            }

            b0 = (byte)(b0 | 1);
        }

        pBuffer.writeByte(b0);
        pBuffer.writeVarInt(pNode.getChildren().size());

        for (CommandNode<ISuggestionProvider> commandnode : pNode.getChildren())
        {
            pBuffer.writeVarInt(pNodeIds.get(commandnode));
        }

        if (pNode.getRedirect() != null)
        {
            pBuffer.writeVarInt(pNodeIds.get(pNode.getRedirect()));
        }

        if (pNode instanceof ArgumentCommandNode)
        {
            ArgumentCommandNode < ISuggestionProvider, ? > argumentcommandnode = (ArgumentCommandNode)pNode;
            pBuffer.writeString(argumentcommandnode.getName());
            //ArgumentTypes.serialize(pBuffer, argumentcommandnode.getType());

            if (argumentcommandnode.getCustomSuggestions() != null)
            {
                pBuffer.writeResourceLocation(SuggestionProviders.getId(argumentcommandnode.getCustomSuggestions()));
            }
        }
        else if (pNode instanceof LiteralCommandNode)
        {
            pBuffer.writeString(((LiteralCommandNode)pNode).getLiteral());
        }
    }

    static class Entry
    {
        @Nullable
        private final ArgumentBuilder< ISuggestionProvider, ? > builder;
        private final byte flags;
        private final int redirect;
        private final int[] children;
        @Nullable
        CommandNode<ISuggestionProvider> node;

        Entry(@Nullable ArgumentBuilder < ISuggestionProvider, ? > pBuilder, byte pFlags, int pRedirect, int[] pChildren)
        {
            this.builder = pBuilder;
            this.flags = pFlags;
            this.redirect = pRedirect;
            this.children = pChildren;
        }

        public boolean build(List<Entry> pEntries)
        {
            if (this.node == null)
            {
                if (this.builder == null)
                {
                    this.node = new RootCommandNode<>();
                }
                else
                {
                    if ((this.flags & 8) != 0)
                    {
                        if ((pEntries.get(this.redirect)).node == null)
                        {
                            return false;
                        }

                        this.builder.redirect((pEntries.get(this.redirect)).node);
                    }

                    if ((this.flags & 4) != 0)
                    {
                        this.builder.executes((p_131906_) ->
                        {
                            return 0;
                        });
                    }

                    this.node = this.builder.build();
                }
            }

            for (int i : this.children)
            {
                if ((pEntries.get(i)).node == null)
                {
                    return false;
                }
            }

            for (int j : this.children)
            {
                CommandNode<ISuggestionProvider> commandnode = (pEntries.get(j)).node;

                if (!(commandnode instanceof RootCommandNode))
                {
                    this.node.addChild(commandnode);
                }
            }

            return true;
        }
    }
}
