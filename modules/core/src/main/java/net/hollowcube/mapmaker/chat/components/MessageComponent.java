package net.hollowcube.mapmaker.chat.components;

import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@RuntimeGson
public record MessageComponent(
        @NotNull Component text,
        @NotNull Map<String, Component> extra,
        boolean ping
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TextComponent.Builder text = Component.text();
        private final Map<String, Component> errors = new HashMap<>();
        private boolean ping = false;

        public Builder append(@NotNull Component component) {
            this.text.append(component);
            return this;
        }

        public Builder appendError(@NotNull String id, @NotNull Component component) {
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
