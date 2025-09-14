package net.hollowcube.mapmaker.runtime.freeform.bundle;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ScriptBundle {

    interface Loader {
        @Nullable ScriptBundle load(String id);
    }

    record Entrypoint(Type type, String script) {
        public enum Type {
            WORLD
        }
    }

    record Script(String filename, String content) {
    }

    String id();

    List<Entrypoint> entrypoints();

    Script loadScript(String name);

}
