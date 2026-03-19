package net.hollowcube.mapmaker.api.interaction;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RuntimeGson
public record InteractionResponse(
    Type type,
    @Nullable String styledText,
    @Nullable String translationKey,
    @Nullable List<String> translationArgs
) {

    public enum Type {
        MESSAGE
    }

    public Component resolveMessage() {
        if (type != Type.MESSAGE)
            throw new IllegalArgumentException("Cannot resolve message for non-message interaction response");
        if (styledText != null)
            return MiniMessage.miniMessage().deserialize(styledText);
        if (translationKey != null) {
            var args = OpUtils.<List<String>, List<String>, List<String>>or(translationArgs, List::of)
                .stream().map(MiniMessage.miniMessage()::deserialize)
                .toList();
            return LanguageProviderV2.translateMultiMerged(translationKey, args);
        }
        return Component.empty();
    }

}
