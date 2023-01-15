package net.hollowcube.mapmaker.dev.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationVisitor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

@ConfigSerializable
public record Config(
        HttpConf http,
        MinestomConf minestom,
        MongoConf mongo,
        S3Conf s3,
        SpiceDBConf spicedb
) {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    /**
     * Loads the config from the given path, or loads the packaged one if the path does not exist.
     */
    public static @NotNull Config loadFromFile(@NotNull Path path) {
        BufferedReader input = null;
        try {
            if (Files.exists(path)) {
                //noinspection resource
                input = Files.newBufferedReader(path);
            } else {
                //noinspection DataFlowIssue - NPE caught below
                input = new BufferedReader(new InputStreamReader(Config.class.getResourceAsStream("/config.yaml"), Charset.defaultCharset()));
            }

            var finalInput = input;
            var loader = YamlConfigurationLoader.builder()
                    .source(() -> finalInput)
                    .build();

            var rootNode = loader.load();
            rootNode.visit(new EnvVarOverrideVisitor());
            return Objects.requireNonNull(rootNode.get(Config.class));
        } catch (Exception e) {
            logger.error("failed to load config file", e);

            // Probably should shutdown gracefully, but this is in theory the first thing that runs so it doesnt matter that much.
            System.exit(1);
            throw new RuntimeException(e); // Does nothing besides stop the compiler from complaining about the exit above.
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (IOException e) {
                logger.error("failed to close config file", e);
            }
        }
    }

    private static class EnvVarOverrideVisitor implements ConfigurationVisitor.Stateless<Exception> {

        @Override
        public void enterNode(ConfigurationNode node) {
            // Do not care about non-leaf nodes
            if (node.childrenMap().size() != 0) return;

            var path = new StringBuilder().append("MAPMAKER");
            for (int i = 0; i < node.path().size(); i++)
                path.append("_").append(node.path().get(i).toString().toUpperCase(Locale.ROOT));
            if (path.isEmpty()) return;

            var value = System.getenv(path.toString());
            if (value == null) return;

            node.raw(value);
        }
    }
}
