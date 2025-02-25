package net.hollowcube.mapmaker.dev;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

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
//        var engine = new ScriptEngine();
//
//        var reactReconciler = engine.loadPrivileged(URI.create("internal://third_party/react/react-reconciler.js"));
//        var reactReconcilerInst = reactReconciler.exports().execute(new ReconcilerHostConfig());


        Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .build(); // TODO not closed

        context.getBindings("js").putMember("process", ProxyObject.fromMap(Map.of(
                "env", Map.of("NODE_ENV", "development")
        )));
        context.getBindings("js").putMember("setTimeout", (ProxyExecutable) (args) -> {
            System.out.println("setTimeout: " + args[1]);
            args[0].execute();
            return null;
        });
        var modules = new ConcurrentHashMap<String, Value>();
        context.getBindings("js").putMember("require", (ProxyExecutable) (args) -> {
            System.out.println("trying to load: " + args[0].asString());
            return Objects.requireNonNull(modules.get(args[0].asString()));
        });

        modules.put("scheduler", loadNamedModule(context, "scheduler"));
        modules.put("react", loadNamedModule(context, "react"));
        modules.put("react-reconciler", loadNamedModule(context, "react-reconciler"));

        var reactReconciler = Objects.requireNonNull(modules.get("react-reconciler"));
        var reactReconcilerInst = reactReconciler.execute(new ReactHostConfig());

        var componentModule = loadNamedModule(context, Source.newBuilder("js", "(function (exports, require, module, __filename, __dirname) {" + """
                "use strict";
                var __importDefault = (this && this.__importDefault) || function (mod) {
                    return (mod && mod.__esModule) ? mod : { "default": mod };
                };
                Object.defineProperty(exports, "__esModule", { value: true });
                exports.default = StoreView;
                const react_1 = __importDefault(require("react"));
                function StoreView() {
                    return react_1.default.createElement("div", null, "Hiiii");
                }
                """ + "})", "child-comp.js").build());
        var component = componentModule.getMember("default");
        var element = modules.get("react").invokeMember("createElement", component, null);

        var root = reactReconcilerInst.invokeMember("createContainer",
                /* containerInfo */ Value.asValue(Map.of()),
                /* tag */ 0,
                /* hydrationCallbacks */ null,
                /* isStrictMode */ true,
                /* concurrentUpdatesByDefaultOverride */ null,
                /* identifierPrefix */ "a",
                /* onRecoverableError */ (ProxyExecutable) (args) -> {
                    System.out.println("ERROR: " + args[0]);
                    return null;
                },
                /* transitionCallbacks */null
        );
        var result = reactReconcilerInst.invokeMember("updateContainer",
                /* element */ element,
                /* container */ root,
                /* parentComponent */ null,
                /* callback */ null);
        System.out.println("LANE: " + result);
    }

    private static Value loadNamedModule(Context context, @NotNull String name) throws Exception {
        try (var is = DevServer.class.getResourceAsStream("/third_party/react/" + name + ".js")) {
            // https://github.com/transposit/graal-commonjs-modules/blob/master/src/main/java/graal/Module.java#L299
            var raw = "(function (exports, require, module, __filename, __dirname) {" + new String(is.readAllBytes()) + "})";
            var source = Source.newBuilder("js", raw, name + ".js").build();
            return loadNamedModule(context, source);
        }
    }

    private static Value loadNamedModule(Context context, @NotNull Source source) throws Exception {
        var module = context.eval("js", "({})");
        var exports = context.eval("js", "({})");

        // TODO: these things should be present in the module object
        module.putMember("exports", exports);
        // module.putMember("children", new WrappedList(children));
        // module.putMember("filename", filename);
        // module.putMember("id", filename);
        // module.putMember("loaded", false);
        // module.putMember("parent", parent != null ? parent.module : null);

        var require = context.getBindings("js").getMember("require"); // TODO just dont add require to global scope
        context.eval(source).execute(exports, require, module, "todo_filename.js", "todo_dirname");
        return module.getMember("exports");
    }
}
