package dev.xhyrom.brigo.command.serialization.serializers;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.xhyrom.brigo.command.serialization.ArgumentSerializer;
import net.minecraft.network.PacketBuffer;

public class IntegerArgumentSerializer implements ArgumentSerializer<IntegerArgumentType> {
    public void serializeToNetwork(IntegerArgumentType pArgument, PacketBuffer pBuffer) {
        boolean flag = pArgument.getMinimum() != Integer.MIN_VALUE;
        boolean flag1 = pArgument.getMaximum() != Integer.MAX_VALUE;
        pBuffer.writeByte(BrigadierArgumentSerializers.createNumberFlags(flag, flag1));

        if (flag) {
            pBuffer.writeInt(pArgument.getMinimum());
        }

        if (flag1) {
            pBuffer.writeInt(pArgument.getMaximum());
        }
    }

    public IntegerArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
        byte b0 = pBuffer.readByte();
        int i = BrigadierArgumentSerializers.numberHasMin(b0) ? pBuffer.readInt() : Integer.MIN_VALUE;
        int j = BrigadierArgumentSerializers.numberHasMax(b0) ? pBuffer.readInt() : Integer.MAX_VALUE;
        return IntegerArgumentType.integer(i, j);
    }
}
