package net.hollowcube.mapmaker.dev;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class YamlConfTest {

    public static class MyConfig {
        public String name;
    }

    public static void main(String[] args) {
        var yaml = new Yaml(new Constructor(MyConfig.class));
        MyConfig reader = yaml.load("name: test");
        System.out.println(reader.name);
    }

}
