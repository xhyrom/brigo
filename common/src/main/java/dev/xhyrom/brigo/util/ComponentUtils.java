package dev.xhyrom.brigo.util;

import com.mojang.brigadier.Message;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class ComponentUtils {
    public static ITextComponent fromMessage(Message message) {
        return message instanceof ITextComponent ? (ITextComponent)message : new TextComponentString(message.getString());
    }
}
