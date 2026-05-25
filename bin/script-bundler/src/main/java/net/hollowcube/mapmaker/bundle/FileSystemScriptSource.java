package net.hollowcube.mapmaker.bundle;

import net.hollowcube.mapmaker.editor.scripting.ScriptSource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class FileSystemScriptSource implements ScriptSource {

    private final Path root;

    public FileSystemScriptSource(Path root) {
        if (!Files.isDirectory(root))
            throw new IllegalArgumentException("source dir does not exist or is not a directory: " + root);
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Iterable<String> listFiles() {
        try (Stream<Path> walk = Files.walk(root)) {
            List<String> out = new ArrayList<>();
            walk.filter(Files::isRegularFile)
                .filter(this::isUserFile)
                .forEach(p -> out.add(toCanonicalPath(p)));
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to list files under " + root, e);
        }
    }

    private boolean isUserFile(Path file) {
        var rel = root.relativize(file);
        for (Path seg : rel) {
            var name = seg.toString();
            if (!name.isEmpty() && name.charAt(0) == '.') return false;
        }
        return true;
    }

    @Override
    public byte @Nullable [] readFile(String path) {
        var resolved = root.resolve(stripLeadingSlash(path)).normalize();
        if (!resolved.startsWith(root)) return null;
        if (!Files.isRegularFile(resolved)) return null;
        try {
            return Files.readAllBytes(resolved);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + resolved, e);
        }
    }

    private String toCanonicalPath(Path file) {
        var rel = root.relativize(file).toString().replace(java.io.File.separatorChar, '/');
        return "/" + rel;
    }

    private static String stripLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }
}
