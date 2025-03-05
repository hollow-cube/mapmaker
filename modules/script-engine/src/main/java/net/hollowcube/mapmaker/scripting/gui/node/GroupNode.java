package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.MenuBuilder;
import net.hollowcube.mapmaker.scripting.gui.util.ClickType;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GroupNode extends Node {
    protected final List<Node> children = new ArrayList<>();

    private enum Layout {ROW, COLUMN}

    private Layout layout = Layout.COLUMN;

    public GroupNode() {
        this("group");
    }

    protected GroupNode(@NotNull String type) {
        super(type);
    }

    @Override
    public int width() {
        if (this.isHidden()) return 0;
        if (this.slotWidth != 0) return this.slotWidth;
        if (this.children.isEmpty()) return 0;
        if (this.children.size() == 1) return this.children.getFirst().width();

        int width = 0;
        for (var child : children) {
            if (child.isHidden()) continue;

            width = switch (this.layout) {
                case ROW -> width + child.width();
                case COLUMN -> Math.max(width, child.width());
            };
        }
        return width;
    }

    @Override
    public int height() {
        if (this.isHidden()) return 0;
        if (this.slotHeight != 0) return this.slotHeight;
        if (this.children.isEmpty()) return 0;
        if (this.children.size() == 1) return this.children.getFirst().height();

        int height = 0;
        for (var child : children) {
            if (child.isHidden()) continue;

            height = switch (this.layout) {
                case ROW -> Math.max(height, child.height());
                case COLUMN -> height + child.height();
            };
        }
        return height;
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean changed = super.updateFromProps(props);

        if (props.hasMember("layout")) {
            this.layout = Layout.valueOf(props.getMember("layout").asString().toUpperCase(Locale.ROOT));
            changed = true;
        }

        return changed;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        if (this.isHidden()) return;
        if (this.children.isEmpty()) return;
        if (this.children.size() == 1) {
            this.children.getFirst().build(builder);
            return;
        }

        // todo it would be nice to be able to make errors like
        //  group.group.button.tooltip > "tooltip" is not valid for XYZ

        int mark = builder.mark();
        for (var child : children) {
            if (child.isHidden()) continue;

            int cWidth = child.width(), cHeight = child.height();
            int before = builder.mark();
            if (!child.isBackground()) builder.boundsRect(0, 0, cWidth, cHeight);
            child.build(builder);
            if (!child.isBackground()) builder.restore(before);

            switch (this.layout) {
                case ROW -> {
                    builder.boundsRect(cWidth, 0);
                }
                case COLUMN -> {
                    builder.boundsRect(0, cHeight);
                }
            }


        }

        builder.restore(mark);
    }

    @Override
    public boolean handleClick(@NotNull ClickType clickType, int x, int y) {
        if (this.isHidden()) return false;
        if (this.children.isEmpty()) return false;
        if (this.children.size() == 1) {
            return this.children.getFirst().handleClick(clickType, x, y);
        }

        int currX = 0, currY = 0;
        for (var child : children) {
            if (child.isHidden()) continue;

            int cWidth = child.width(), cHeight = child.height();

            if (x >= currX && x < currX + cWidth && y >= currY && y < currY + cHeight) {
                return child.handleClick(clickType, x - currX, y - currY);
            }

            switch (this.layout) {
                case ROW -> currX += cWidth;
                case COLUMN -> currY += cHeight;
            }
        }

        return false;
    }

    public void appendChild(@NotNull Node child) {
        // This is a little gross, but oh well its fine :)
        if (!(this instanceof TextNode) && child instanceof TextNode.Raw) {
            throw new IllegalArgumentException("only text may have raw text contents");
        }

        this.children.add(child);
    }

    public void insertBefore(@NotNull Node child, @NotNull Node beforeChild) {
        int index = this.children.indexOf(beforeChild);
        if (index == -1) throw new IllegalArgumentException("insert target is not a child");
        this.children.add(index, child);
    }

    public void removeChild(@NotNull Node child) {
        this.children.remove(child);
    }
}
