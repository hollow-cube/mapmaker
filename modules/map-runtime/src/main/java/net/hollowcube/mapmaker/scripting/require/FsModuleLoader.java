package net.hollowcube.mapmaker.scripting.require;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.function.Consumer;

/// A filesystem backed module loader with support for hot reloading
public class FsModuleLoader extends AbstractModuleLoader {
    // TODO: https://github.com/google/jimfs for development on the live server

    private final LuauCompiler compiler;
    private final Path root;

    // todo reload obv will need more info
    private final Consumer<String> onReload;
    private final DirectoryWatcher watcher;

    public FsModuleLoader(LuauCompiler compiler, Path root, Consumer<String> reload) throws IOException {
        this.compiler = compiler;
        this.root = root;

        this.onReload = reload;
        this.watcher = DirectoryWatcher.builder()
            .path(root).listener(this::handleFileChanged)
            .build();
        this.watcher.watchAsync();
    }

    @Override
    protected boolean isFile(String path) {
        // TODO: this needs to not allow escaping the filesystem...
        return Files.isRegularFile(root.resolve(path), LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public @Nullable Module getModule(LuaState luaState) {
        var module = super.getModule(luaState);
        System.out.println("getModule from " + modulePath + " (" + absoluteModulePath + "): " + module);
        return module;
    }

    @Override
    public byte[] readAndParseFile(String loadName) throws IOException, LuauCompileException {
        if (loadName.startsWith("/")) loadName = loadName.substring(1);
        return compiler.compile(Files.readAllBytes(root.resolve(loadName)));
    }

    private void handleFileChanged(DirectoryChangeEvent event) {
        // Create is never relevant since we couldn't have depended on it from another module.
        // In the future maybe we track failed loads so we can reload on create?
        if (event.eventType() != DirectoryChangeEvent.EventType.MODIFY && event.eventType() != DirectoryChangeEvent.EventType.DELETE)
            return;

        var changed = '/' + root.relativize(event.path()).toString();
        System.out.println("File changed: " + changed);
        onReload.accept(changed); // todo need to sync on correct thread
    }
}
