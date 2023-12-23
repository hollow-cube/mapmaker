package net.hollowcube.mapmaker.config;

import net.hollowcube.common.config.ConfigPath;
import net.hollowcube.common.config.ConfigProvider;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationVisitor;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class NewConfigProvider implements ConfigProvider {
    private static final String ENV_PREFIX = "MAPMAKER";

    private static final System.Logger logger = System.getLogger(NewConfigProvider.class.getName());


    public static @NotNull NewConfigProvider loadFromFile(@NotNull Path path) {
        BufferedReader input = null;
        try {
            if (Files.exists(path)) {
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
            return new NewConfigProvider(rootNode);
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "failed to load config file", e);

            // Probably should shutdown gracefully, but this is in theory the first thing that runs so it doesnt matter that much.
            System.exit(1);
            throw new RuntimeException(e); // Does nothing besides stop the compiler from complaining about the exit above.
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (IOException e) {
                logger.log(System.Logger.Level.ERROR, "failed to close config file", e);
            }
        }
    }

    private final ConfigurationNode root;

    private NewConfigProvider(@NotNull ConfigurationNode root) {
        this.root = root;
    }

    @Override
    public <C extends Record> @NotNull C get(@NotNull Class<C> clazz) {
        var path = "";
        var anno = clazz.getAnnotation(ConfigPath.class);
        if (anno != null) path = anno.value();

        try {
            return Objects.requireNonNull(root.node(path).get(clazz));
        } catch (SerializationException e) {
            //todo
            throw new RuntimeException(e);
        }
    }

    private static class EnvVarOverrideVisitor implements ConfigurationVisitor.Stateless<Exception> {

        @Override
        public void enterNode(ConfigurationNode node) {
            // Do not care about non-leaf nodes
            if (node.childrenMap().size() != 0) return;

            var path = new StringBuilder().append(ENV_PREFIX);
            for (int i = 0; i < node.path().size(); i++)
                path.append("_").append(node.path().get(i).toString().toUpperCase(Locale.ROOT));
            if (path.isEmpty()) return;

            var value = System.getenv(path.toString());
            if (value == null) return;

            node.raw(value);
        }
    }

}
