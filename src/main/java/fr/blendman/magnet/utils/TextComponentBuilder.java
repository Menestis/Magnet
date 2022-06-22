package fr.blendman.magnet.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * @author Ariloxe
 */
public class TextComponentBuilder {
    private TextComponent textComponent;

    public TextComponentBuilder(String text) {
        this.textComponent = new TextComponent(text);
    }

    public TextComponentBuilder setHoverMessage(String... msg) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String s : msg) {
            sb.append(s + ((i < msg.length) ? "\n" : ""));
            i++;
        }
        this.textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(sb.toString())).create()));
        return this;
    }

    public TextComponentBuilder setClickAction(ClickEvent.Action action, String value) {
        this.textComponent.setClickEvent(new ClickEvent(action, value));
        return this;
    }

    public TextComponent build() {
        return this.textComponent;
    }
}
