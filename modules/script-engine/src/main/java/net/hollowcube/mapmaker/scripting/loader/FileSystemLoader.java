package net.hollowcube.mapmaker.scripting.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Loads scripts from a directory (containing the compiled outputs of a typescript project set to commonjs).
 *
 * <p>Hot reload is supported and will be triggered on file change.</p>
 */
public class FileSystemLoader implements ScriptLoader {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemLoader.class);

    private final String scheme;
    private final Path root;
    private final ReloadHook reloadHook;

    private final @Nullable WatchService watcher;
    private final @Nullable Thread watcherThread;

    // Loaded uri mapping keeps track of which path is loaded from which URI.
    // It is a debugging to warn if we load the same module from different paths (which will have different exports etc).
    // This is only used when we have a reload hook, aka are running in a "development" environment.
    // This is not a security feature.
    //
    // This may need to change in the future I(matt) believe that its valid in nodejs module resolution to resolve the
    // same module differently and have different instances of it. Not sure if we want to replicate that behavior tho.
    private final Map<Path, URI> loadedUriMapping;

    public FileSystemLoader(@NotNull String scheme, @NotNull Path root, @Nullable ReloadHook reloadHook) throws IOException {
        this.scheme = scheme;
        this.root = root.toRealPath();

        if ((this.reloadHook = reloadHook) != null) {
            this.watcher = FileSystems.getDefault().newWatchService();
            this.watcherThread = createFileWatcher();
            this.loadedUriMapping = new HashMap<>();
        } else {
            this.watcher = null;
            this.watcherThread = null;
            this.loadedUriMapping = null;
        }
    }

    @Override
    public @Nullable String load(@NotNull URI uri) throws IOException {
        if (!this.scheme.equals(uri.getScheme()))
            throw new IllegalArgumentException("uri scheme does not match loader scheme");

        String relativePath = uri.getPath();
        while (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);
        final Path filePath = this.root.resolve(relativePath).toRealPath();

        // The loaded path must be within the root directory.
        if (!filePath.startsWith(this.root))
            throw new IllegalArgumentException("uri path is outside of loader root");

        // Check if we have already loaded this module from a different path.
        if (this.loadedUriMapping != null) {
            final URI existingUri = this.loadedUriMapping.get(filePath);
            if (existingUri != null && !existingUri.equals(uri)) {
                logger.warn("module loaded from different path: originally {}; now {}", existingUri, uri);
            }
            this.loadedUriMapping.put(filePath, uri);
        }

        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        if (this.watcherThread != null) this.watcherThread.interrupt();
        if (this.watcher != null) this.watcher.close();
    }

    private @NotNull Thread createFileWatcher() throws IOException {
        Objects.requireNonNull(this.watcher); // Precondition

        watchDirectoryRecursive(this.root);

        return Thread.startVirtualThread(() -> {
            try {
                WatchKey key;
                while ((key = this.watcher.take()) != null) {
                    final Path directory = (Path) key.watchable();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        final Path path = directory.resolve((Path) event.context());
                        if (!Files.exists(path)) continue; // Sanity/out of sync handling

                        // If this is a directory, we need to add/remove it from the watch service.
                        if (Files.isDirectory(path)) {
                            if (event.kind().equals(ENTRY_CREATE)) {
                                watchDirectoryRecursive(path);
                            }
                            // Delete invalidates the WatchKey automatically after the #reset call.
                            continue;
                        }

                        final URI moduleUri = URI.create("%s:///%s".formatted(this.scheme, this.root.relativize(path)));
                        if (event.kind().equals(ENTRY_MODIFY) || event.kind().equals(ENTRY_CREATE)) {
                            logger.info("module changed: {}", moduleUri);
                            this.reloadHook.onReload(moduleUri, this.load(moduleUri)); // Reload
                        } else if (event.kind().equals(ENTRY_DELETE)) {
                            logger.info("module deleted: {}", moduleUri);
                            this.reloadHook.onReload(moduleUri, null); // Unload
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                logger.error("an exception occurred in file watch thread", e);
            }
        });
    }

    private void watchDirectoryRecursive(@NotNull Path directory) throws IOException {
        // Empty set indicates that we dont follow links. Max depth of 20 is just a sanity limit not sure what it should be.
        Files.walkFileTree(directory, Set.of(), 20, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(FileSystemLoader.this.watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
