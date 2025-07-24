package dev.xhyrom.brigo.client.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import dev.xhyrom.brigo.command.serialization.ArgumentTypes;
import dev.xhyrom.brigo.util.SuggestionProviders;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

public class CommandsPacket {
    public static final String COMMANDS_CHANNEL = BrigoClient.MOD_ID + ":commands";

    private static final byte NODE_TYPE_ROOT = 0;
    private static final byte NODE_TYPE_LITERAL = 1;
    private static final byte NODE_TYPE_ARGUMENT = 2;

    private static final byte FLAG_HAS_REDIRECT = 8;
    private static final byte FLAG_HAS_COMMAND = 4;
    private static final byte FLAG_HAS_SUGGESTIONS = 16;

    public static SPacketCustomPayload create(RootCommandNode<ISuggestionProvider> root) {
        return new PacketBuilder()
                .withRoot(root)
                .build();
    }

    public static RootCommandNode<ISuggestionProvider> read(PacketBuffer buffer) {
        return new PacketReader(buffer)
                .readCommands();
    }

    private static class PacketBuilder {
        private RootCommandNode<ISuggestionProvider> root;
        private final Map<CommandNode<ISuggestionProvider>, Integer> nodeIdMap = Maps.newHashMap();
        private final List<CommandNode<ISuggestionProvider>> nodeList = Lists.newArrayList();

        public PacketBuilder withRoot(RootCommandNode<ISuggestionProvider> root) {
            this.root = root;
            return this;
        }

        public SPacketCustomPayload build() {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());

            this.enumerateNodes()
                    .writeNodes(buffer)
                    .writeRootIndex(buffer);

            return new SPacketCustomPayload(COMMANDS_CHANNEL, buffer);
        }

        private PacketBuilder enumerateNodes() {
            Object2IntMap<CommandNode<ISuggestionProvider>> enumeration = enumerateNodesRecursively(root);
            this.nodeIdMap.putAll(enumeration);
            this.nodeList.addAll(getNodesInIdOrder(enumeration));
            return this;
        }

        private PacketBuilder writeNodes(PacketBuffer buffer) {
            writeCollection(buffer, nodeList, this::writeNode);
            return this;
        }

        private PacketBuilder writeRootIndex(PacketBuffer buffer) {
            buffer.writeVarInt(nodeIdMap.get(root));
            return this;
        }

        private void writeNode(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) {
            byte flags = calculateNodeFlags(node);

            buffer.writeByte(flags);
            writeChildrenIds(buffer, node);
            writeRedirectId(buffer, node);
            writeNodeData(buffer, node);
        }

        private byte calculateNodeFlags(CommandNode<ISuggestionProvider> node) {
            byte flags = getNodeType(node);

            if (node.getRedirect() != null) flags |= FLAG_HAS_REDIRECT;
            if (node.getCommand() != null) flags |= FLAG_HAS_COMMAND;
            if (node instanceof ArgumentCommandNode &&
                    ((ArgumentCommandNode<ISuggestionProvider, ?>) node).getCustomSuggestions() != null) {
                flags |= FLAG_HAS_SUGGESTIONS;
            }

            return flags;
        }

        private byte getNodeType(CommandNode<ISuggestionProvider> node) {
            if (node instanceof RootCommandNode) return NODE_TYPE_ROOT;
            if (node instanceof LiteralCommandNode) return NODE_TYPE_LITERAL;
            if (node instanceof ArgumentCommandNode) return NODE_TYPE_ARGUMENT;
            throw new UnsupportedOperationException("Unknown node type: " + node.getClass());
        }

        private void writeChildrenIds(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) {
            buffer.writeVarInt(node.getChildren().size());
            for (CommandNode<ISuggestionProvider> child : node.getChildren()) {
                buffer.writeVarInt(nodeIdMap.get(child));
            }
        }

        private void writeRedirectId(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) {
            if (node.getRedirect() != null) {
                buffer.writeVarInt(nodeIdMap.get(node.getRedirect()));
            }
        }

        private void writeNodeData(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) {
            if (node instanceof ArgumentCommandNode) {
                writeArgumentNode(buffer, (ArgumentCommandNode<ISuggestionProvider, ?>) node);
            } else if (node instanceof LiteralCommandNode) {
                writeLiteralNode(buffer, (LiteralCommandNode<ISuggestionProvider>) node);
            }
        }

        private void writeArgumentNode(PacketBuffer buffer, ArgumentCommandNode<ISuggestionProvider, ?> node) {
            buffer.writeString(node.getName());
            ArgumentTypes.serialize(buffer, node.getType());

            if (node.getCustomSuggestions() != null) {
                buffer.writeResourceLocation(SuggestionProviders.getId(node.getCustomSuggestions()));
            }
        }

        private void writeLiteralNode(PacketBuffer buffer, LiteralCommandNode<ISuggestionProvider> node) {
            buffer.writeString(node.getLiteral());
        }
    }

    private static class PacketReader {
        private final PacketBuffer buffer;
        private final List<NodeEntry> entries = Lists.newArrayList();
        private int rootIndex;

        public PacketReader(PacketBuffer buffer) {
            this.buffer = buffer;
        }

        public RootCommandNode<ISuggestionProvider> readCommands() {
            return this.readEntries()
                    .readRootIndex()
                    .resolveNodes()
                    .getRootNode();
        }

        private PacketReader readEntries() {
            int entryCount = buffer.readVarInt();
            for (int i = 0; i < entryCount; i++) {
                entries.add(readNodeEntry());
            }
            return this;
        }

        private PacketReader readRootIndex() {
            rootIndex = buffer.readVarInt();
            return this;
        }

        private PacketReader resolveNodes() {
            List<NodeEntry> unresolved = Lists.newArrayList(entries);

            while (!unresolved.isEmpty()) {
                boolean progress = unresolved.removeIf(entry -> entry.tryBuild(entries));
                if (!progress) {
                    throw new IllegalStateException("Server sent an impossible command tree");
                }
            }
            return this;
        }

        private RootCommandNode<ISuggestionProvider> getRootNode() {
            return (RootCommandNode<ISuggestionProvider>) entries.get(rootIndex).getNode();
        }

        private NodeEntry readNodeEntry() {
            byte flags = buffer.readByte();
            int[] childIndices = buffer.readVarIntArray();
            int redirectIndex = (flags & FLAG_HAS_REDIRECT) != 0 ? buffer.readVarInt() : -1;

            ArgumentBuilder<ISuggestionProvider, ?> builder = createBuilder(flags);

            return new NodeEntry(builder, flags, redirectIndex, childIndices);
        }

        @Nullable
        private ArgumentBuilder<ISuggestionProvider, ?> createBuilder(byte flags) {
            int nodeType = flags & 3;

            switch (nodeType) {
                case NODE_TYPE_LITERAL:
                    return LiteralArgumentBuilder.literal(buffer.readString(32767));

                case NODE_TYPE_ARGUMENT:
                    return createArgumentBuilder(flags);

                default:
                    return null; // Root node
            }
        }

        @Nullable
        private RequiredArgumentBuilder<ISuggestionProvider, ?> createArgumentBuilder(byte flags) {
            String name = buffer.readString(32767);
            ArgumentType<?> argumentType = ArgumentTypes.deserialize(buffer);

            if (argumentType == null) return null;

            RequiredArgumentBuilder<ISuggestionProvider, ?> builder =
                    RequiredArgumentBuilder.argument(name, argumentType);

            if ((flags & FLAG_HAS_SUGGESTIONS) != 0) {
                builder.suggests(SuggestionProviders.get(buffer.readResourceLocation()));
            }

            return builder;
        }
    }

    private static class NodeEntry {
        @Nullable private final ArgumentBuilder<ISuggestionProvider, ?> builder;
        private final byte flags;
        private final int redirectIndex;
        private final int[] childIndices;
        @Nullable private CommandNode<ISuggestionProvider> node;

        NodeEntry(@Nullable ArgumentBuilder<ISuggestionProvider, ?> builder,
                  byte flags, int redirectIndex, int[] childIndices) {
            this.builder = builder;
            this.flags = flags;
            this.redirectIndex = redirectIndex;
            this.childIndices = childIndices;
        }

        boolean tryBuild(List<NodeEntry> allEntries) {
            if (node != null) return true;

            if (redirectIndex >= 0 && allEntries.get(redirectIndex).node == null) {
                return false;
            }

            for (int childIndex : childIndices) {
                if (allEntries.get(childIndex).node == null) {
                    return false;
                }
            }

            if (redirectIndex >= 0 && builder != null) {
                builder.redirect(allEntries.get(redirectIndex).node);
            }

            if (builder == null) {
                node = new RootCommandNode<>();
            } else {
                if ((flags & FLAG_HAS_COMMAND) != 0) {
                    builder.executes(context -> 0);
                }
                node = builder.build();
            }

            for (int childIndex : childIndices) {
                CommandNode<ISuggestionProvider> childNode = allEntries.get(childIndex).node;
                if (!(childNode instanceof RootCommandNode)) {
                    node.addChild(childNode);
                }
            }

            return true;
        }

        private NodeEntry buildNode() {
            if (builder == null) {
                node = new RootCommandNode<>();
            } else {
                if ((flags & FLAG_HAS_COMMAND) != 0) {
                    builder.executes(context -> 0);
                }
                node = builder.build();
            }
            return this;
        }

        private boolean applyRedirect(List<NodeEntry> allEntries) {
            if (redirectIndex >= 0) {
                NodeEntry redirectEntry = allEntries.get(redirectIndex);
                if (redirectEntry.node == null) return false;

                if (builder != null) builder.redirect(redirectEntry.node);
            }

            return true;
        }

        private NodeEntry addChildren(List<NodeEntry> allEntries) {
            for (int childIndex : childIndices) {
                if (allEntries.get(childIndex).node == null) {
                    return this;
                }
            }

            for (int childIndex : childIndices) {
                CommandNode<ISuggestionProvider> childNode = allEntries.get(childIndex).node;
                if (!(childNode instanceof RootCommandNode)) {
                    node.addChild(childNode);
                }
            }

            return this;
        }

        CommandNode<ISuggestionProvider> getNode() {
            return node;
        }
    }

    // Utility methods
    private static Object2IntMap<CommandNode<ISuggestionProvider>> enumerateNodesRecursively(
            RootCommandNode<ISuggestionProvider> root) {
        Object2IntMap<CommandNode<ISuggestionProvider>> nodeMap = new Object2IntOpenHashMap<>();
        Queue<CommandNode<ISuggestionProvider>> queue = Queues.newArrayDeque();
        queue.add(root);

        CommandNode<ISuggestionProvider> current;
        while ((current = queue.poll()) != null) {
            if (!nodeMap.containsKey(current)) {
                nodeMap.put(current, nodeMap.size());
                queue.addAll(current.getChildren());

                if (current.getRedirect() != null) {
                    queue.add(current.getRedirect());
                }
            }
        }

        return nodeMap;
    }

    private static List<CommandNode<ISuggestionProvider>> getNodesInIdOrder(
            Object2IntMap<CommandNode<ISuggestionProvider>> nodeMap) {
        CommandNode<ISuggestionProvider>[] nodes = new CommandNode[nodeMap.size()];

        nodeMap.object2IntEntrySet().forEach(entry ->
                nodes[entry.getIntValue()] = entry.getKey()
        );

        return Arrays.asList(nodes);
    }

    private static <T> void writeCollection(PacketBuffer buffer, Collection<T> collection,
                                            BiConsumer<PacketBuffer, T> writer) {
        buffer.writeVarInt(collection.size());
        collection.forEach(item -> writer.accept(buffer, item));
    }
}