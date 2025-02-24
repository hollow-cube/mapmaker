package net.hollowcube.mapmaker.dev;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DevServer {

    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    public static void main(String[] args) throws Exception {
//        CyloneImport.tempConvert();
//        MapServerInitializer.run(DevServerRunner::new, args);
        runGui();

//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/modules/hub/src/main/resources/spawn/hcspawn.polar");
//        var world = PolarReader.read(Files.readAllBytes(path));
//
//        var newWorld = new PolarWorld(
//                world.version(),
//                world.dataVersion(),
//                world.compression(),
//                world.minSection(), world.maxSection(),
//                world.userData(),
//                new ArrayList<>()
//        );
//
//        int total = 0;
//        outer:
//        for (var chunk : world.chunks()) {
//            for (var section : chunk.sections()) {
//                boolean isEmpty = section.isEmpty() || (section.blockPalette().length == 1 && NamespaceID.from(section.blockPalette()[0]).asMinimalString().equals("air"));
//                if (!isEmpty) {
//                    newWorld.updateChunkAt(chunk.x(), chunk.z(), chunk);
//                    total++;
//                    continue outer;
//                }
//            }
//
//            System.out.println("EMPTY SECTION AT " + chunk.x() + ", " + chunk.z());
//
//
//        }
//        System.out.println(total);
//
//        var newBytes = PolarWriter.write(newWorld);
//        System.out.println("total size: " + newBytes.length + " old is " + Files.readAllBytes(path).length);
//        Files.write(path, newBytes);
    }

    public static void runGui() throws Exception {
        var modules = new ConcurrentHashMap<String, Value>();
        modules.put("scheduler", loadNamedModule(modules, true, "scheduler"));
        modules.put("react", loadNamedModule(modules, true, "react"));
        modules.put("react-reconciler", loadNamedModule(modules, true, "react-reconciler"));

        var reactReconciler = Objects.requireNonNull(modules.get("react-reconciler"));
        var reactReconcilerInst = reactReconciler.execute(new ReactHostConfig());

        var react = Objects.requireNonNull(modules.get("react"));


        Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .build(); // TODO not closed

        context.getBindings("js").putMember("React", react);
        var component = context.eval("js", """
                function App() {
                    return React.createElement("div", {});
                }
                App.displayName = "App";
                React.createElement("div", {})
                """);
        System.out.println(react.getMemberKeys());

        var root = reactReconcilerInst.invokeMember("createContainer",
                /* containerInfo */ Value.asValue("I AM ROOT"),
                /* tag */ 0,
                /* hydrationCallbacks */ null,
                /* isStrictMode */ true,
                /* concurrentUpdatesByDefaultOverride */ null,
                /* identifierPrefix */ "a",
                /* onRecoverableError */ (ProxyExecutable) (args) -> {
//                    throw new RuntimeException("ERROR: " + args[0]);
                    System.out.println("ERROR: " + args[0]);
                    return null;
                },
                /* transitionCallbacks */null
        );
        var result = reactReconcilerInst.invokeMember("updateContainer",
                /* element */ component,
                /* container */ root,
                /* parentComponent */ null,
                /* callback */ null);

        System.out.println("LANE: " + result);

//        Context context = Context.newBuilder("js").build(); // TODO not closed
//
//        try (var is = DevServer.class.getResourceAsStream("/third_party/react/scheduler.js")) {
//            context.getBindings("js").putMember("process", ProxyObject.fromMap(Map.of(
//                    "env", Map.of("NODE_ENV", "development")
//            )));
//            var exports = context.eval("js", "({})");
//            context.getBindings("js").putMember("exports", exports);
//            var result = context.eval("js", new String(is.readAllBytes()));
//            System.out.println(result);
//            System.out.println(exports);
//        }
    }

    private static Value loadNamedModule(Map<String, Value> modules, boolean isDevelopment, @NotNull String name) throws Exception {
        Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .build(); // TODO not closed

        try (var is = DevServer.class.getResourceAsStream("/third_party/react/" + name + ".js")) {
            context.getBindings("js").putMember("process", ProxyObject.fromMap(Map.of(
                    "env", Map.of("NODE_ENV", isDevelopment ? "development" : "production")
            )));
            context.getBindings("js").putMember("setTimeout", (ProxyExecutable) (args) -> {
                System.out.println("setTimeout: " + args[1]);
                args[0].execute();
                return null;
            });
            context.getBindings("js").putMember("require", (ProxyExecutable) (args) -> {
                System.out.println("trying to load: " + args[0].asString());
                return Objects.requireNonNull(modules.get(args[0].asString()));
            });
            context.getBindings("js").putMember("exports",
                    context.eval("js", "({})"));
            context.getBindings("js").putMember("module",
                    context.eval("js", "({exports})"));

            var source = Source.newBuilder("js", new InputStreamReader(is), name + ".js").build();
            context.eval(source);
            return context.eval("js", "module.exports");
        }
    }
}
