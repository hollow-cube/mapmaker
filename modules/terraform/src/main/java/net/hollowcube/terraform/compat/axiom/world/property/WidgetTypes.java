package net.hollowcube.terraform.compat.axiom.world.property;

import net.hollowcube.terraform.util.ProtocolUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.minestom.server.network.NetworkBuffer.INT;

@SuppressWarnings("UnstableApiUsage")
final class WidgetTypes {

    record CheckboxType() implements WidgetType<Boolean> {
        static final CheckboxType INSTANCE = new CheckboxType();

        @Override
        public int typeId() {
            return 0;
        }

        @Override
        public @NotNull DataType<Boolean> dataType() {
            return DataType.BOOLEAN;
        }

        @Override
        public byte[] properties() {
            return new byte[0];
        }
    }

    record SliderType(int min, int max) implements WidgetType<Integer> {

        @Override
        public int typeId() {
            return 1;
        }

        @Override
        public @NotNull DataType<Integer> dataType() {
            return DataType.INTEGER;
        }

        @Override
        public byte[] properties() {
            return ProtocolUtil.makeArray(8, buffer -> {
                buffer.write(INT, min);
                buffer.write(INT, max);
            });
        }
    }

    record TextBoxType() implements WidgetType<String> {
        static final TextBoxType INSTANCE = new TextBoxType();

        @Override
        public int typeId() {
            return 2;
        }

        @Override
        public @NotNull DataType<String> dataType() {
            return DataType.STRING;
        }

        @Override
        public byte[] properties() {
            return new byte[0];
        }
    }

    record TimeType() implements WidgetType<Integer> {
        static final TimeType INSTANCE = new TimeType();

        @Override
        public int typeId() {
            return 3;
        }

        @Override
        public @NotNull DataType<Integer> dataType() {
            return DataType.INTEGER;
        }

        @Override
        public byte[] properties() {
            return new byte[0];
        }
    }

    record ButtonType() implements WidgetType<Void> {
        static final ButtonType INSTANCE = new ButtonType();

        @Override
        public int typeId() {
            return 4;
        }

        @Override
        public @NotNull DataType<Void> dataType() {
            return DataType.EMPTY;
        }

        @Override
        public byte[] properties() {
            return new byte[0];
        }
    }

    record ButtonArrayType(@NotNull List<String> buttons) implements WidgetType<Integer> {

        @Override
        public int typeId() {
            return 5;
        }

        @Override
        public @NotNull DataType<Integer> dataType() {
            return DataType.INTEGER;
        }

        @Override
        public byte[] properties() {
            // TODO(1.21.2)
            return new byte[0];
//            return ProtocolUtil.makeArray(32, buffer ->
//                    buffer.writeCollection(buttons, (b, s) -> b.write(STRING, s)));
        }
    }

    private WidgetTypes() {
    }
}
