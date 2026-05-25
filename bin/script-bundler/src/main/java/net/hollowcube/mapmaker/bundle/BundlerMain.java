package net.hollowcube.mapmaker.bundle;

import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.editor.scripting.MapClientScriptSource;
import net.hollowcube.mapmaker.editor.scripting.ScriptSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class BundlerMain {
    private static final Logger logger = LoggerFactory.getLogger(BundlerMain.class);

    static void main(String[] args) throws Exception {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("usage: bundler --out <zip> ( --source-dir <path> | --map-id <id> --api-url <url> )");
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }

        ScriptSource source = parsed.sourceDir != null
            ? new FileSystemScriptSource(parsed.sourceDir)
            : new MapClientScriptSource(httpMapClient(parsed.apiUrl), parsed.mapId);

        logger.info("bundling scripts from {} → {}",
            parsed.sourceDir != null ? parsed.sourceDir : ("map-id=" + parsed.mapId),
            parsed.out);

        new BundleAssembler().assemble(source, parsed.out);
        logger.info("wrote bundle to {}", parsed.out);
    }

    private static MapClient httpMapClient(String apiUrl) {
        var http = new HttpClientWrapper(OpenTelemetry.noop(), apiUrl);
        return new MapClient.Http(http);
    }

    private record Args(Path out, Path sourceDir, String mapId, String apiUrl) {
        static Args parse(String[] argv) {
            Map<String, String> flags = new HashMap<>();
            for (int i = 0; i < argv.length; i++) {
                if (!argv[i].startsWith("--"))
                    throw new IllegalArgumentException("unexpected positional argument: " + argv[i]);
                if (i + 1 >= argv.length)
                    throw new IllegalArgumentException("flag missing value: " + argv[i]);
                flags.put(argv[i], argv[++i]);
            }

            String out = flags.get("--out");
            if (out == null) throw new IllegalArgumentException("--out is required");

            String sourceDir = flags.get("--source-dir");
            String mapId = flags.get("--map-id");
            String apiUrl = flags.get("--api-url");

            boolean fsMode = sourceDir != null;
            boolean mapMode = mapId != null;
            if (fsMode == mapMode)
                throw new IllegalArgumentException(
                    "exactly one of --source-dir or --map-id must be provided");
            if (mapMode && apiUrl == null)
                throw new IllegalArgumentException("--map-id requires --api-url");

            return new Args(
                Path.of(out),
                fsMode ? Path.of(sourceDir) : null,
                mapId,
                apiUrl);
        }
    }
}
