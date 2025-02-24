package build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class Main {
    private static final List<String> REACT_DEPS = List.of("scheduler", "react", "react-reconciler");

    public static void main(String[] args) throws Exception {
        outer:
        for (final String dependency : REACT_DEPS) {
            Path out = null;
            for (final String arg : args) {
                if (!arg.endsWith("/" + dependency + ".js"))
                    continue;

                out = Path.of(arg);
                break;
            }
            if (out == null) {
                throw new RuntimeException("Could not find " + dependency + " in " + Arrays.toString(args));
            }

            for (final String arg : args) {
                final Path path = Path.of(arg).resolve("cjs/" + dependency + ".development.js");
                if (!Files.exists(path)) continue;

                Files.createDirectories(out.getParent());
                Files.copy(path, out);
                continue outer;
            }

            throw new RuntimeException("Could not find " + dependency + " in " + Arrays.toString(args));
        }
    }
}