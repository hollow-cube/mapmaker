package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import static net.hollowcube.mapmaker.dev.element.PropHelper.getInt;

public class TextNode extends Node {

    private String value = "";

    private int xOffset = 0;
    private int yOffset = 0;

    @Override
    public @NotNull TextNode readProps(@NotNull Value props, @NotNull Value[] children) {
        super.readProps(props, children);

        // TODO: this is not a sufficient way to collect the string here.
        this.value = Arrays.stream(children)
                .map(Value::asString)
                .collect(Collectors.joining());

        this.xOffset = getInt(props, "x", 0);
        this.yOffset = getInt(props, "y", 0);

        return this;
    }

    @Override
    public void render(@NotNull RenderContext context) {
        context.drawText(this.value, xOffset, yOffset);
    }
}
