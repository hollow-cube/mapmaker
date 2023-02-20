package net.hollowcube.terraform.mask.script;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MaskParseException extends Exception {
    private static final int BEGIN_OFFSET = 10;

    private final int start, end;

    public MaskParseException(@NotNull String message) {
        this(-1, -1, message);
    }

    public MaskParseException(int start, int end, @NotNull String message) {
        super(message);
        this.start = start;
        this.end = end;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public @NotNull List<Component> toFriendlyMessage(@NotNull String source) {
        if (start == -1 || end == -1)
            return List.of(Component.text(getMessage()));

        var textSource = source;
        int textStart = start, textEnd = end;
        if (start > BEGIN_OFFSET) {
            textSource = source.substring(start - BEGIN_OFFSET);
            textStart = BEGIN_OFFSET;
            textEnd = Math.max(BEGIN_OFFSET, end - (start - BEGIN_OFFSET));
        }

        var offsetStr = "";
        // Add the source code line
        var sourceComp = Component.text();
        if (start != textStart) {
            sourceComp.append(Component.text("..", NamedTextColor.GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text(source.substring(0, start - BEGIN_OFFSET)))));
            offsetStr += "..";
        }
        var prefix = textSource.substring(0, textStart);
        sourceComp.append(Component.text(prefix, NamedTextColor.WHITE));
        offsetStr += prefix;
        sourceComp.append(Component.text(textSource.substring(textStart, textEnd), NamedTextColor.RED));
        sourceComp.append(Component.text(textSource.substring(textEnd), NamedTextColor.WHITE));

        // Add error message line
        var errorComp = Component.text();
        errorComp.append(Component.text(FontUtil.computeOffset(FontUtil.measureText(offsetStr))));
        errorComp.append(Component.text("^".repeat(Math.max(1, textEnd - textStart)), NamedTextColor.RED));
        errorComp.append(Component.text(" " + getMessage(), NamedTextColor.GRAY));

        return List.of(sourceComp.build(), errorComp.build());
    }
}
