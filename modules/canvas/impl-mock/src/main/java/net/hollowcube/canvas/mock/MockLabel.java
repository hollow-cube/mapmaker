package net.hollowcube.canvas.mock;

import net.hollowcube.canvas.Label;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockLabel extends MockElement implements Label {
    private final List<Component> args = new ArrayList<>();

    public MockLabel(@NotNull String id) {
        super(id);
    }

    @Override
    public void setArgs(@NotNull Component... args) {
        this.args.clear();
        Collections.addAll(this.args, args);
    }

    public @NotNull String argAsString(int arg) {
        assertTrue(arg < args.size(),
                "Requested argument " + arg + " but only " + args.size() + " arguments were set");
        return PlainTextComponentSerializer.plainText().serialize(args.get(arg));
    }
}
