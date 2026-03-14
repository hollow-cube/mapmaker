package net.hollowcube.mapmaker.chat.components;

import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.HashMap;
import java.util.Map;

@RuntimeGson
public record MessageComponent(
    Component text,
    Map<String, Component> extra,
    boolean ping
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TextComponent.Builder text = Component.text();
        private final Map<String, Component> errors = new HashMap<>();
        private boolean ping = false;

        public Builder append(Component component) {
            this.text.append(component);
            return this;
        }

        public Builder appendError(String id, Component component) {
            this.errors.put(id, component);
            return this;
        }

        public Builder ping(boolean ping) {
            this.ping |= ping;
            return this;
        }

        public MessageComponent build() {
            return new MessageComponent(this.text.build(), this.errors, this.ping);
        }
    }
}
