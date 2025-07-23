package dev.xhyrom.brigo.command.serialization.serializers;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import dev.xhyrom.brigo.command.serialization.ArgumentSerializer;
import net.minecraft.network.PacketBuffer;

public class StringArgumentSerializer implements ArgumentSerializer<StringArgumentType> {
    public void serializeToNetwork(StringArgumentType pArgument, PacketBuffer pBuffer) {
        pBuffer.writeEnumValue(pArgument.getType());
    }

    public StringArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
        StringType stringtype = pBuffer.readEnumValue(StringType.class);

        switch (stringtype) {
            case SINGLE_WORD:
                return StringArgumentType.word();

            case QUOTABLE_PHRASE:
                return StringArgumentType.string();

            case GREEDY_PHRASE:
            default:
                return StringArgumentType.greedyString();
        }
    }
}

