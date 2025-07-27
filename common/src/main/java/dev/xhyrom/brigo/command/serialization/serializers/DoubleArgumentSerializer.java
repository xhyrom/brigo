package dev.xhyrom.brigo.command.serialization.serializers;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.xhyrom.brigo.command.serialization.ArgumentSerializer;
import net.minecraft.network.PacketBuffer;

public class DoubleArgumentSerializer implements ArgumentSerializer<DoubleArgumentType> {
    public void serializeToNetwork(DoubleArgumentType pArgument, PacketBuffer pBuffer) {
        boolean flag = pArgument.getMinimum() != -Double.MAX_VALUE;
        boolean flag1 = pArgument.getMaximum() != Double.MAX_VALUE;
        pBuffer.writeByte(BrigadierArgumentSerializers.createNumberFlags(flag, flag1));

        if (flag) {
            pBuffer.writeDouble(pArgument.getMinimum());
        }

        if (flag1) {
            pBuffer.writeDouble(pArgument.getMaximum());
        }
    }

    public DoubleArgumentType deserializeFromNetwork(PacketBuffer pBuffer) {
        byte b0 = pBuffer.readByte();
        double d0 = BrigadierArgumentSerializers.numberHasMin(b0) ? pBuffer.readDouble() : -Double.MAX_VALUE;
        double d1 = BrigadierArgumentSerializers.numberHasMax(b0) ? pBuffer.readDouble() : Double.MAX_VALUE;
        return DoubleArgumentType.doubleArg(d0, d1);
    }
}
