package net.hollowcube.ipc.gen.wire;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// The tag walk over a real repository: which tags are checked, which are skipped, and what a
/// break at one of them prints.
class WireCompatTest {

    private static final String V1 = """
        {"services": {"echo": {"interface": "test.EchoService", "methods": {"echo": {"params": [{"name": "message", "type": "String"}]}}}}}
        """;
    private static final String V2 = """
        {"services": {"echo": {"interface": "test.EchoService", "methods": {"echo": {"params": [{"name": "message", "type": "String"}, {"name": "count", "type": "int", "nullable": true}]}}}}}
        """;

    @TempDir
    Path dir;

    @Test
    void checksEveryReleaseTagFromTheBaselineUp() throws Exception {
        var repo = repo();
        git(repo, "commit", "--allow-empty", "-m", "before wire.json");
        git(repo, "tag", "0.9.0");
        commit(repo, V1, "v1");
        git(repo, "tag", "1.0.0");
        git(repo, "tag", "replay-backup");
        commit(repo, V2, "v2");
        git(repo, "tag", "1.1.0");

        // Current still has both params: fine against every tag.
        var out = run(repo, V2, "1.0.0");
        assertTrue(out.ok(), out.text());
        assertTrue(out.text().contains("checking 2 release tag(s)"), out.text());
        assertTrue(out.text().contains("wire-compat: ok"), out.text());

        // Dropping `count` again is fine for 1.0.0, which never had it, but not for 1.1.0.
        var broken = run(repo, V1.replace("\"message\", \"type\": \"String\"", "\"message\", \"type\": \"int\""), "1.0.0");
        assertFalse(broken.ok());
        assertTrue(broken.text().contains("1.0.0: service echo / method echo / param message: type String -> int"), broken.text());
        assertTrue(broken.text().contains("1.1.0: service echo / method echo / param message: type String -> int"), broken.text());

        // From 1.1.0 up, 1.0.0 is not looked at.
        var later = run(repo, V1, "1.1.0");
        assertTrue(later.ok(), later.text());
        assertTrue(later.text().contains("checking 1 release tag(s)"), later.text());
    }

    @Test
    void tagsThatPredateTheDescriptorAreSkippedWithANote() throws Exception {
        var repo = repo();
        git(repo, "commit", "--allow-empty", "-m", "before wire.json");
        git(repo, "tag", "1.0.0");
        commit(repo, V1, "v1");
        git(repo, "tag", "1.1.0");

        var out = run(repo, V1, "1.0.0");
        assertTrue(out.ok(), out.text());
        assertTrue(out.text().contains("1.0.0: no modules/ipc/wire.json at this tag, skipped"), out.text());
    }

    @Test
    void aBaselineThatIsNotATagFails() throws Exception {
        var repo = repo();
        commit(repo, V1, "v1");
        git(repo, "tag", "1.0.0");

        var out = run(repo, V1, "1.0.1");
        assertFalse(out.ok());
        assertTrue(out.text().contains("baseline 1.0.1 is not a release tag"), out.text());
    }

    /// A checkout without tags is what a shallow clone looks like, and passing on it would be the
    /// check silently not running.
    @Test
    void aRepositoryWithoutReleaseTagsFails() throws Exception {
        var repo = repo();
        commit(repo, V1, "v1");

        var out = run(repo, V1, "1.0.0");
        assertFalse(out.ok());
        assertTrue(out.text().contains("no release tags"), out.text());
    }

    // ----- plumbing -----

    private record Run(boolean ok, String text) {
    }

    private Run run(Path repo, String current, String baseline) throws IOException {
        var currentFile = Files.writeString(dir.resolve("current.json"), current);
        var baselineFile = Files.writeString(dir.resolve("wire-baseline"), "# comment\n\n" + baseline + "\n");
        var bytes = new ByteArrayOutputStream();
        var ok = WireCompat.run(currentFile, baselineFile, repo, "modules/ipc/wire.json", new PrintStream(bytes, true, StandardCharsets.UTF_8));
        return new Run(ok, bytes.toString(StandardCharsets.UTF_8));
    }

    private Path repo() throws Exception {
        var repo = Files.createDirectories(dir.resolve("repo"));
        git(repo, "init", "-q");
        git(repo, "config", "user.email", "test@example.com");
        git(repo, "config", "user.name", "test");
        git(repo, "config", "commit.gpgsign", "false");
        return repo;
    }

    private static void commit(Path repo, String descriptor, String message) throws Exception {
        var file = repo.resolve("modules/ipc/wire.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, descriptor);
        git(repo, "add", ".");
        git(repo, "commit", "-q", "-m", message);
    }

    private static void git(Path repo, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", repo.toString()));
        command.addAll(List.of(args));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), "git " + String.join(" ", args) + ": " + output);
    }
}
