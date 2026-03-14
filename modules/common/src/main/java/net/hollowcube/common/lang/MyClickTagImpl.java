package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.Tag;

import java.util.List;

public class MyClickTagImpl implements Tag, InsertingWithArgs {
    private final ClickEvent.Action action;
    private final String text;

    MyClickTagImpl(ClickEvent.Action action, String text) {
        this.action = action;
        this.text = text;
    }

    @Override
    public Component value(List<Component> args) {
        var replacedText = LanguageProviderV2.replaceInString(this.text, args);
        return Component.text("", Style.style(ClickEvent.clickEvent(action, replacedText)));
    }

//    @Override
//    public int hashCode() {
//        return 31 + Arrays.hashCode(this.styles);
//    }
//
//    @Override
//    public boolean equals(final @Nullable Object other) {
//        if (this == other) return true;
//        if (!(other instanceof MyStylingTagImpl)) return false;
//        final MyStylingTagImpl that = (MyStylingTagImpl) other;
//        return Arrays.equals(this.styles, that.styles);
//    }

}
