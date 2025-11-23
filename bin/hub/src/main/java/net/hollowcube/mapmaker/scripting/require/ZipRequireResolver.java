package net.hollowcube.mapmaker.scripting.require;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaStatus;
import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.luau.require.RequireResolver;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// uses @/path as chunknames
public class ZipRequireResolver implements RequireResolver {
    private static final Logger logger = LoggerFactory.getLogger(ZipRequireResolver.class);

    private static final List<String> SUFFIXES = List.of(".luau", ".lua", "/init.luau", "/init.lua");

    private record ResolvedPath(Result result, String realPath) {}

    private final Map<String, byte[]> vfs;

    private String realPath;
    private String absoluteRealPath;
    private String absolutePathPrefix;

    private String modulePath;
    private String absoluteModulePath;

    public ZipRequireResolver(LuauCompiler compiler, URI zip) throws IOException, LuauCompileException {
        this.vfs = readAndCompileEntries(compiler, zip);
    }

    @Deprecated
    public Map<String, byte[]> getVfsThisIsBadPleaseFix() {
        return vfs;
    }

    @Override
    public boolean isRequireAllowed(LuaState state, String requirerChunkName) {
        return true;
    }

    @Override
    public Result reset(LuaState luaState, String chunkName) {
        return resetToPath(chunkName);
    }

    @Override
    public Result toParent(LuaState luaState) {
        if ("/".equals(absoluteModulePath))
            return Result.NOT_FOUND; // At root

        modulePath = normalizePath(modulePath + "/..");
        absoluteModulePath = normalizePath(absoluteModulePath + "/..");

        // There is no ambiguity when navigating up in a tree.
        var result = updateRealPaths();
        return result == Result.AMBIGUOUS ? Result.PRESENT : result;
    }

    @Override
    public Result toChild(LuaState luaState, String name) {
        // Never resolve .config as a module since it conflicts with .config.luau files.
        if (".config".equals(name)) return Result.NOT_FOUND;

        modulePath = normalizePath(modulePath + "/" + name);
        absoluteModulePath = normalizePath(absoluteModulePath + "/" + name);

        return updateRealPaths();
    }

    @Override
    public Result jumpToAlias(LuaState luaState, String s) {
        return Result.NOT_FOUND;
    }

    @Override
    public Result getConfigStatus(LuaState luaState) {
        return Result.NOT_FOUND;
    }

    @Override
    public @Nullable String resolveAlias(LuaState luaState, String s) {
        return null;
    }

    @Override
    public @Nullable Module getModule(LuaState luaState) {
        return new Module(realPath, absoluteRealPath, absoluteRealPath);
    }

    @Override
    public int load(LuaState state, String path, String chunkName, String loadName) {
        // module needs to run in a new thread, isolated from the rest
        // note: we create ML on main thread so that it doesn't inherit environment of L
        LuaState main = state.mainThread();
        LuaState thread = main.newThread();
        main.xmove(state, 1);

        // new thread needs to have the globals sandboxed
        thread.sandboxThread();

        var bytecode = Objects.requireNonNull(vfs.get(loadName), loadName);

        thread.load(chunkName, bytecode); // throws on fail
        var status = thread.resume(state, 0); // todo deal with other states in resume its currently cooked
        if (status == LuaStatus.OK) {
            if (thread.top() != 1)
                throw state.error("module must return a single value");
        } else if (status == LuaStatus.YIELD) {
            throw state.error("module can not yield");
        }

        // add ML result to L stack
        thread.xmove(state, 1);

        // remove ML thread from L stack
        state.remove(-2);

        // added one value to L stack: module result
        return 1;
    }

    private Result resetToPath(String path) {
        var normalizedPath = normalizePath(path);

        if (isAbsolutePath(normalizedPath)) {
            modulePath = getModulePath(normalizedPath);
            absoluteModulePath = modulePath;

            int firstSlash = normalizedPath.indexOf('/');
            absolutePathPrefix = normalizedPath.substring(0, firstSlash);
        } else {
            throw new UnsupportedOperationException("non absolute path in reset");
//            std::optional<std::string> cwd = getCurrentWorkingDirectory();
//            if (!cwd)
//                return NavigationStatus::NotFound;
//
//            modulePath = getModulePath(normalizedPath);
//            std::string joinedPath = normalizePath(*cwd + "/" + normalizedPath);
//            absoluteModulePath = getModulePath(joinedPath);
//
//            size_t firstSlash = joinedPath.find_first_of('/');
//            LUAU_ASSERT(firstSlash != std::string::npos);
//            absolutePathPrefix = joinedPath.substr(0, firstSlash);
        }

        return updateRealPaths();
    }

    private Result updateRealPaths() {
        ResolvedPath result = getRealPath(modulePath);
        ResolvedPath absoluteResult = getRealPath(absoluteModulePath);
        if (result.result != Result.PRESENT || absoluteResult.result != Result.PRESENT)
            return result.result;

        realPath = isAbsolutePath(result.realPath) ? absolutePathPrefix + result.realPath : result.realPath;
        absoluteRealPath = absolutePathPrefix + absoluteResult.realPath;
        return Result.PRESENT;
    }

    private ResolvedPath getRealPath(String modulePath) {
        boolean found = false;
        String suffix = "";

        int lastSlash = modulePath.lastIndexOf('/');
        String lastComponent = modulePath.substring(lastSlash + 1);
        if (!"init".equals(lastComponent)) {
            for (var potentialSuffix : SUFFIXES) {
                if (isFile(modulePath + potentialSuffix)) {
                    if (found) return new ResolvedPath(Result.AMBIGUOUS, "");

                    suffix = potentialSuffix;
                    found = true;
                }
            }
        }
//        if (!found) return new ResolvedPath(Result.NOT_FOUND, "");
        return new ResolvedPath(Result.PRESENT, modulePath + suffix);
    }

    private String getModulePath(String filePath) {
        filePath = filePath.replaceAll("\\\\", "/");

        if (isAbsolutePath(filePath)) {
            int firstSlash = filePath.indexOf('/');
            filePath = filePath.substring(firstSlash);
        }

        for (var suffix : SUFFIXES) {
            if (filePath.endsWith(suffix)) {
                filePath = filePath.substring(0, filePath.length() - suffix.length());
                return filePath;
            }
        }

        return filePath;
    }

    private boolean isFile(String path) {
        return vfs.containsKey(path);
    }

    private static String normalizePath(String path) {
        var components = path.split("/");
        var normalizedComponents = new ArrayList<String>();

        boolean isAbsolute = isAbsolutePath(path);

        // 1. Normalize path components
        for (var component : components) {
            if ("..".equals(component)) {
                if (normalizedComponents.isEmpty()) {
                    if (!isAbsolute) normalizedComponents.add("..");
                } else if ("..".equals(normalizedComponents.getLast())) {
                    normalizedComponents.add("..");
                } else {
                    normalizedComponents.removeLast();
                }
            } else if (!component.isEmpty() && !".".equals(component)) {
                normalizedComponents.add(component);
            }
        }

        var normalizedPath = (isAbsolute ? "/" : "./") + String.join("/", normalizedComponents);
        if (normalizedPath.endsWith("..")) normalizedPath += "/";

        return normalizedPath;
    }

    private static boolean isAbsolutePath(String path) {
        return path.startsWith("/");
    }

    private static Map<String, byte[]> readAndCompileEntries(LuauCompiler compiler, URI zip) throws IOException, LuauCompileException {
        var vfs = new HashMap<String, byte[]>();
        try (var is = zip.toURL().openStream()) {
            if (is == null) throw new IOException("Could not open zip stream for URI: " + zip);

            var zis = new ZipInputStream(is);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                if (!entry.getName().endsWith(".luau"))
                    continue;

                var path = normalizePath("/" + entry.getName());
                var content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                vfs.put(path, compiler.compile(content));
                logger.info("compiled script {} ({} bytes)", path, content.length());
            }
        }
        return Map.copyOf(vfs);
    }
}
