package net.hollowcube.ipc.gen.wire;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/// `wireCompat`: diffs the descriptor this build produced against the one committed at every
/// release tag still in production, and fails on the first thing an old client could not survive.
///
/// The floor is `modules/ipc/wire-baseline`, the oldest tag still running. Every release tag from
/// there up is checked, not only the baseline, because a field added at one release and removed
/// at the next breaks the release in between. A tag that predates `wire.json` has nothing to
/// hold the build to and is skipped with a note.
public final class WireCompat {

    private static final Pattern RELEASE_TAG = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    private static final Comparator<String> BY_VERSION = Comparator.comparing(Version::parse,
        Comparator.comparingInt(Version::major).thenComparingInt(Version::minor).thenComparingInt(Version::patch));

    public static void main(String[] args) {
        Path current = null, baseline = null, repo = null;
        String path = null;
        for (int i = 0; i + 1 < args.length; i += 2) {
            switch (args[i]) {
                case "--current" -> current = Path.of(args[i + 1]);
                case "--baseline" -> baseline = Path.of(args[i + 1]);
                case "--repo" -> repo = Path.of(args[i + 1]);
                case "--path" -> path = args[i + 1];
                default -> throw new IllegalArgumentException("unknown argument " + args[i]);
            }
        }
        if (current == null || baseline == null || repo == null || path == null) {
            throw new IllegalArgumentException("usage: --current <wire.json> --baseline <wire-baseline> --repo <dir> --path <repo path of wire.json>");
        }
        System.exit(run(current, baseline, repo, path, System.out) ? 0 : 1);
    }

    /// Answers whether the current descriptor is compatible with every checked tag.
    static boolean run(Path currentFile, Path baselineFile, Path repo, String path, PrintStream out) {
        var current = WireDescriptor.parse(read(currentFile));
        var baseline = baseline(baselineFile);
        var git = new Git(repo);

        var tags = git.tags().stream()
            .filter(tag -> RELEASE_TAG.matcher(tag).matches())
            .sorted(BY_VERSION)
            .toList();
        if (tags.isEmpty()) {
            out.println("wire-compat: no release tags in " + repo + "; is this a checkout without tags?");
            return false;
        }
        if (!tags.contains(baseline)) {
            out.println("wire-compat: baseline " + baseline + " is not a release tag");
            return false;
        }
        var checked = tags.stream().filter(tag -> BY_VERSION.compare(tag, baseline) >= 0).toList();
        out.println("wire-compat: baseline " + baseline + ", checking " + checked.size() + " release tag(s)");

        var breaks = 0;
        for (var tag : checked) {
            var json = git.show(tag, path);
            if (json == null) {
                out.println(tag + ": no " + path + " at this tag, skipped");
                continue;
            }
            var found = WireDiff.diff(WireDescriptor.parse(json), current);
            for (var item : found) out.println(tag + ": " + item);
            breaks += found.size();
        }
        if (breaks > 0) {
            out.println("wire-compat: " + breaks + " break(s); an old client at one of these tags would not survive this build. "
                + "Move " + baselineFile.getFileName() + " forward only once nothing older is calling.");
            return false;
        }
        out.println("wire-compat: ok");
        return true;
    }

    /// The first line of the baseline file that is not blank or a comment.
    private static String baseline(Path file) {
        for (var line : read(file).lines().toList()) {
            var trimmed = line.strip();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) return trimmed;
        }
        throw new IllegalArgumentException(file + " names no baseline tag");
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// A release tag as numbers, so that `1.58.3` sorts after `1.9.0`.
    private record Version(int major, int minor, int patch) {
        static Version parse(String tag) {
            var parts = tag.split("\\.");
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
    }

    private record Git(Path repo) {
        List<String> tags() {
            return run("tag", "--list").output().lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
        }

        /// The file at `tag`, or null when the tag has no such file. Any other failure — an
        /// unreadable tag, no git — is an error, because passing silently is the one thing this
        /// check must not do.
        String show(String tag, String path) {
            var result = run("show", tag + ":" + path);
            if (result.status() == 0) return result.output();
            if (result.output().contains("does not exist in") || result.output().contains("exists on disk, but not in")) return null;
            throw new IllegalStateException("git show " + tag + ":" + path + " failed: " + result.output().strip());
        }

        private record Result(int status, String output) {
        }

        private Result run(String... args) {
            var command = new ArrayList<String>(args.length + 3);
            command.add("git");
            command.add("-C");
            command.add(repo.toString());
            command.addAll(List.of(args));
            try {
                var process = new ProcessBuilder(command).redirectErrorStream(true).start();
                var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return new Result(process.waitFor(), output);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted running " + command, e);
            }
        }
    }

    private WireCompat() {
    }
}
