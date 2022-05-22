package fr.blendman.magnet.utils;

import fr.blendman.skynet.models.MessageComponent;
import fr.blendman.skynet.models.MessageComponentModifiers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * @author Blendman974
 */
public class ProxyUtils {

    public static Component messageToComponent(List<MessageComponent> message) {
        TextComponent ret = Component.empty();
        if (message == null || message.isEmpty())
            return ret;

        for (MessageComponent c : message) {
            ret = ret.append(messageToComponent(c));
        }
        return ret;
    }

    private static Component messageToComponent(MessageComponent message) {
        TextComponent text = Component.text(message.getText());
        if (message.getColor() != null) {
            if (message.getColor().startsWith("#"))
                text = text.color(TextColor.fromHexString(message.getColor()));
            else
                text = text.color(NamedTextColor.NAMES.value(camelToSnake(message.getColor())));
        }

        if (message.getModifiers() != null) {
            MessageComponentModifiers modifiers = message.getModifiers();
            if (modifiers.getBold() != null && modifiers.getBold())
                text = text.decoration(TextDecoration.BOLD, true);
            if (modifiers.getItalic() != null && modifiers.getItalic())
                text = text.decoration(TextDecoration.ITALIC, true);
            if (modifiers.getObfuscated() != null && modifiers.getObfuscated())
                text = text.decoration(TextDecoration.OBFUSCATED, true);
            if (modifiers.getStrikethrough() != null && modifiers.getStrikethrough())
                text = text.decoration(TextDecoration.STRIKETHROUGH, true);
            if (modifiers.getUnderlined() != null && modifiers.getUnderlined())
                text = text.decoration(TextDecoration.UNDERLINED, true);
        }

        return text;
    }


    public static String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();

        char c = str.charAt(0);
        result.append(Character.toLowerCase(c));

        for (int i = 1; i < str.length(); i++) {

            char ch = str.charAt(i);

            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }


}
