package dev.xhyrom.brigo.command.serialization.serializers;

public class BrigadierArgumentSerializers {
    public static byte createNumberFlags(boolean min, boolean max) {
        byte b0 = 0;

        if (min)
        {
            b0 = (byte)(b0 | 1);
        }

        if (max)
        {
            b0 = (byte)(b0 | 2);
        }

        return b0;
    }

    public static boolean numberHasMin(byte flags) {
        return (flags & 1) != 0;
    }

    public static boolean numberHasMax(byte flags) {
        return (flags & 2) != 0;
    }
}
