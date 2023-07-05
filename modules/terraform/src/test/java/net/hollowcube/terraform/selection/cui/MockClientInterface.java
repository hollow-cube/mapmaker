package net.hollowcube.terraform.selection.cui;

import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.cui.ClientRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MockClientInterface implements ClientInterface {
    private final List<String> messages = new ArrayList<>();

    private final MockSelectionRenderer renderer = new MockSelectionRenderer();

    @Override
    public void sendMessage(@NotNull String key, @NotNull Object... args) {
        messages.add(key);
    }

    @Override
    public @NotNull MockSelectionRenderer renderer() {
        return renderer;
    }

}
