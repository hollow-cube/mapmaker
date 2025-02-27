package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.hollowcube.mapmaker.scripting.gui.react.ReconcilerHostConfig;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class DevServer {

    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    public static void main(String[] args) throws Exception {
//        CyloneImport.tempConvert();
        runGui();
        MapServerInitializer.run(DevServerRunner::new, args);

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

    public static InventoryHost host;
    public static Component title;
    public static ItemStack[] itemList;

    public static void runGui() throws Exception {
        var engine = new ScriptEngine();

        var reactReconciler = engine.load(URI.create("internal:///third_party/react/react-reconciler.js"));
        var reactReconcilerInst = reactReconciler.exports().execute(new ReconcilerHostConfig());

        var componentSource = Files.readString(Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/guilib/dist/StoreView.js"));
        var componentModule = engine.loadText("StoreView.js", componentSource).exports().getMember("default");
        var react = engine.load(URI.create("internal:///third_party/react/react.js"));
        var element = react.exports().invokeMember("createElement", componentModule, null);

        var rootNode = new InventoryHost(null);
        var root = reactReconcilerInst.invokeMember("createContainer",
                /* containerInfo */ rootNode,
                /* tag */ 0,
                /* hydrationCallbacks */ null,
                /* isStrictMode */ true,
                /* concurrentUpdatesByDefaultOverride */ null,
                /* identifierPrefix */ "a",
                /* onRecoverableError */ (ProxyExecutable) (args) -> {
                    System.out.println("ERROR: " + args[0]);
                    return null;
                },
                /* transitionCallbacks */ null
        );
        var result = reactReconcilerInst.invokeMember("updateContainer",
                /* element */ element,
                /* container */ root,
                /* parentComponent */ null,
                /* callback */ null);
        System.out.println("LANE: " + result);
        System.out.println(rootNode);

        var items = rootNode.build();
        for (int i = 0; i < 9; i++) {
            System.out.println(i + ": " + items[i].material().name());
        }

        title = rootNode.titleTemp;
        itemList = rootNode.itemsTemp;
        host = rootNode;


    }

}
