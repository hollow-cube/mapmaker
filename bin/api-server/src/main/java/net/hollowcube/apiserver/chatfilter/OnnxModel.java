package net.hollowcube.apiserver.chatfilter;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/// FFM wrapper over onnxruntime C api (the java api is jni and does runtime unpacking we
/// dont want to deal with). [#run] is thread safe.
///
/// Every function is called through its slot in the `OrtApi` table, which onnxruntime only ever
/// appends to, so the indices below hold for any library at least as new as [#API_VERSION]. Each
/// call shape is registered for native-image under `foreign` in `reachability-metadata.json`.
public final class OnnxModel implements AutoCloseable {
    private static final int API_VERSION = 20;

    private static final int GET_ERROR_MESSAGE = 2;
    private static final int CREATE_ENV = 3;
    private static final int CREATE_SESSION = 7;
    private static final int RUN = 9;
    private static final int CREATE_SESSION_OPTIONS = 10;
    private static final int SET_SESSION_GRAPH_OPTIMIZATION_LEVEL = 23;
    private static final int SET_INTRA_OP_NUM_THREADS = 24;
    private static final int SET_INTER_OP_NUM_THREADS = 25;
    private static final int CREATE_TENSOR_WITH_DATA_AS_ORT_VALUE = 49;
    private static final int GET_TENSOR_MUTABLE_DATA = 51;
    private static final int CREATE_CPU_MEMORY_INFO = 69;
    private static final int RELEASE_ENV = 92;
    private static final int RELEASE_STATUS = 93;
    private static final int RELEASE_MEMORY_INFO = 94;
    private static final int RELEASE_SESSION = 95;
    private static final int RELEASE_VALUE = 96;
    private static final int RELEASE_SESSION_OPTIONS = 100;
    private static final int API_SLOTS = 101;

    private static final int LOGGING_LEVEL_WARNING = 2;
    private static final int ENABLE_ALL = 99;
    private static final int ARENA_ALLOCATOR = 1;
    private static final int MEM_TYPE_DEFAULT = 0;
    private static final int TENSOR_INT64 = 7;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final MethodHandle P_ = handle(FunctionDescriptor.of(ADDRESS));
    private static final MethodHandle P_I = handle(FunctionDescriptor.of(ADDRESS, JAVA_INT));
    private static final MethodHandle P_P = handle(FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle V_P = handle(FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle P_PI = handle(
        FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT)
    );
    private static final MethodHandle P_PP = handle(
        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)
    );
    private static final MethodHandle P_IPP = handle(
        FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS)
    );
    private static final MethodHandle P_IIP = handle(
        FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS)
    );
    private static final MethodHandle P_PPPP = handle(
        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS)
    );
    private static final MethodHandle P_PPLPLIP = handle(
        FunctionDescriptor.of(
            ADDRESS,
            ADDRESS,
            ADDRESS,
            JAVA_LONG,
            ADDRESS,
            JAVA_LONG,
            JAVA_INT,
            ADDRESS
        )
    );
    private static final MethodHandle P_PPPPLPLP = handle(
        FunctionDescriptor.of(
            ADDRESS,
            ADDRESS,
            ADDRESS,
            ADDRESS,
            ADDRESS,
            JAVA_LONG,
            ADDRESS,
            JAVA_LONG,
            ADDRESS
        )
    );

    private final MemorySegment api;
    private final MemorySegment env;
    private final MemorySegment session;
    private final MemorySegment memoryInfo;
    private final MemorySegment inputNames;
    private final MemorySegment outputNames;
    private final int outputs;

    private OnnxModel(
        MemorySegment api,
        MemorySegment env,
        MemorySegment session,
        MemorySegment memoryInfo,
        MemorySegment inputNames,
        MemorySegment outputNames,
        int outputs
    ) {
        this.api = api;
        this.env = env;
        this.session = session;
        this.memoryInfo = memoryInfo;
        this.inputNames = inputNames;
        this.outputNames = outputNames;
        this.outputs = outputs;
    }

    /// Opens `model` with the onnxruntime shared library at `library`, which stays loaded for the
    /// life of the process.
    ///
    /// The session runs single-threaded: under a cgroup cpu limit a second intra-op thread lowers
    /// the median and blows up the tail, and concurrency comes from callers instead.
    public static OnnxModel load(Path library, Path model, String outputName, int outputs) {
        var lookup = SymbolLookup.libraryLookup(library, Arena.global());
        var base = call(
            P_,
            lookup.findOrThrow("OrtGetApiBase")
        ).reinterpret(2 * ADDRESS.byteSize());
        var api = call(P_I, base.getAtIndex(ADDRESS, 0), API_VERSION);
        if (api.equals(MemorySegment.NULL))
            throw new IllegalStateException(
                library + " is older than onnxruntime api " + API_VERSION
            );
        api = api.reinterpret(API_SLOTS * ADDRESS.byteSize());

        try (var arena = Arena.ofConfined()) {
            var out = arena.allocate(ADDRESS);
            check(
                api,
                call(
                    P_IPP,
                    fn(api, CREATE_ENV),
                    LOGGING_LEVEL_WARNING,
                    arena.allocateFrom("chat-filter"),
                    out
                )
            );
            var env = out.get(ADDRESS, 0);

            check(api, call(P_P, fn(api, CREATE_SESSION_OPTIONS), out));
            var options = out.get(ADDRESS, 0);
            try {
                check(api, call(P_PI, fn(api, SET_INTRA_OP_NUM_THREADS), options, 1));
                check(api, call(P_PI, fn(api, SET_INTER_OP_NUM_THREADS), options, 1));
                check(
                    api,
                    call(P_PI, fn(api, SET_SESSION_GRAPH_OPTIMIZATION_LEVEL), options, ENABLE_ALL)
                );
                var path = arena.allocateFrom(model.toAbsolutePath().toString());
                check(api, call(P_PPPP, fn(api, CREATE_SESSION), env, path, options, out));
            } finally {
                callVoid(fn(api, RELEASE_SESSION_OPTIONS), options);
            }
            var session = out.get(ADDRESS, 0);

            check(
                api,
                call(P_IIP, fn(api, CREATE_CPU_MEMORY_INFO), ARENA_ALLOCATOR, MEM_TYPE_DEFAULT, out)
            );
            var memoryInfo = out.get(ADDRESS, 0);

            var names = Arena.global();
            var inputNames = names.allocate(ADDRESS, 2);
            inputNames.setAtIndex(ADDRESS, 0, names.allocateFrom("input_ids"));
            inputNames.setAtIndex(ADDRESS, 1, names.allocateFrom("attention_mask"));
            var outputNames = names.allocate(ADDRESS, 1);
            outputNames.setAtIndex(ADDRESS, 0, names.allocateFrom(outputName));
            return new OnnxModel(api, env, session, memoryInfo, inputNames, outputNames, outputs);
        }
    }

    /// Runs `batch` sequences of `seq` tokens each, row-major in `ids` and `mask`, and answers the
    /// raw outputs, `outputs` per sequence, row-major.
    public float[] run(long[] ids, long[] mask, int batch, int seq) {
        if (ids.length != batch * seq || mask.length != batch * seq)
            throw new IllegalArgumentException("expected " + batch + "x" + seq + " inputs");

        try (var arena = Arena.ofConfined()) {
            var shape = arena.allocateFrom(JAVA_LONG, batch, seq);
            var out = arena.allocate(ADDRESS);
            var inputs = arena.allocate(ADDRESS, 2);
            var result = arena.allocate(ADDRESS);
            try {
                inputs.setAtIndex(ADDRESS, 0, tensor(arena, ids, shape, out));
                inputs.setAtIndex(ADDRESS, 1, tensor(arena, mask, shape, out));
                check(
                    api,
                    call(
                        P_PPPPLPLP,
                        fn(api, RUN),
                        session,
                        MemorySegment.NULL,
                        inputNames,
                        inputs,
                        2L,
                        outputNames,
                        1L,
                        result
                    )
                );

                check(
                    api,
                    call(P_PP, fn(api, GET_TENSOR_MUTABLE_DATA), result.get(ADDRESS, 0), out)
                );
                return out.get(ADDRESS, 0)
                    .reinterpret((long) batch * outputs * JAVA_FLOAT.byteSize())
                    .toArray(JAVA_FLOAT);
            } finally {
                release(inputs.getAtIndex(ADDRESS, 0));
                release(inputs.getAtIndex(ADDRESS, 1));
                release(result.get(ADDRESS, 0));
            }
        }
    }

    @Override
    public void close() {
        callVoid(fn(api, RELEASE_MEMORY_INFO), memoryInfo);
        callVoid(fn(api, RELEASE_SESSION), session);
        callVoid(fn(api, RELEASE_ENV), env);
    }

    /// The tensor reads `data` in place, so it must not outlive `arena`.
    private MemorySegment tensor(Arena arena, long[] data, MemorySegment shape, MemorySegment out) {
        var segment = arena.allocateFrom(JAVA_LONG, data);
        check(
            api,
            call(
                P_PPLPLIP,
                fn(api, CREATE_TENSOR_WITH_DATA_AS_ORT_VALUE),
                memoryInfo,
                segment,
                segment.byteSize(),
                shape,
                2L,
                TENSOR_INT64,
                out
            )
        );
        return out.get(ADDRESS, 0);
    }

    private void release(MemorySegment value) {
        if (!value.equals(MemorySegment.NULL)) callVoid(fn(api, RELEASE_VALUE), value);
    }

    private static void check(MemorySegment api, MemorySegment status) {
        if (status.equals(MemorySegment.NULL)) return;
        var message = call(P_P, fn(api, GET_ERROR_MESSAGE), status).reinterpret(Long.MAX_VALUE)
            .getString(0);
        callVoid(fn(api, RELEASE_STATUS), status);
        throw new IllegalStateException("onnxruntime: " + message);
    }

    private static MemorySegment fn(MemorySegment api, int slot) {
        return api.getAtIndex(ADDRESS, slot);
    }

    private static MethodHandle handle(FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(descriptor);
    }

    // One per call shape, since invokeExact needs the static types to match the handle exactly.

    private static MemorySegment call(MethodHandle h, MemorySegment fn) {
        try {
            return (MemorySegment) h.invokeExact(fn);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(MethodHandle h, MemorySegment fn, int a) {
        try {
            return (MemorySegment) h.invokeExact(fn, a);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(MethodHandle h, MemorySegment fn, MemorySegment a) {
        try {
            return (MemorySegment) h.invokeExact(fn, a);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static void callVoid(MemorySegment fn, MemorySegment a) {
        try {
            V_P.invokeExact(fn, a);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(MethodHandle h, MemorySegment fn, MemorySegment a, int b) {
        try {
            return (MemorySegment) h.invokeExact(fn, a, b);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(
        MethodHandle h,
        MemorySegment fn,
        MemorySegment a,
        MemorySegment b
    ) {
        try {
            return (MemorySegment) h.invokeExact(fn, a, b);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(
        MethodHandle h,
        MemorySegment fn,
        int a,
        MemorySegment b,
        MemorySegment c
    ) {
        try {
            return (MemorySegment) h.invokeExact(fn, a, b, c);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(
        MethodHandle h,
        MemorySegment fn,
        int a,
        int b,
        MemorySegment c
    ) {
        try {
            return (MemorySegment) h.invokeExact(fn, a, b, c);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(
        MethodHandle h,
        MemorySegment fn,
        MemorySegment a,
        MemorySegment b,
        MemorySegment c,
        MemorySegment d
    ) {
        try {
            return (MemorySegment) h.invokeExact(fn, a, b, c, d);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static MemorySegment call(
        MethodHandle h,
        MemorySegment fn,
        MemorySegment a,
        MemorySegment b,
        long c,
        MemorySegment d,
        long e,
        int f,
        MemorySegment g
    ) {
        try {
            return (MemorySegment) h.invokeExact(fn, a, b, c, d, e, f, g);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static MemorySegment call(
        MethodHandle h,
        MemorySegment fn,
        MemorySegment a,
        MemorySegment b,
        MemorySegment c,
        MemorySegment d,
        long e,
        MemorySegment f,
        long g,
        MemorySegment i
    ) {
        try {
            return (MemorySegment) h.invokeExact(fn, a, b, c, d, e, f, g, i);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static RuntimeException rethrow(Throwable e) {
        if (e instanceof RuntimeException r) return r;
        if (e instanceof Error err) throw err;
        return new IllegalStateException(e);
    }
}
