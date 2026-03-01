package net.hollowcube.mapmaker.api.interaction;

import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record InteractionResponse(
    Type type,
    @Nullable String styledText
) {

    public enum Type {
        MESSAGE
    }

    public Component resolveMessage() {
        if (type != Type.MESSAGE)
            throw new IllegalArgumentException("Cannot resolve message for non-message interaction response");
        if (styledText != null)
            return MiniMessage.miniMessage().deserialize(styledText);
        return Component.empty();
    }

}
