package net.hollowcube.mapmaker.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationVisitor;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ConfigLoaderV3 {
    private static final String ENV_PREFIX = "MAPMAKER";

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoaderV3.class);

    public static @NotNull ConfigLoaderV3 loadDefault() {
        try (var is = ConfigLoaderV3.class.getResourceAsStream("/config.yaml")) {
            if (is == null) {
                logger.error("config.yaml not present in binary");
                System.exit(1);
            }

            return loadFromText(is.readAllBytes(), System.getenv());
        } catch (IOException e) {
            logger.error("failed to load config file", e);

            // Probably should shutdown gracefully, but this is in theory the first thing that runs so it doesnt matter that much.
            System.exit(1);
            throw new RuntimeException(e); // Does nothing besides stop the compiler from complaining about the exit above.
        }
    }

    public static @NotNull ConfigLoaderV3 loadFromText(byte @NotNull [] text, @NotNull Map<String, String> env) {
        try {
            var loader = YamlConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text))))
                    .build();

            var rootNode = loader.load();
            rootNode.visit(new EnvVarOverrideVisitor(env));
            //todo: load secrets from secret file.
            return new ConfigLoaderV3(rootNode);
        } catch (Exception e) {
            logger.error("failed to load config file", e);

            // Probably should shutdown gracefully, but this is in theory the first thing that runs so it doesnt matter that much.
            System.exit(1);
            throw new RuntimeException(e); // Does nothing besides stop the compiler from complaining about the exit above.
        }
    }

    private final ConfigurationNode root;

    private ConfigLoaderV3(ConfigurationNode root) {
        this.root = root;
    }

    public <C> @NotNull C get(@NotNull Class<C> clazz) {
        try {
            var path = clazz.getSimpleName().replace("Config", "")
                    .toLowerCase(Locale.ROOT);
            return Objects.requireNonNull(root.node(path).get(clazz));
        } catch (SerializationException e) {
            //todo
            throw new RuntimeException(e);
        }
    }

    private static class EnvVarOverrideVisitor implements ConfigurationVisitor.Stateless<Exception> {
        private final Map<String, String> env;

        private EnvVarOverrideVisitor(@NotNull Map<String, String> env) {
            this.env = env;
        }

        @Override
        public void enterNode(ConfigurationNode node) {
            // Do not care about non-leaf nodes
            if (!node.childrenMap().isEmpty()) return;

            var path = new StringBuilder().append(ENV_PREFIX);
            for (int i = 0; i < node.path().size(); i++)
                path.append("_").append(node.path().get(i).toString().toUpperCase(Locale.ROOT));
            if (path.isEmpty()) return;

            var value = this.env.get(path.toString());
            if (value == null) return;

            node.raw(value);
        }
    }
}
