package net.hollowcube.mapmaker.config;

import java.util.Map;

public interface ConfigLoaderV3 {
    static ConfigLoaderV3 loadDefault(String[] args) {
        return ConfigLoaderV3Impl.loadDefault(args);
    }

    static ConfigLoaderV3 loadFromText(byte[] text, Map<String, String> env) {
        return ConfigLoaderV3Impl.loadFromText(text, env);
    }

    <C> C get(Class<C> clazz);

    void dump();
}
