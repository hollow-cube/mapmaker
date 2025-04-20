package net.hollowcube.mapmaker.scripting.gui.util;

import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

/**
 * A value which can
 */
public class Align {
    private static final int MAX_NUMBER = 1 << 29;
    private static final int START = 0;
    private static final int CENTER = 1 << 30;
    private static final int END = 1 << 31;

    private int value = START;

    public boolean updateFromProps(@NotNull Value props, @NotNull String name) {
        int oldValue = this.value;
        if (props.hasMember(name)) {
            var member = props.getMember(name);
            if (member.isNumber()) {
                int number = member.asInt();
                if (number > -MAX_NUMBER && number < MAX_NUMBER) {
                    this.value = number;
                } else {
                    throw new IllegalArgumentException("Offset out of bounds");
                }
            } else {
                String value = member.asString();
                this.value = switch (value) {
                    case "start" -> START;
                    case "center" -> CENTER;
                    case "end" -> END;
                    case null, default -> throw new IllegalArgumentException("Invalid value for align");
                };
            }
        } else {
            this.value = START;
        }

        return oldValue != this.value;
    }

    public int value(int size, int parentSize) {
        if (this.value > -MAX_NUMBER && this.value < MAX_NUMBER) {
            return this.value; // Start is 0 so also covered here
        }

        if (this.value == CENTER) {
            return (parentSize - size) / 2;
        } else if (this.value == END) {
            return parentSize - size;
        } else {
            return this.value;
        }
    }

}
