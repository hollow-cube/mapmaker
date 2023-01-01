package net.hollowcube.mapmaker.dev;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationVisitor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;
import java.util.Locale;

public class ConfigTest2 {
    public static void main(String[] args) throws Exception {
        var loader = YamlConfigurationLoader.builder()
                .path(Path.of("/Users/matt/dev/projects/mmo/mapmaker/bin/development/src/main/resources/config.yaml"))
                .build();

        var root = loader.load();
        System.out.println(root);

        root.visit(new Test(), null);

        var config = root.get(Config.class);

        System.out.println(config);

    }

    @ConfigSerializable
    public record Config(
            Http http
    ) {}

    @ConfigSerializable
    public record Http(
            String host
    ) {}

    public static class Test implements ConfigurationVisitor<Void, Void, Exception> {

        @Override
        public Void newState() throws Exception {
            return null;
        }

        @Override
        public void beginVisit(ConfigurationNode node, Void state) throws Exception {

        }

        @Override
        public void enterNode(ConfigurationNode node, Void state) throws Exception {
            // Do not care about non-leaf nodes
            if (node.childrenMap().size() != 0) return;

            var path = new StringBuilder();
            for (int i = 0; i < node.path().size(); i++)
                path.append(node.path().get(i).toString().toUpperCase(Locale.ROOT)).append("_");
            if (path.isEmpty()) return;
            path.deleteCharAt(path.length() - 1);

            var value = System.getenv(path.toString());
            if (value == null) return;

            System.out.println(path + " " + value);
            node.raw(value);
        }

        @Override
        public void enterMappingNode(ConfigurationNode node, Void state) throws Exception {

        }

        @Override
        public void enterListNode(ConfigurationNode node, Void state) throws Exception {

        }

        @Override
        public void enterScalarNode(ConfigurationNode node, Void state) throws Exception {

        }

        @Override
        public void exitMappingNode(ConfigurationNode node, Void state) throws Exception {

        }

        @Override
        public void exitListNode(ConfigurationNode node, Void state) throws Exception {

        }

        @Override
        public Void endVisit(Void state) throws Exception {
            return null;
        }
    }
}
