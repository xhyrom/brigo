package dev.xhyrom.brigo.command.serialization.serializers;

import com.mojang.brigadier.arguments.LongArgumentType;
import dev.xhyrom.brigo.command.serialization.ArgumentSerializer;
import net.minecraft.network.PacketBuffer;

public class LongArgumentSerializer implements ArgumentSerializer<LongArgumentType> {
    public void serializeToNetwork(LongArgumentType pArgument, PacketBuffer pBuffer)
    {
        boolean flag = pArgument.getMinimum() != Long.MIN_VALUE;
        boolean flag1 = pArgument.getMaximum() != Long.MAX_VALUE;
        pBuffer.writeByte(BrigadierArgumentSerializers.createNumberFlags(flag, flag1));

        if (flag)
        {
            pBuffer.writeLong(pArgument.getMinimum());
        }

        if (flag1)
        {
            pBuffer.writeLong(pArgument.getMaximum());
        }
    }

    public LongArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
        byte b0 = pBuffer.readByte();
        long i = BrigadierArgumentSerializers.numberHasMin(b0) ? pBuffer.readLong() : Long.MIN_VALUE;
        long j = BrigadierArgumentSerializers.numberHasMax(b0) ? pBuffer.readLong() : Long.MAX_VALUE;
        return LongArgumentType.longArg(i, j);
    }
}
